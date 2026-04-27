package com.hjo2oa.org.org.structure.domain;

import java.time.Instant;
import java.util.UUID;

public record DepartmentView(
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
}
