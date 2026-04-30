package com.hjo2oa.data.connector.infrastructure;

import com.hjo2oa.data.connector.domain.ConnectorContext;
import com.hjo2oa.data.connector.domain.ConnectorContextProvider;
import com.hjo2oa.shared.tenant.SharedTenant;
import com.hjo2oa.shared.tenant.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class StaticConnectorContextProvider implements ConnectorContextProvider {

    private static final String DEFAULT_TENANT_ID = "11111111-1111-1111-1111-111111111111";
    private static final String DEFAULT_OPERATOR_ID = "connector-admin";
    private static final String DEFAULT_ENVIRONMENT = "local";
    private static final String OPERATOR_HEADER = "X-Operator-Id";
    private static final String ENVIRONMENT_HEADER = "X-Environment";

    @Override
    public ConnectorContext currentContext() {
        HttpServletRequest request = currentRequest();
        return new ConnectorContext(
                currentTenantId(request),
                currentOperatorId(request),
                headerOrDefault(request, ENVIRONMENT_HEADER, DEFAULT_ENVIRONMENT)
        );
    }

    private String currentTenantId(HttpServletRequest request) {
        return TenantContextHolder.currentTenantId()
                .map(UUID::toString)
                .orElseGet(() -> headerOrDefault(request, SharedTenant.TENANT_ID_HEADER, DEFAULT_TENANT_ID));
    }

    private String currentOperatorId(HttpServletRequest request) {
        String headerOperator = headerOrDefault(request, OPERATOR_HEADER, null);
        if (headerOperator != null) {
            return headerOperator;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            return DEFAULT_OPERATOR_ID;
        }
        return authentication.getName();
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String headerOrDefault(HttpServletRequest request, String headerName, String defaultValue) {
        if (request == null) {
            return defaultValue;
        }
        String headerValue = request.getHeader(headerName);
        if (headerValue == null || headerValue.isBlank()) {
            return defaultValue;
        }
        return headerValue.trim();
    }
}
