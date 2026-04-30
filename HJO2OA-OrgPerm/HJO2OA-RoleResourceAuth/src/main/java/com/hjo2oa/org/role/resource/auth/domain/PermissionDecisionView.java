package com.hjo2oa.org.role.resource.auth.domain;

import java.util.List;

public record PermissionDecisionView(
        ResourceType resourceType,
        String resourceCode,
        ResourceAction action,
        boolean allowed,
        PermissionEffect effect,
        List<ResourcePermissionView> matchedPermissions,
        PermissionSnapshot snapshot
) {
}
