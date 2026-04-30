package com.hjo2oa.org.role.resource.auth.domain;

import java.util.UUID;

public interface PermissionCacheInvalidator {

    void invalidateAll();

    default void invalidateTenant(UUID tenantId) {
        invalidateAll();
    }

    default void invalidateRole(UUID tenantId, UUID roleId) {
        invalidateTenant(tenantId);
    }

    default void invalidatePerson(UUID tenantId, UUID personId) {
        invalidateTenant(tenantId);
    }

    default void invalidatePosition(UUID tenantId, UUID positionId) {
        invalidateTenant(tenantId);
    }

    static PermissionCacheInvalidator noop() {
        return () -> {
        };
    }
}
