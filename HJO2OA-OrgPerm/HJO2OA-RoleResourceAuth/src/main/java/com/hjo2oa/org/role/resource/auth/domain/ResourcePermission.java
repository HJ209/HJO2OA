package com.hjo2oa.org.role.resource.auth.domain;

import java.util.Objects;
import java.util.UUID;

public record ResourcePermission(
        UUID id,
        UUID roleId,
        ResourceType resourceType,
        String resourceCode,
        ResourceAction action,
        PermissionEffect effect,
        UUID tenantId
) {

    public ResourcePermission {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(roleId, "roleId must not be null");
        Objects.requireNonNull(resourceType, "resourceType must not be null");
        resourceCode = Role.requireText(resourceCode, "resourceCode");
        Objects.requireNonNull(action, "action must not be null");
        effect = effect == null ? PermissionEffect.ALLOW : effect;
        Objects.requireNonNull(tenantId, "tenantId must not be null");
    }

    public static ResourcePermission create(
            UUID id,
            UUID roleId,
            ResourceType resourceType,
            String resourceCode,
            ResourceAction action,
            PermissionEffect effect,
            UUID tenantId
    ) {
        return new ResourcePermission(id, roleId, resourceType, resourceCode, action, effect, tenantId);
    }

    public ResourcePermissionView toView() {
        return new ResourcePermissionView(id, roleId, resourceType, resourceCode, action, effect, tenantId);
    }
}
