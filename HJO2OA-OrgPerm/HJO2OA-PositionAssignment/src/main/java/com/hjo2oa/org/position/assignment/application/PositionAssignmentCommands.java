package com.hjo2oa.org.position.assignment.application;

import com.hjo2oa.org.position.assignment.domain.AssignmentType;
import com.hjo2oa.org.position.assignment.domain.PositionCategory;
import java.time.LocalDate;
import java.util.UUID;

public final class PositionAssignmentCommands {

    private PositionAssignmentCommands() {
    }

    public record CreatePositionCommand(
            String code,
            String name,
            UUID organizationId,
            UUID departmentId,
            PositionCategory category,
            Integer level,
            int sortOrder,
            UUID tenantId
    ) {
    }

    public record UpdatePositionCommand(
            UUID positionId,
            String code,
            String name,
            UUID organizationId,
            UUID departmentId,
            PositionCategory category,
            Integer level,
            int sortOrder,
            UUID tenantId
    ) {
    }

    public record CreateAssignmentCommand(
            UUID personId,
            UUID positionId,
            AssignmentType type,
            LocalDate startDate,
            LocalDate endDate,
            UUID tenantId
    ) {
    }

    public record AddPositionRoleCommand(
            UUID positionId,
            UUID roleId,
            UUID tenantId
    ) {
    }
}
