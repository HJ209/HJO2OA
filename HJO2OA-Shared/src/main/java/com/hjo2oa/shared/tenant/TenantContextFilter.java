package com.hjo2oa.shared.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMeta;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String SERVER_TIMEZONE = "UTC";

    private final ObjectMapper objectMapper;

    public TenantContextFilter() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    @Autowired
    public TenantContextFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        TenantRequestContext context;
        try {
            context = TenantRequestContext.builder()
                    .tenantId(request.getHeader(SharedTenant.TENANT_ID_HEADER))
                    .requestId(request.getHeader(SharedTenant.REQUEST_ID_HEADER))
                    .idempotencyKey(request.getHeader(SharedTenant.IDEMPOTENCY_KEY_HEADER))
                    .language(request.getHeader(SharedTenant.LANGUAGE_HEADER))
                    .timezone(request.getHeader(SharedTenant.TIMEZONE_HEADER))
                    .identityAssignmentId(request.getHeader(SharedTenant.IDENTITY_ASSIGNMENT_ID_HEADER))
                    .identityPositionId(request.getHeader(SharedTenant.IDENTITY_POSITION_ID_HEADER))
                    .build();
        } catch (IllegalArgumentException ex) {
            writeBadRequest(request, response, ex.getMessage());
            return;
        }
        try (TenantContextHolder.Scope ignored = TenantContextHolder.bind(context)) {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void writeBadRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            String message
    ) throws IOException {
        ResponseMeta meta = new ResponseMeta(
                resolveRequestId(request),
                Instant.now(),
                SERVER_TIMEZONE,
                null,
                null,
                null,
                null
        );
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setHeader(SharedTenant.REQUEST_ID_HEADER, meta.requestId());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.failure(SharedErrorDescriptors.BAD_REQUEST, message, null, meta)
        );
    }

    private static String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(SharedTenant.REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId.trim();
    }
}
