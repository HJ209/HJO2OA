package com.hjo2oa.org.position.assignment.domain;

import java.time.Instant;
import java.util.UUID;

public record PositionView(
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
}
