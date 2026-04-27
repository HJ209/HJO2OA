package com.hjo2oa.org.role.resource.auth.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record Role(
        UUID id,
        String code,
        String name,
        RoleCategory category,
        RoleScope scope,
        String description,
        RoleStatus status,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {

    public Role {
        Objects.requireNonNull(id, "id must not be null");
        code = requireText(code, "code").toUpperCase(Locale.ROOT);
        name = requireText(name, "name");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        description = normalizeNullable(description);
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Role create(
            UUID id,
            String code,
            String name,
            RoleCategory category,
            RoleScope scope,
            String description,
            UUID tenantId,
            Instant now
    ) {
        return new Role(id, code, name, category, scope, description, RoleStatus.ACTIVE, tenantId, now, now);
    }

    public Role update(
            String code,
            String name,
            RoleCategory category,
            RoleScope scope,
            String description,
            Instant now
    ) {
        return new Role(id, code, name, category, scope, description, status, tenantId, createdAt, now);
    }

    public Role enable(Instant now) {
        return new Role(id, code, name, category, scope, description, RoleStatus.ACTIVE, tenantId, createdAt, now);
    }

    public Role disable(Instant now) {
        return new Role(id, code, name, category, scope, description, RoleStatus.DISABLED, tenantId, createdAt, now);
    }

    public RoleView toView() {
        return new RoleView(id, code, name, category, scope, description, status, tenantId, createdAt, updatedAt);
    }

    public static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    public static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
