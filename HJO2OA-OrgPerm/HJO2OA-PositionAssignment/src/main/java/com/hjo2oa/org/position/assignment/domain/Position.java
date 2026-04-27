package com.hjo2oa.org.position.assignment.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Position(
        UUID id,
        String code,
        String name,
        UUID organizationId,
        UUID departmentId,
        PositionCategory category,
        Integer level,
        int sortOrder,
        PositionStatus status,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {

    public Position {
        Objects.requireNonNull(id, "id must not be null");
        code = requireText(code, "code");
        name = requireText(name, "name");
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Position create(
            UUID id,
            String code,
            String name,
            UUID organizationId,
            UUID departmentId,
            PositionCategory category,
            Integer level,
            int sortOrder,
            UUID tenantId,
            Instant now
    ) {
        return new Position(
                id,
                code,
                name,
                organizationId,
                departmentId,
                category,
                level,
                sortOrder,
                PositionStatus.ACTIVE,
                tenantId,
                now,
                now
        );
    }

    public Position update(
            String code,
            String name,
            UUID organizationId,
            UUID departmentId,
            PositionCategory category,
            Integer level,
            int sortOrder,
            Instant now
    ) {
        return new Position(
                id,
                code,
                name,
                organizationId,
                departmentId,
                category,
                level,
                sortOrder,
                status,
                tenantId,
                createdAt,
                now
        );
    }

    public Position disable(Instant now) {
        return new Position(
                id,
                code,
                name,
                organizationId,
                departmentId,
                category,
                level,
                sortOrder,
                PositionStatus.DISABLED,
                tenantId,
                createdAt,
                now
        );
    }

    public Position activate(Instant now) {
        return new Position(
                id,
                code,
                name,
                organizationId,
                departmentId,
                category,
                level,
                sortOrder,
                PositionStatus.ACTIVE,
                tenantId,
                createdAt,
                now
        );
    }

    public PositionView toView() {
        return new PositionView(
                id,
                code,
                name,
                organizationId,
                departmentId,
                category,
                level,
                sortOrder,
                status,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
