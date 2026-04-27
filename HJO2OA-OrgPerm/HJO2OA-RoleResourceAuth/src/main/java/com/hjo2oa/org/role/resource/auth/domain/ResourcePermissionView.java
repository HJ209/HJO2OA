package com.hjo2oa.org.role.resource.auth.domain;

import java.util.UUID;

public record ResourcePermissionView(
        UUID id,
        UUID roleId,
        ResourceType resourceType,
        String resourceCode,
        ResourceAction action,
        PermissionEffect effect,
        UUID tenantId
) {
}
