package com.hjo2oa.data.service.domain;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record DataServiceOperationContext(
        UUID tenantId,
        String operatorId,
        Set<String> roles,
        Set<String> authorizedAppCodes,
        Set<String> authorizedSubjectIds,
        boolean internalAccess
) {

    public static final String ROLE_PLATFORM_ADMIN = "PLATFORM_ADMIN";
    public static final String ROLE_DATA_SERVICE_MANAGER = "DATA_SERVICE_MANAGER";
    public static final String ROLE_DATA_SERVICE_AUDITOR = "DATA_SERVICE_AUDITOR";

    public DataServiceOperationContext {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        operatorId = requireText(operatorId, "operatorId");
        roles = immutableNormalizedUpperSet(roles);
        authorizedAppCodes = immutableNormalizedSet(authorizedAppCodes);
        authorizedSubjectIds = immutableNormalizedSet(authorizedSubjectIds);
    }

    public boolean canManageDefinitions() {
        return hasRole(ROLE_PLATFORM_ADMIN) || hasRole(ROLE_DATA_SERVICE_MANAGER);
    }

    public boolean canViewDefinitions() {
        return canManageDefinitions() || hasRole(ROLE_DATA_SERVICE_AUDITOR);
    }

    public boolean hasRole(String role) {
        return roles.contains(role == null ? "" : role.trim().toUpperCase(Locale.ROOT));
    }

    private static Set<String> immutableNormalizedUpperSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String candidate = value.trim();
            if (!candidate.isEmpty()) {
                normalized.add(candidate.toUpperCase(Locale.ROOT));
            }
        }
        return Set.copyOf(normalized);
    }

    private static Set<String> immutableNormalizedSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String candidate = value.trim();
            if (!candidate.isEmpty()) {
                normalized.add(candidate);
            }
        }
        return Set.copyOf(normalized);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
