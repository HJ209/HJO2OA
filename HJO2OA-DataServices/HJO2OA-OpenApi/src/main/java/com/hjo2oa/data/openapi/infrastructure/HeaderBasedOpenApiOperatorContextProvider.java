package com.hjo2oa.data.openapi.infrastructure;

import com.hjo2oa.data.openapi.domain.OpenApiOperatorContext;
import com.hjo2oa.data.openapi.domain.OpenApiOperatorContextProvider;
import com.hjo2oa.data.openapi.domain.OpenApiOperatorPermission;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class HeaderBasedOpenApiOperatorContextProvider implements OpenApiOperatorContextProvider {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String OPERATOR_HEADER = "X-Operator-Id";
    public static final String PERMISSION_HEADER = "X-Operator-Permissions";

    @Override
    public OpenApiOperatorContext currentContext() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return new OpenApiOperatorContext(
                    "11111111-1111-1111-1111-111111111111",
                    "system",
                    EnumSet.allOf(OpenApiOperatorPermission.class)
            );
        }
        HttpServletRequest request = attributes.getRequest();
        String tenantId = headerOrDefault(request, TENANT_HEADER, "11111111-1111-1111-1111-111111111111");
        String operatorId = headerOrDefault(request, OPERATOR_HEADER, "system");
        String permissionsHeader = request.getHeader(PERMISSION_HEADER);
        Set<OpenApiOperatorPermission> permissions = permissionsHeader == null || permissionsHeader.isBlank()
                ? EnumSet.allOf(OpenApiOperatorPermission.class)
                : Arrays.stream(permissionsHeader.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .map(OpenApiOperatorPermission::valueOf)
                        .collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(OpenApiOperatorPermission.class)));
        return new OpenApiOperatorContext(tenantId, operatorId, permissions);
    }

    private String headerOrDefault(HttpServletRequest request, String headerName, String defaultValue) {
        String headerValue = request.getHeader(headerName);
        if (headerValue == null || headerValue.isBlank()) {
            return defaultValue;
        }
        return headerValue.trim();
    }
}
