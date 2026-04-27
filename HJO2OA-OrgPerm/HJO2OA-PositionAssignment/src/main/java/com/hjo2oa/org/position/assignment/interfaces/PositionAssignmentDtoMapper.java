package com.hjo2oa.org.position.assignment.interfaces;

import com.hjo2oa.org.position.assignment.domain.AssignmentView;
import com.hjo2oa.org.position.assignment.domain.PositionRoleView;
import com.hjo2oa.org.position.assignment.domain.PositionView;
import org.springframework.stereotype.Component;

@Component
public class PositionAssignmentDtoMapper {

    public PositionAssignmentDtos.PositionResponse toPositionResponse(PositionView view) {
        return new PositionAssignmentDtos.PositionResponse(
                view.id(),
                view.code(),
                view.name(),
                view.organizationId(),
                view.departmentId(),
                view.category(),
                view.level(),
                view.sortOrder(),
                view.status(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public PositionAssignmentDtos.AssignmentResponse toAssignmentResponse(AssignmentView view) {
        return new PositionAssignmentDtos.AssignmentResponse(
                view.id(),
                view.personId(),
                view.positionId(),
                view.type(),
                view.startDate(),
                view.endDate(),
                view.status(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public PositionAssignmentDtos.PositionRoleResponse toPositionRoleResponse(PositionRoleView view) {
        return new PositionAssignmentDtos.PositionRoleResponse(
                view.id(),
                view.positionId(),
                view.roleId(),
                view.tenantId(),
                view.createdAt()
        );
    }
}
