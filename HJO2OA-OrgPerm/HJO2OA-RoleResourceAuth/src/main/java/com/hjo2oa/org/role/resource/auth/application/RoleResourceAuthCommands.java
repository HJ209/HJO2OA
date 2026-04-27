package com.hjo2oa.org.role.resource.auth.application;

import com.hjo2oa.org.role.resource.auth.domain.PermissionEffect;
import com.hjo2oa.org.role.resource.auth.domain.ResourceAction;
import com.hjo2oa.org.role.resource.auth.domain.ResourceType;
import com.hjo2oa.org.role.resource.auth.domain.RoleCategory;
import com.hjo2oa.org.role.resource.auth.domain.RoleScope;
import com.hjo2oa.org.role.resource.auth.domain.RoleStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RoleResourceAuthCommands {

    private RoleResourceAuthCommands() {
    }

    public record CreateRoleCommand(
            String code,
            String name,
            RoleCategory category,
            RoleScope scope,
            String description,
            UUID tenantId
    ) {
    }

    public record UpdateRoleCommand(
            UUID roleId,
            String code,
            String name,
            RoleCategory category,
            RoleScope scope,
            String description
    ) {
    }

    public record ChangeRoleStatusCommand(UUID roleId, RoleStatus status) {
    }

    public record ReplaceResourcePermissionsCommand(
            UUID roleId,
            List<ResourcePermissionItem> permissions
    ) {
    }

    public record ResourcePermissionItem(
            ResourceType resourceType,
            String resourceCode,
            ResourceAction action,
            PermissionEffect effect
    ) {
    }

    public record GrantPersonRoleCommand(
            UUID personId,
            UUID roleId,
            String reason,
            Instant expiresAt
    ) {
    }
}
