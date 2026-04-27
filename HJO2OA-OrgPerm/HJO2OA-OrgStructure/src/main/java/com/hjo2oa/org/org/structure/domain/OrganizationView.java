package com.hjo2oa.org.org.structure.domain;

import java.time.Instant;
import java.util.UUID;

public record OrganizationView(
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
}
