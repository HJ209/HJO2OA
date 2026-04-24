package com.hjo2oa.data.common.infrastructure.audit;

import com.hjo2oa.data.common.application.audit.DataAuditLog;
import com.hjo2oa.data.common.domain.exception.DataServicesErrorCode;
import com.hjo2oa.shared.kernel.BizException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
public class DataAuditLogAspect {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final int MAX_DETAIL_LENGTH = 512;

    private final DataAuditLogWriter dataAuditLogWriter;

    public DataAuditLogAspect(DataAuditLogWriter dataAuditLogWriter) {
        this.dataAuditLogWriter = dataAuditLogWriter;
    }

    @Around("@annotation(dataAuditLog)")
    public Object around(ProceedingJoinPoint joinPoint, DataAuditLog dataAuditLog) throws Throwable {
        long startedAt = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            dataAuditLogWriter.write(buildRecord(
                    joinPoint,
                    dataAuditLog,
                    System.currentTimeMillis() - startedAt,
                    true,
                    null,
                    "操作执行成功",
                    dataAuditLog.captureResult() ? abbreviate(String.valueOf(result)) : summarizeArguments(joinPoint, dataAuditLog)
            ));
            return result;
        } catch (Throwable throwable) {
            dataAuditLogWriter.write(buildRecord(
                    joinPoint,
                    dataAuditLog,
                    System.currentTimeMillis() - startedAt,
                    false,
                    resolveErrorCode(throwable),
                    throwable.getMessage(),
                    summarizeArguments(joinPoint, dataAuditLog)
            ));
            throw throwable;
        }
    }

    private DataAuditRecord buildRecord(
            ProceedingJoinPoint joinPoint,
            DataAuditLog dataAuditLog,
            long durationMs,
            boolean success,
            String errorCode,
            String message,
            String detail
    ) {
        HttpServletRequest request = currentRequest();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String operatorId = authentication == null ? null : authentication.getName();
        String tenantId = request == null ? null : request.getHeader(TENANT_ID_HEADER);
        String requestId = request == null ? null : request.getHeader(REQUEST_ID_HEADER);
        return new DataAuditRecord(
                Instant.now(),
                dataAuditLog.module(),
                dataAuditLog.action(),
                dataAuditLog.targetType(),
                joinPoint.getSignature().toShortString(),
                tenantId,
                operatorId,
                requestId,
                success,
                durationMs,
                errorCode,
                message,
                detail
        );
    }

    private String summarizeArguments(ProceedingJoinPoint joinPoint, DataAuditLog dataAuditLog) {
        if (!dataAuditLog.captureArguments()) {
            return null;
        }
        return abbreviate(Arrays.stream(joinPoint.getArgs())
                .map(this::safeToString)
                .collect(Collectors.joining(", ")));
    }

    private String resolveErrorCode(Throwable throwable) {
        if (throwable instanceof BizException bizException) {
            return bizException.errorCode();
        }
        return DataServicesErrorCode.DATA_COMMON_INTERNAL_ERROR.code();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String safeToString(Object value) {
        try {
            return String.valueOf(value);
        } catch (Exception ex) {
            log.debug("Failed to stringify audit argument", ex);
            return "<unprintable>";
        }
    }

    private String abbreviate(String source) {
        if (source == null || source.length() <= MAX_DETAIL_LENGTH) {
            return source;
        }
        return source.substring(0, MAX_DETAIL_LENGTH) + "...";
    }
}
