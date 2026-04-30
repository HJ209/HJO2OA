package com.hjo2oa.org.role.resource.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ResourceDefinition(
        UUID id,
        ResourceType resourceType,
        String resourceCode,
        String name,
        String parentCode,
        int sortOrder,
        ResourceStatus status,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {

    public ResourceDefinition {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(resourceType, "resourceType must not be null");
        resourceCode = Role.requireText(resourceCode, "resourceCode");
        name = Role.requireText(name, "name");
        parentCode = normalize(parentCode);
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ResourceDefinition create(
            UUID id,
            ResourceType resourceType,
            String resourceCode,
            String name,
            String parentCode,
            int sortOrder,
            UUID tenantId,
            Instant now
    ) {
        return new ResourceDefinition(
                id,
                resourceType,
                resourceCode,
                name,
                parentCode,
                sortOrder,
                ResourceStatus.ACTIVE,
                tenantId,
                now,
                now
        );
    }

    public ResourceDefinition update(
            ResourceType resourceType,
            String resourceCode,
            String name,
            String parentCode,
            int sortOrder,
            ResourceStatus status,
            Instant now
    ) {
        return new ResourceDefinition(
                id,
                resourceType,
                resourceCode,
                name,
                parentCode,
                sortOrder,
                status == null ? this.status : status,
                tenantId,
                createdAt,
                now
        );
    }

    public ResourceDefinitionView toView() {
        return new ResourceDefinitionView(
                id,
                resourceType,
                resourceCode,
                name,
                parentCode,
                sortOrder,
                status,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
