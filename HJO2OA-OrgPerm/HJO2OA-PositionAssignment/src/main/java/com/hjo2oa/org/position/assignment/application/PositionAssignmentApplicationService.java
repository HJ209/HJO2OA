package com.hjo2oa.org.position.assignment.application;

import com.hjo2oa.org.position.assignment.domain.Assignment;
import com.hjo2oa.org.position.assignment.domain.AssignmentStatus;
import com.hjo2oa.org.position.assignment.domain.AssignmentType;
import com.hjo2oa.org.position.assignment.domain.AssignmentView;
import com.hjo2oa.org.position.assignment.domain.Position;
import com.hjo2oa.org.position.assignment.domain.PositionAssignmentRepository;
import com.hjo2oa.org.position.assignment.domain.PositionRole;
import com.hjo2oa.org.position.assignment.domain.PositionRoleView;
import com.hjo2oa.org.position.assignment.domain.PositionStatus;
import com.hjo2oa.org.position.assignment.domain.PositionView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PositionAssignmentApplicationService {

    private static final Comparator<Position> POSITION_ORDER = Comparator
            .comparing(Position::sortOrder)
            .thenComparing(Position::code);

    private static final Comparator<Assignment> ASSIGNMENT_ORDER = Comparator
            .comparing(Assignment::type)
            .thenComparing(assignment -> assignment.startDate() == null ? LocalDate.MIN : assignment.startDate())
            .thenComparing(assignment -> assignment.createdAt());

    private final PositionAssignmentRepository repository;
    private final Clock clock;

    public PositionAssignmentApplicationService(PositionAssignmentRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public PositionAssignmentApplicationService(PositionAssignmentRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public PositionView createPosition(PositionAssignmentCommands.CreatePositionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ensurePositionCodeAvailable(command.tenantId(), command.code(), null);
        Instant now = now();
        Position position = Position.create(
                UUID.randomUUID(),
                command.code(),
                command.name(),
                command.organizationId(),
                command.departmentId(),
                command.category(),
                command.level(),
                command.sortOrder(),
                command.tenantId(),
                now
        );
        return repository.savePosition(position).toView();
    }

    @Transactional
    public PositionView updatePosition(PositionAssignmentCommands.UpdatePositionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Position current = loadPosition(command.positionId());
        ensurePositionCodeAvailable(current.tenantId(), command.code(), current.id());
        Position updated = current.update(
                command.code(),
                command.name(),
                command.organizationId(),
                command.departmentId(),
                command.category(),
                command.level(),
                command.sortOrder(),
                now()
        );
        return repository.savePosition(updated).toView();
    }

    @Transactional
    public PositionView disablePosition(UUID positionId) {
        return repository.savePosition(loadPosition(positionId).disable(now())).toView();
    }

    @Transactional
    public PositionView activatePosition(UUID positionId) {
        return repository.savePosition(loadPosition(positionId).activate(now())).toView();
    }

    public PositionView getPosition(UUID positionId) {
        return loadPosition(positionId).toView();
    }

    public List<PositionView> listPositions(UUID tenantId, UUID organizationId, UUID departmentId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        return repository.findPositions(tenantId, organizationId, departmentId).stream()
                .sorted(POSITION_ORDER)
                .map(Position::toView)
                .toList();
    }

    @Transactional
    public AssignmentView createAssignment(PositionAssignmentCommands.CreateAssignmentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Position position = loadPosition(command.positionId());
        if (!position.tenantId().equals(command.tenantId())) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Position tenant mismatch");
        }
        if (position.status() != PositionStatus.ACTIVE) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Position is disabled");
        }
        ensureNoDuplicateActiveAssignment(command.tenantId(), command.personId(), command.positionId());
        AssignmentType type = Objects.requireNonNull(command.type(), "type must not be null");
        if (type == AssignmentType.PRIMARY) {
            ensureNoActivePrimary(command.tenantId(), command.personId(), null);
        }
        Assignment assignment = Assignment.create(
                UUID.randomUUID(),
                command.personId(),
                command.positionId(),
                type,
                command.startDate(),
                command.endDate(),
                command.tenantId(),
                now()
        );
        return repository.saveAssignment(assignment).toView();
    }

    @Transactional
    public AssignmentView changeAssignmentType(UUID assignmentId, AssignmentType type) {
        Assignment assignment = loadAssignment(assignmentId);
        Objects.requireNonNull(type, "type must not be null");
        if (assignment.status() != AssignmentStatus.ACTIVE) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Assignment is inactive");
        }
        if (type == AssignmentType.PRIMARY) {
            ensureNoActivePrimary(assignment.tenantId(), assignment.personId(), assignment.id());
        }
        return repository.saveAssignment(assignment.changeType(type, now())).toView();
    }

    @Transactional
    public AssignmentView changePrimaryAssignment(UUID personId, UUID assignmentId) {
        Assignment target = loadAssignment(assignmentId);
        if (!target.personId().equals(personId)) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Assignment person mismatch");
        }
        if (target.status() != AssignmentStatus.ACTIVE) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Assignment is inactive");
        }
        repository.findActivePrimaryAssignment(target.tenantId(), personId)
                .filter(current -> !current.id().equals(target.id()))
                .ifPresent(current -> repository.saveAssignment(current.changeType(AssignmentType.SECONDARY, now())));
        return repository.saveAssignment(target.changeType(AssignmentType.PRIMARY, now())).toView();
    }

    @Transactional
    public AssignmentView deactivateAssignment(UUID assignmentId, LocalDate endDate) {
        Assignment assignment = loadAssignment(assignmentId);
        return repository.saveAssignment(assignment.deactivate(endDate, now())).toView();
    }

    public AssignmentView getAssignment(UUID assignmentId) {
        return loadAssignment(assignmentId).toView();
    }

    public List<AssignmentView> listAssignmentsByPerson(UUID tenantId, UUID personId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(personId, "personId must not be null");
        return repository.findAssignmentsByPerson(tenantId, personId).stream()
                .sorted(ASSIGNMENT_ORDER)
                .map(Assignment::toView)
                .toList();
    }

    public List<AssignmentView> listAssignmentsByPosition(UUID tenantId, UUID positionId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(positionId, "positionId must not be null");
        return repository.findAssignmentsByPosition(tenantId, positionId).stream()
                .sorted(ASSIGNMENT_ORDER)
                .map(Assignment::toView)
                .toList();
    }

    @Transactional
    public PositionRoleView addPositionRole(PositionAssignmentCommands.AddPositionRoleCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Position position = loadPosition(command.positionId());
        if (!position.tenantId().equals(command.tenantId())) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Position tenant mismatch");
        }
        return repository.findPositionRole(command.tenantId(), command.positionId(), command.roleId())
                .orElseGet(() -> repository.savePositionRole(PositionRole.create(
                        UUID.randomUUID(),
                        command.positionId(),
                        command.roleId(),
                        command.tenantId(),
                        now()
                )))
                .toView();
    }

    @Transactional
    public void removePositionRole(UUID tenantId, UUID positionId, UUID roleId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(positionId, "positionId must not be null");
        Objects.requireNonNull(roleId, "roleId must not be null");
        repository.findPositionRole(tenantId, positionId, roleId)
                .ifPresent(positionRole -> repository.deletePositionRole(positionRole.id()));
    }

    public List<PositionRoleView> listPositionRoles(UUID tenantId, UUID positionId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(positionId, "positionId must not be null");
        return repository.findRolesByPosition(tenantId, positionId).stream()
                .map(PositionRole::toView)
                .toList();
    }

    private Position loadPosition(UUID positionId) {
        return repository.findPositionById(positionId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Position not found"));
    }

    private Assignment loadAssignment(UUID assignmentId) {
        return repository.findAssignmentById(assignmentId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Assignment not found"));
    }

    private void ensurePositionCodeAvailable(UUID tenantId, String code, UUID currentPositionId) {
        repository.findPositionByCode(tenantId, code)
                .filter(position -> !position.id().equals(currentPositionId))
                .ifPresent(position -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Position code already exists");
                });
    }

    private void ensureNoDuplicateActiveAssignment(UUID tenantId, UUID personId, UUID positionId) {
        repository.findActiveAssignment(tenantId, personId, positionId).ifPresent(assignment -> {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Active assignment already exists");
        });
    }

    private void ensureNoActivePrimary(UUID tenantId, UUID personId, UUID excludedAssignmentId) {
        repository.findActivePrimaryAssignment(tenantId, personId)
                .filter(assignment -> !assignment.id().equals(excludedAssignmentId))
                .ifPresent(assignment -> {
                    throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Primary assignment exists");
                });
    }

    private Instant now() {
        return clock.instant();
    }
}
