package com.hjo2oa.data.openapi.domain;

import java.util.Set;

public record OpenApiOperatorContext(
        String tenantId,
        String operatorId,
        Set<OpenApiOperatorPermission> permissions
) {

    public OpenApiOperatorContext {
        tenantId = requireText(tenantId, "tenantId");
        operatorId = requireText(operatorId, "operatorId");
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public boolean has(OpenApiOperatorPermission permission) {
        return permissions.contains(permission);
    }

    public boolean hasAny(OpenApiOperatorPermission... requiredPermissions) {
        for (OpenApiOperatorPermission permission : requiredPermissions) {
            if (has(permission)) {
                return true;
            }
        }
        return false;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
