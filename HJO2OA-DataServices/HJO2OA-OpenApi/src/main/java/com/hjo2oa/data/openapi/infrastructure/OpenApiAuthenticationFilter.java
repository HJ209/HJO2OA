package com.hjo2oa.data.openapi.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.data.openapi.application.OpenApiAuthenticationApplicationService;
import com.hjo2oa.data.openapi.domain.ApiInvocationAuditLog;
import com.hjo2oa.data.openapi.domain.ApiInvocationAuditLogRepository;
import com.hjo2oa.data.openapi.domain.ApiInvocationOutcome;
import com.hjo2oa.data.openapi.domain.AuthenticatedOpenApiInvocationContext;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class OpenApiAuthenticationFilter extends OncePerRequestFilter {

    private final OpenApiAuthenticationApplicationService authenticationApplicationService;
    private final OpenApiInvocationContextHolder contextHolder;
    private final ApiInvocationAuditLogRepository auditLogRepository;
    private final ResponseMetaFactory responseMetaFactory;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OpenApiAuthenticationFilter(
            OpenApiAuthenticationApplicationService authenticationApplicationService,
            OpenApiInvocationContextHolder contextHolder,
            ApiInvocationAuditLogRepository auditLogRepository,
            ResponseMetaFactory responseMetaFactory,
            ObjectMapper objectMapper
    ) {
        this(
                authenticationApplicationService,
                contextHolder,
                auditLogRepository,
                responseMetaFactory,
                objectMapper,
                Clock.systemUTC()
        );
    }

    @Autowired
    public OpenApiAuthenticationFilter(
            OpenApiAuthenticationApplicationService authenticationApplicationService,
            OpenApiInvocationContextHolder contextHolder,
            ApiInvocationAuditLogRepository auditLogRepository,
            ResponseMetaFactory responseMetaFactory,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.authenticationApplicationService = Objects.requireNonNull(
                authenticationApplicationService,
                "authenticationApplicationService must not be null"
        );
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder must not be null");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository must not be null");
        this.responseMetaFactory = Objects.requireNonNull(responseMetaFactory, "responseMetaFactory must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null")
                .copy()
                .findAndRegisterModules();
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/open/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        Instant startedAt = clock.instant();
        AuthenticatedOpenApiInvocationContext authenticatedContext = null;
        BizException authFailure = null;
        try {
            authenticatedContext = authenticationApplicationService.authenticate(wrappedRequest);
            contextHolder.set(authenticatedContext);
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } catch (BizException ex) {
            authFailure = ex;
            OpenApiInvocationRequestAttributes.mark(ApiInvocationOutcome.AUTH_FAILED, ex.errorCode());
            writeErrorResponse(wrappedRequest, wrappedResponse, ex);
        } finally {
            persistAuditLog(wrappedRequest, wrappedResponse, startedAt, authenticatedContext, authFailure);
            contextHolder.clear();
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void writeErrorResponse(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            BizException ex
    ) throws IOException {
        response.setStatus(ex.httpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        ApiResponse<Void> body = ApiResponse.failure(
                ex.errorDescriptor(),
                ex.userMessage(),
                null,
                responseMetaFactory.create(request)
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.flushBuffer();
    }

    private void persistAuditLog(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            Instant startedAt,
            AuthenticatedOpenApiInvocationContext authenticatedContext,
            BizException authFailure
    ) {
        Instant finishedAt = clock.instant();
        ApiInvocationOutcome outcome = OpenApiInvocationRequestAttributes.resolveOutcome(request)
                .orElseGet(() -> resolveOutcome(response.getStatus(), authFailure != null));
        String errorCode = authFailure == null
                ? OpenApiInvocationRequestAttributes.resolveErrorCode(request)
                : authFailure.errorCode();
        String tenantId = authenticatedContext == null
                ? headerOrDefault(request, OpenApiAuthenticationApplicationService.TENANT_HEADER, "unknown-tenant")
                : authenticatedContext.tenantId();
        String requestId = authenticatedContext == null
                ? authenticationApplicationService.resolveRequestId(request)
                : authenticatedContext.requestId();
        ApiInvocationAuditLog auditLog = ApiInvocationAuditLog.create(
                requestId,
                tenantId,
                authenticatedContext == null ? null : authenticatedContext.endpoint().apiId(),
                authenticatedContext == null ? null : authenticatedContext.endpoint().code(),
                authenticatedContext == null ? authenticationApplicationService.resolveVersion(request)
                        : authenticatedContext.endpoint().version(),
                request.getRequestURI(),
                com.hjo2oa.data.openapi.domain.OpenApiHttpMethod.valueOf(request.getMethod().toUpperCase()),
                headerOrDefault(request, OpenApiAuthenticationApplicationService.CLIENT_CODE_HEADER, null),
                authenticatedContext == null ? null : authenticatedContext.endpoint().authType(),
                outcome,
                response.getStatus(),
                errorCode,
                Duration.between(startedAt, finishedAt).toMillis(),
                digestRequest(request),
                request.getRemoteAddr(),
                startedAt
        );
        auditLogRepository.save(auditLog);
    }

    private ApiInvocationOutcome resolveOutcome(int responseStatus, boolean authFailed) {
        if (authFailed) {
            return ApiInvocationOutcome.AUTH_FAILED;
        }
        if (responseStatus >= 200 && responseStatus < 300) {
            return ApiInvocationOutcome.SUCCESS;
        }
        return ApiInvocationOutcome.ERROR;
    }

    private String digestRequest(ContentCachingRequestWrapper request) {
        byte[] requestBody = request.getContentAsByteArray();
        String source = request.getRequestURI()
                + "?"
                + (request.getQueryString() == null ? "" : request.getQueryString())
                + "|"
                + new String(requestBody, StandardCharsets.UTF_8);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is required", ex);
        }
    }

    private String headerOrDefault(HttpServletRequest request, String headerName, String defaultValue) {
        String value = request.getHeader(headerName);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
