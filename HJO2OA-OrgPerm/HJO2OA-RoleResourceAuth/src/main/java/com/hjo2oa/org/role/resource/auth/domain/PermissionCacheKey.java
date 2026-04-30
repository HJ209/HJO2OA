package com.hjo2oa.org.role.resource.auth.domain;

import java.util.Objects;
import java.util.UUID;

public record PermissionCacheKey(UUID tenantId, UUID personId, UUID positionId) {

    public PermissionCacheKey {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(personId, "personId must not be null");
        Objects.requireNonNull(positionId, "positionId must not be null");
    }
}
