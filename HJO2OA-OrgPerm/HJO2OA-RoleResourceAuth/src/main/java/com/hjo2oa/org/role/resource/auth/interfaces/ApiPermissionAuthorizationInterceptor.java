package com.hjo2oa.org.role.resource.auth.interfaces;

import com.hjo2oa.infra.security.infrastructure.jwt.JwtAuthenticationToken;
import com.hjo2oa.infra.security.infrastructure.jwt.JwtClaims;
import com.hjo2oa.org.role.resource.auth.application.PermissionCalculator;
import com.hjo2oa.org.role.resource.auth.domain.ResourceAction;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiPermissionAuthorizationInterceptor implements HandlerInterceptor {

    private static final String POSITION_HEADER = "X-Identity-Position-Id";

    private final PermissionCalculator permissionCalculator;
    private final boolean denyUnregisteredApi;

    public ApiPermissionAuthorizationInterceptor(
            PermissionCalculator permissionCalculator,
            @Value("${hjo2oa.security.api-permission.deny-unregistered:true}") boolean denyUnregisteredApi
    ) {
        this.permissionCalculator = Objects.requireNonNull(permissionCalculator, "permissionCalculator must not be null");
        this.denyUnregisteredApi = denyUnregisteredApi;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || path.startsWith("/api/auth/") || path.startsWith("/api/v1/auth/")) {
            return true;
        }
        JwtClaims claims = currentClaims();
        if (claims == null) {
            return true;
        }
        ResourceAction action = toAction(request.getMethod());
        UUID tenantId = UUID.fromString(claims.tenantId());
        if (!permissionCalculator.isApiResourceConfigured(tenantId, path, action)) {
            if (denyUnregisteredApi) {
                throw new BizException(SharedErrorDescriptors.FORBIDDEN, "API resource is not registered");
            }
            return true;
        }
        UUID personId = UUID.fromString(claims.personId());
        UUID positionId = resolvePositionId(request, claims);
        if (positionId == null || !permissionCalculator.hasApiPermission(tenantId, personId, positionId, path, action)) {
            throw new BizException(SharedErrorDescriptors.FORBIDDEN, "API permission denied");
        }
        return true;
    }

    private JwtClaims currentClaims() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.claims();
        }
        return null;
    }

    private UUID resolvePositionId(HttpServletRequest request, JwtClaims claims) {
        String headerPositionId = request.getHeader(POSITION_HEADER);
        String positionId = headerPositionId == null || headerPositionId.isBlank()
                ? claims.currentPositionId()
                : headerPositionId;
        if (positionId == null || positionId.isBlank()) {
            return null;
        }
        return UUID.fromString(positionId);
    }

    private ResourceAction toAction(String method) {
        return switch (method.toUpperCase(Locale.ROOT)) {
            case "POST" -> ResourceAction.CREATE;
            case "PUT", "PATCH" -> ResourceAction.UPDATE;
            case "DELETE" -> ResourceAction.DELETE;
            default -> ResourceAction.READ;
        };
    }
}
