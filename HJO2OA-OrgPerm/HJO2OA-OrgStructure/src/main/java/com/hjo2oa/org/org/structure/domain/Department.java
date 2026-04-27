package com.hjo2oa.org.org.structure.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Department(
        UUID id,
        String code,
        String name,
        UUID organizationId,
        UUID parentId,
        int level,
        String path,
        UUID managerId,
        int sortOrder,
        DeptStatus status,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {

    public Department {
        Objects.requireNonNull(id, "id must not be null");
        code = Organization.requireText(code, "code");
        name = Organization.requireText(name, "name");
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        if (level < 0) {
            throw new IllegalArgumentException("level must not be negative");
        }
        path = Organization.requireText(path, "path");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Department create(
            UUID id,
            String code,
            String name,
            UUID organizationId,
            UUID parentId,
            int level,
            String path,
            UUID managerId,
            int sortOrder,
            UUID tenantId,
            Instant now
    ) {
        return new Department(
                id,
                code,
                name,
                organizationId,
                parentId,
                level,
                path,
                managerId,
                sortOrder,
                DeptStatus.ACTIVE,
                tenantId,
                now,
                now
        );
    }

    public Department update(String code, String name, UUID managerId, int sortOrder, Instant now) {
        return new Department(
                id,
                code,
                name,
                organizationId,
                parentId,
                level,
                path,
                managerId,
                sortOrder,
                status,
                tenantId,
                createdAt,
                now
        );
    }

    public Department move(UUID parentId, int level, String path, Instant now) {
        return new Department(
                id,
                code,
                name,
                organizationId,
                parentId,
                level,
                path,
                managerId,
                sortOrder,
                status,
                tenantId,
                createdAt,
                now
        );
    }

    public Department disable(Instant now) {
        if (status == DeptStatus.DISABLED) {
            return this;
        }
        return withStatus(DeptStatus.DISABLED, now);
    }

    public Department activate(Instant now) {
        if (status == DeptStatus.ACTIVE) {
            return this;
        }
        return withStatus(DeptStatus.ACTIVE, now);
    }

    public DepartmentView toView() {
        return new DepartmentView(
                id,
                code,
                name,
                organizationId,
                parentId,
                level,
                path,
                managerId,
                sortOrder,
                status,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    private Department withStatus(DeptStatus status, Instant now) {
        return new Department(
                id,
                code,
                name,
                organizationId,
                parentId,
                level,
                path,
                managerId,
                sortOrder,
                status,
                tenantId,
                createdAt,
                now
        );
    }
}
