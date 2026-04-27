package com.hjo2oa.org.position.assignment.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PositionAssignmentRepository {

    Optional<Position> findPositionById(UUID positionId);

    Optional<Position> findPositionByCode(UUID tenantId, String code);

    List<Position> findPositions(UUID tenantId, UUID organizationId, UUID departmentId);

    Position savePosition(Position position);

    Optional<Assignment> findAssignmentById(UUID assignmentId);

    List<Assignment> findAssignmentsByPerson(UUID tenantId, UUID personId);

    List<Assignment> findAssignmentsByPosition(UUID tenantId, UUID positionId);

    Optional<Assignment> findActiveAssignment(UUID tenantId, UUID personId, UUID positionId);

    Optional<Assignment> findActivePrimaryAssignment(UUID tenantId, UUID personId);

    Assignment saveAssignment(Assignment assignment);

    List<PositionRole> findRolesByPosition(UUID tenantId, UUID positionId);

    Optional<PositionRole> findPositionRole(UUID tenantId, UUID positionId, UUID roleId);

    PositionRole savePositionRole(PositionRole positionRole);

    void deletePositionRole(UUID positionRoleId);
}
