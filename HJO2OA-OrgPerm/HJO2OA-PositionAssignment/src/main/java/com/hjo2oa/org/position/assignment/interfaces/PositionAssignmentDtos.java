package com.hjo2oa.org.position.assignment.interfaces;

import com.hjo2oa.org.position.assignment.application.PositionAssignmentCommands;
import com.hjo2oa.org.position.assignment.domain.AssignmentStatus;
import com.hjo2oa.org.position.assignment.domain.AssignmentType;
import com.hjo2oa.org.position.assignment.domain.PositionCategory;
import com.hjo2oa.org.position.assignment.domain.PositionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class PositionAssignmentDtos {

    private PositionAssignmentDtos() {
    }

    public record CreatePositionRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            @NotNull UUID organizationId,
            UUID departmentId,
            @NotNull PositionCategory category,
            Integer level,
            int sortOrder,
            UUID tenantId
    ) {

        public PositionAssignmentCommands.CreatePositionCommand toCommand(UUID requestTenantId) {
            return new PositionAssignmentCommands.CreatePositionCommand(
                    code,
                    name,
                    organizationId,
                    departmentId,
                    category,
                    level,
                    sortOrder,
                    requestTenantId
            );
        }
    }

    public record UpdatePositionRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            @NotNull UUID organizationId,
            UUID departmentId,
            @NotNull PositionCategory category,
            Integer level,
            int sortOrder
    ) {

        public PositionAssignmentCommands.UpdatePositionCommand toCommand(UUID positionId, UUID requestTenantId) {
            return new PositionAssignmentCommands.UpdatePositionCommand(
                    positionId,
                    code,
                    name,
                    organizationId,
                    departmentId,
                    category,
                    level,
                    sortOrder,
                    requestTenantId
            );
        }
    }

    public record CreateAssignmentRequest(
            @NotNull UUID personId,
            @NotNull UUID positionId,
            @NotNull AssignmentType type,
            LocalDate startDate,
            LocalDate endDate,
            UUID tenantId
    ) {

        public PositionAssignmentCommands.CreateAssignmentCommand toCommand(UUID requestTenantId) {
            return new PositionAssignmentCommands.CreateAssignmentCommand(
                    personId,
                    positionId,
                    type,
                    startDate,
                    endDate,
                    requestTenantId
            );
        }
    }

    public record ChangeAssignmentTypeRequest(
            @NotNull AssignmentType type
    ) {
    }

    public record DeactivateAssignmentRequest(
            LocalDate endDate
    ) {
    }

    public record AddPositionRoleRequest(
            @NotNull UUID roleId,
            UUID tenantId
    ) {

        public PositionAssignmentCommands.AddPositionRoleCommand toCommand(UUID positionId, UUID requestTenantId) {
            return new PositionAssignmentCommands.AddPositionRoleCommand(positionId, roleId, requestTenantId);
        }
    }

    public record PositionAssignmentExportResponse(
            java.util.List<PositionResponse> positions,
            java.util.List<AssignmentResponse> assignments,
            java.util.List<PositionRoleResponse> positionRoles
    ) {
    }

    public record PositionAssignmentImportRequest(
            java.util.List<CreatePositionRequest> positions,
            java.util.List<CreateAssignmentRequest> assignments
    ) {
    }

    public record PositionAssignmentImportResponse(
            int positionsCreated,
            int assignmentsCreated,
            java.util.List<UUID> positionIds,
            java.util.List<UUID> assignmentIds
    ) {
    }

    public record PositionResponse(
            UUID id,
            String code,
            String name,
            UUID organizationId,
            UUID departmentId,
            PositionCategory category,
            Integer level,
            int sortOrder,
            PositionStatus status,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record AssignmentResponse(
            UUID id,
            UUID personId,
            UUID positionId,
            AssignmentType type,
            LocalDate startDate,
            LocalDate endDate,
            AssignmentStatus status,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record PositionRoleResponse(
            UUID id,
            UUID positionId,
            UUID roleId,
            UUID tenantId,
            Instant createdAt
    ) {
    }
}
