package com.hjo2oa.org.role.resource.auth.interfaces;

import com.hjo2oa.org.role.resource.auth.domain.PermissionDecisionView;
import com.hjo2oa.org.role.resource.auth.domain.PersonRoleView;
import com.hjo2oa.org.role.resource.auth.domain.PositionRoleGrantView;
import com.hjo2oa.org.role.resource.auth.domain.ResourceDefinitionView;
import com.hjo2oa.org.role.resource.auth.domain.ResourcePermissionView;
import com.hjo2oa.org.role.resource.auth.domain.RoleView;
import org.springframework.stereotype.Component;

@Component
public class RoleResourceAuthDtoMapper {

    public RoleResourceAuthDtos.ResourceResponse toResourceResponse(ResourceDefinitionView view) {
        return new RoleResourceAuthDtos.ResourceResponse(
                view.id(),
                view.resourceType(),
                view.resourceCode(),
                view.name(),
                view.parentCode(),
                view.sortOrder(),
                view.status(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public RoleResourceAuthDtos.RoleResponse toRoleResponse(RoleView view) {
        return new RoleResourceAuthDtos.RoleResponse(
                view.id(),
                view.code(),
                view.name(),
                view.category(),
                view.scope(),
                view.description(),
                view.status(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public RoleResourceAuthDtos.ResourcePermissionResponse toResourcePermissionResponse(
            ResourcePermissionView view
    ) {
        return new RoleResourceAuthDtos.ResourcePermissionResponse(
                view.id(),
                view.roleId(),
                view.resourceType(),
                view.resourceCode(),
                view.action(),
                view.effect(),
                view.tenantId()
        );
    }

    public RoleResourceAuthDtos.PersonRoleResponse toPersonRoleResponse(PersonRoleView view) {
        return new RoleResourceAuthDtos.PersonRoleResponse(
                view.id(),
                view.personId(),
                view.roleId(),
                view.reason(),
                view.expiresAt(),
                view.tenantId()
        );
    }

    public RoleResourceAuthDtos.PositionRoleResponse toPositionRoleResponse(PositionRoleGrantView view) {
        return new RoleResourceAuthDtos.PositionRoleResponse(
                view.id(),
                view.positionId(),
                view.roleId(),
                view.tenantId(),
                view.createdAt()
        );
    }

    public RoleResourceAuthDtos.PermissionDecisionResponse toPermissionDecisionResponse(
            PermissionDecisionView view
    ) {
        return new RoleResourceAuthDtos.PermissionDecisionResponse(
                view.resourceType(),
                view.resourceCode(),
                view.action(),
                view.allowed(),
                view.effect(),
                view.matchedPermissions().stream()
                        .map(this::toResourcePermissionResponse)
                        .toList(),
                RoleResourceAuthDtos.PermissionSnapshotResponse.fromSnapshot(view.snapshot())
        );
    }
}
