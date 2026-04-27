package com.hjo2oa.org.position.assignment.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PositionRole(
        UUID id,
        UUID positionId,
        UUID roleId,
        UUID tenantId,
        Instant createdAt
) {

    public PositionRole {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(positionId, "positionId must not be null");
        Objects.requireNonNull(roleId, "roleId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static PositionRole create(UUID id, UUID positionId, UUID roleId, UUID tenantId, Instant now) {
        return new PositionRole(id, positionId, roleId, tenantId, now);
    }

    public PositionRoleView toView() {
        return new PositionRoleView(id, positionId, roleId, tenantId, createdAt);
    }
}
