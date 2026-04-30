package com.hjo2oa.org.org.structure.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Organization(
        UUID id,
        String code,
        String name,
        String shortName,
        String type,
        UUID parentId,
        int level,
        String path,
        int sortOrder,
        OrgStatus status,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {

    public Organization {
        Objects.requireNonNull(id, "id must not be null");
        code = requireText(code, "code");
        name = requireText(name, "name");
        shortName = normalizeNullable(shortName);
        type = requireText(type, "type");
        if (level < 0) {
            throw new IllegalArgumentException("level must not be negative");
        }
        path = requireText(path, "path");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Organization create(
            UUID id,
            String code,
            String name,
            String shortName,
            String type,
            UUID parentId,
            int level,
            String path,
            int sortOrder,
            UUID tenantId,
            Instant now
    ) {
        return new Organization(
                id,
                code,
                name,
                shortName,
                type,
                parentId,
                level,
                path,
                sortOrder,
                OrgStatus.ACTIVE,
                tenantId,
                now,
                now
        );
    }

    public Organization update(
            String code,
            String name,
            String shortName,
            String type,
            int sortOrder,
            Instant now
    ) {
        return new Organization(
                id,
                code,
                name,
                shortName,
                type,
                parentId,
                level,
                path,
                sortOrder,
                status,
                tenantId,
                createdAt,
                now
        );
    }

    public Organization move(UUID parentId, int level, String path, int sortOrder, Instant now) {
        return new Organization(
                id,
                code,
                name,
                shortName,
                type,
                parentId,
                level,
                path,
                sortOrder,
                status,
                tenantId,
                createdAt,
                now
        );
    }

    public Organization disable(Instant now) {
        if (status == OrgStatus.DISABLED) {
            return this;
        }
        return withStatus(OrgStatus.DISABLED, now);
    }

    public Organization activate(Instant now) {
        if (status == OrgStatus.ACTIVE) {
            return this;
        }
        return withStatus(OrgStatus.ACTIVE, now);
    }

    public OrganizationView toView() {
        return new OrganizationView(
                id,
                code,
                name,
                shortName,
                type,
                parentId,
                level,
                path,
                sortOrder,
                status,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    private Organization withStatus(OrgStatus status, Instant now) {
        return new Organization(
                id,
                code,
                name,
                shortName,
                type,
                parentId,
                level,
                path,
                sortOrder,
                status,
                tenantId,
                createdAt,
                now
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

    static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
