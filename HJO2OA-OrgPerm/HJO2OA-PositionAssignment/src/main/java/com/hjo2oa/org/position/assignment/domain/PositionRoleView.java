package com.hjo2oa.org.position.assignment.domain;

import java.time.Instant;
import java.util.UUID;

public record PositionRoleView(
        UUID id,
        UUID positionId,
        UUID roleId,
        UUID tenantId,
        Instant createdAt
) {
}
