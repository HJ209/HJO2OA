package com.hjo2oa.org.org.structure.interfaces;

import com.hjo2oa.org.org.structure.application.OrgStructureCommands;
import com.hjo2oa.org.org.structure.domain.DeptStatus;
import com.hjo2oa.org.org.structure.domain.OrgStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class OrgStructureDtos {

    private OrgStructureDtos() {
    }

    public record CreateOrganizationRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            @Size(max = 64) String shortName,
            @NotBlank @Size(max = 32) String type,
            UUID parentId,
            int sortOrder,
            @NotNull UUID tenantId
    ) {

        public OrgStructureCommands.CreateOrganizationCommand toCommand() {
            return new OrgStructureCommands.CreateOrganizationCommand(
                    code,
                    name,
                    shortName,
                    type,
                    parentId,
                    sortOrder,
                    tenantId
            );
        }
    }

    public record UpdateOrganizationRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            @Size(max = 64) String shortName,
            @NotBlank @Size(max = 32) String type,
            int sortOrder
    ) {

        public OrgStructureCommands.UpdateOrganizationCommand toCommand(UUID organizationId) {
            return new OrgStructureCommands.UpdateOrganizationCommand(
                    organizationId,
                    code,
                    name,
                    shortName,
                    type,
                    sortOrder
            );
        }
    }

    public record MoveNodeRequest(UUID parentId) {

        public OrgStructureCommands.MoveOrganizationCommand toOrganizationCommand(UUID organizationId) {
            return new OrgStructureCommands.MoveOrganizationCommand(organizationId, parentId);
        }

        public OrgStructureCommands.MoveDepartmentCommand toDepartmentCommand(UUID departmentId) {
            return new OrgStructureCommands.MoveDepartmentCommand(departmentId, parentId);
        }
    }

    public record CreateDepartmentRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            @NotNull UUID organizationId,
            UUID parentId,
            UUID managerId,
            int sortOrder,
            @NotNull UUID tenantId
    ) {

        public OrgStructureCommands.CreateDepartmentCommand toCommand() {
            return new OrgStructureCommands.CreateDepartmentCommand(
                    code,
                    name,
                    organizationId,
                    parentId,
                    managerId,
                    sortOrder,
                    tenantId
            );
        }
    }

    public record UpdateDepartmentRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            UUID managerId,
            int sortOrder
    ) {

        public OrgStructureCommands.UpdateDepartmentCommand toCommand(UUID departmentId) {
            return new OrgStructureCommands.UpdateDepartmentCommand(
                    departmentId,
                    code,
                    name,
                    managerId,
                    sortOrder
            );
        }
    }

    public record OrganizationResponse(
            UUID id,
            String code,
            String name,
            String shortName,
            String type,
            UUID parentId,
            int level,
            String path,
            int sortOrder,
            OrgStatus status,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record DepartmentResponse(
            UUID id,
            String code,
            String name,
            UUID organizationId,
            UUID parentId,
            int level,
            String path,
            UUID managerId,
            int sortOrder,
            DeptStatus status,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
