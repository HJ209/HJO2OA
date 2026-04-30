package com.hjo2oa.org.role.resource.auth.domain;

import java.time.Instant;
import java.util.UUID;

public record ResourceDefinitionView(
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
}
