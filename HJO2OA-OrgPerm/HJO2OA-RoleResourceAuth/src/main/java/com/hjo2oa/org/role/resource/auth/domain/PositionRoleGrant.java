package com.hjo2oa.org.role.resource.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PositionRoleGrant(
        UUID id,
        UUID positionId,
        UUID roleId,
        UUID tenantId,
        Instant createdAt
) {

    public PositionRoleGrant {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(positionId, "positionId must not be null");
        Objects.requireNonNull(roleId, "roleId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static PositionRoleGrant create(UUID positionId, UUID roleId, UUID tenantId, Instant now) {
        return new PositionRoleGrant(UUID.randomUUID(), positionId, roleId, tenantId, now);
    }

    public PositionRoleGrantView toView() {
        return new PositionRoleGrantView(id, positionId, roleId, tenantId, createdAt);
    }
}
