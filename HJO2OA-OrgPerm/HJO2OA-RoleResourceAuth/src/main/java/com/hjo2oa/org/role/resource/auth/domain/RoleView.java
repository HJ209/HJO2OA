package com.hjo2oa.org.role.resource.auth.domain;

import java.time.Instant;
import java.util.UUID;

public record RoleView(
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
}
