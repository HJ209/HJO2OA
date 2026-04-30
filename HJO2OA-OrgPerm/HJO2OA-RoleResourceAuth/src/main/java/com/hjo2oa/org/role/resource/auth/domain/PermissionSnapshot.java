package com.hjo2oa.org.role.resource.auth.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PermissionSnapshot(
        UUID tenantId,
        UUID personId,
        UUID positionId,
        List<UUID> roleIds,
        List<ResourcePermissionView> resourcePermissions,
        long version
) {

    public PermissionSnapshot {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(personId, "personId must not be null");
        Objects.requireNonNull(positionId, "positionId must not be null");
        roleIds = List.copyOf(new LinkedHashSet<>(Objects.requireNonNullElse(roleIds, List.of())));
        resourcePermissions = List.copyOf(Objects.requireNonNullElse(resourcePermissions, List.of()));
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
