package com.hjo2oa.org.role.resource.auth.domain;

import java.time.Instant;
import java.util.UUID;

public record PositionRoleGrantView(
        UUID id,
        UUID positionId,
        UUID roleId,
        UUID tenantId,
        Instant createdAt
) {
}
