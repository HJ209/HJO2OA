package com.hjo2oa.org.org.structure.application;

import java.util.UUID;

public final class OrgStructureCommands {

    private OrgStructureCommands() {
    }

    public record CreateOrganizationCommand(
            String code,
            String name,
            String shortName,
            String type,
            UUID parentId,
            int sortOrder,
            UUID tenantId
    ) {
    }

    public record UpdateOrganizationCommand(
            UUID organizationId,
            String code,
            String name,
            String shortName,
            String type,
            int sortOrder,
            UUID tenantId
    ) {
    }

    public record MoveOrganizationCommand(
            UUID organizationId,
            UUID parentId,
            Integer sortOrder,
            UUID tenantId
    ) {
    }

    public record CreateDepartmentCommand(
            String code,
            String name,
            UUID organizationId,
            UUID parentId,
            UUID managerId,
            int sortOrder,
            UUID tenantId
    ) {
    }

    public record UpdateDepartmentCommand(
            UUID departmentId,
            String code,
            String name,
            UUID managerId,
            int sortOrder,
            UUID tenantId
    ) {
    }

    public record MoveDepartmentCommand(
            UUID departmentId,
            UUID parentId,
            Integer sortOrder,
            UUID tenantId
    ) {
    }
}
