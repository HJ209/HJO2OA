package com.hjo2oa.org.role.resource.auth.interfaces;

import com.hjo2oa.org.role.resource.auth.application.RoleResourceAuthCommands;
import com.hjo2oa.org.role.resource.auth.domain.PermissionEffect;
import com.hjo2oa.org.role.resource.auth.domain.ResourceAction;
import com.hjo2oa.org.role.resource.auth.domain.ResourceType;
import com.hjo2oa.org.role.resource.auth.domain.RoleCategory;
import com.hjo2oa.org.role.resource.auth.domain.RoleScope;
import com.hjo2oa.org.role.resource.auth.domain.RoleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RoleResourceAuthDtos {

    private RoleResourceAuthDtos() {
    }

    public record CreateRoleRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            @NotNull RoleCategory category,
            @NotNull RoleScope scope,
            @Size(max = 512) String description,
            @NotNull UUID tenantId
    ) {

        public RoleResourceAuthCommands.CreateRoleCommand toCommand() {
            return new RoleResourceAuthCommands.CreateRoleCommand(
                    code,
                    name,
                    category,
                    scope,
                    description,
                    tenantId
            );
        }
    }

    public record UpdateRoleRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            @NotNull RoleCategory category,
            @NotNull RoleScope scope,
            @Size(max = 512) String description
    ) {

        public RoleResourceAuthCommands.UpdateRoleCommand toCommand(UUID roleId) {
            return new RoleResourceAuthCommands.UpdateRoleCommand(roleId, code, name, category, scope, description);
        }
    }

    public record ChangeRoleStatusRequest(@NotNull RoleStatus status) {

        public RoleResourceAuthCommands.ChangeRoleStatusCommand toCommand(UUID roleId) {
            return new RoleResourceAuthCommands.ChangeRoleStatusCommand(roleId, status);
        }
    }

    public record ReplaceResourcePermissionsRequest(
            @NotNull List<@Valid ResourcePermissionItemRequest> permissions
    ) {

        public RoleResourceAuthCommands.ReplaceResourcePermissionsCommand toCommand(UUID roleId) {
            return new RoleResourceAuthCommands.ReplaceResourcePermissionsCommand(
                    roleId,
                    permissions.stream().map(ResourcePermissionItemRequest::toCommandItem).toList()
            );
        }
    }

    public record ResourcePermissionItemRequest(
            @NotNull ResourceType resourceType,
            @NotBlank @Size(max = 128) String resourceCode,
            @NotNull ResourceAction action,
            PermissionEffect effect
    ) {

        public RoleResourceAuthCommands.ResourcePermissionItem toCommandItem() {
            return new RoleResourceAuthCommands.ResourcePermissionItem(resourceType, resourceCode, action, effect);
        }
    }

    public record GrantPersonRoleRequest(
            @NotNull UUID roleId,
            @NotBlank @Size(max = 256) String reason,
            Instant expiresAt
    ) {

        public RoleResourceAuthCommands.GrantPersonRoleCommand toCommand(UUID personId) {
            return new RoleResourceAuthCommands.GrantPersonRoleCommand(personId, roleId, reason, expiresAt);
        }
    }

    public record RoleResponse(
            UUID id,
            String code,
            String name,
            RoleCategory category,
            RoleScope scope,
            String description,
            RoleStatus status,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ResourcePermissionResponse(
            UUID id,
            UUID roleId,
            ResourceType resourceType,
            String resourceCode,
            ResourceAction action,
            PermissionEffect effect,
            UUID tenantId
    ) {
    }

    public record PersonRoleResponse(
            UUID id,
            UUID personId,
            UUID roleId,
            String reason,
            Instant expiresAt,
            UUID tenantId
    ) {
    }
}
