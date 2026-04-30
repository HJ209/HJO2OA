package com.hjo2oa.infra.audit.application;

import com.hjo2oa.infra.audit.domain.SensitivityLevel;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditAspect {

    private static final int SUMMARY_MAX_LENGTH = 512;
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final AuditRecordApplicationService auditRecordApplicationService;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public AuditAspect(AuditRecordApplicationService auditRecordApplicationService) {
        this.auditRecordApplicationService = auditRecordApplicationService;
    }

    @Around("@annotation(audited)")
    public Object record(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object[] args = joinPoint.getArgs();
        String beforeSummary = audited.captureArguments() ? summarize(args) : null;
        try {
            Object result = joinPoint.proceed();
            writeAudit(audited, method, args, result, null, beforeSummary);
            return result;
        } catch (Throwable throwable) {
            writeAudit(audited, method, args, null, throwable, beforeSummary);
            throw throwable;
        }
    }

    private void writeAudit(
            Audited audited,
            Method method,
            Object[] args,
            Object result,
            Throwable throwable,
            String beforeSummary
    ) {
        HttpServletRequest request = currentRequest();
        String traceId = header(request, REQUEST_ID_HEADER);
        UUID tenantId = firstNonNull(
                uuidHeader(request, "X-Tenant-Id"),
                findUuidValue(args, "tenantId"),
                findUuidValue(new Object[] {result}, "tenantId")
        );
        List<AuditRecordCommands.FieldChangeCommand> changes = new ArrayList<>();
        if (beforeSummary != null) {
            changes.add(change("before", null, beforeSummary));
        }
        if (throwable == null && audited.captureResult()) {
            changes.add(change("after", null, summarize(result)));
        }
        if (throwable != null) {
            changes.add(change("error", null, throwable.getClass().getName() + ": " + throwable.getMessage()));
        }
        changes.add(change("requestId", null, traceId));
        changes.add(change("language", null, header(request, "Accept-Language")));
        changes.add(change("timezone", null, header(request, "X-Timezone")));

        auditRecordApplicationService.recordAudit(new AuditRecordCommands.RecordAuditCommand(
                audited.module(),
                audited.targetType(),
                resolveTargetId(audited, method, args, result, throwable),
                audited.action(),
                uuidHeader(request, "X-Operator-Account-Id"),
                uuidHeader(request, "X-Operator-Person-Id"),
                tenantId,
                traceId,
                truncate(summary(audited, method, throwable)),
                changes
        ));
    }

    private String resolveTargetId(Audited audited, Method method, Object[] args, Object result, Throwable throwable) {
        String expression = audited.targetId();
        if (expression != null && !expression.isBlank()) {
            Object value = evaluateExpression(expression, method, args, result, throwable);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        UUID id = firstNonNull(findUuidValue(new Object[] {result}, "id"), firstUuidArg(args));
        return id == null ? method.getDeclaringClass().getSimpleName() + "." + method.getName() : id.toString();
    }

    private Object evaluateExpression(String expression, Method method, Object[] args, Object result, Throwable throwable) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        for (int i = 0; i < args.length; i++) {
            context.setVariable("p" + i, args[i]);
            context.setVariable("a" + i, args[i]);
            if (parameterNames != null && i < parameterNames.length) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        context.setVariable("result", result);
        context.setVariable("exception", throwable);
        try {
            return expressionParser.parseExpression(expression).getValue(context);
        } catch (EvaluationException ex) {
            return null;
        }
    }

    private AuditRecordCommands.FieldChangeCommand change(String fieldName, String oldValue, String newValue) {
        return new AuditRecordCommands.FieldChangeCommand(fieldName, oldValue, newValue, SensitivityLevel.LOW);
    }

    private String summary(Audited audited, Method method, Throwable throwable) {
        String status = throwable == null ? "SUCCEEDED" : "FAILED";
        return audited.module() + "." + audited.action() + " "
                + method.getDeclaringClass().getSimpleName() + "." + method.getName() + " " + status;
    }

    private String summarize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Object[] values) {
            List<String> parts = new ArrayList<>();
            for (Object item : values) {
                parts.add(summarize(item));
            }
            return truncate(parts.toString());
        }
        return truncate(String.valueOf(value));
    }

    private UUID firstUuidArg(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof UUID uuid) {
                return uuid;
            }
        }
        return null;
    }

    private UUID findUuidValue(Object[] sources, String name) {
        for (Object source : sources) {
            UUID value = readUuidValue(source, name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private UUID readUuidValue(Object source, String name) {
        if (source == null) {
            return null;
        }
        try {
            Method method = source.getClass().getMethod(name);
            Object value = method.invoke(source);
            return value instanceof UUID uuid ? uuid : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private UUID uuidHeader(HttpServletRequest request, String headerName) {
        String value = header(request, headerName);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String header(HttpServletRequest request, String headerName) {
        if (request == null) {
            return null;
        }
        String value = request.getHeader(headerName);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= SUMMARY_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, SUMMARY_MAX_LENGTH);
    }
}
