package com.hjo2oa.org.position.assignment.application;

import com.hjo2oa.org.position.assignment.domain.Assignment;
import com.hjo2oa.org.position.assignment.domain.AssignmentStatus;
import com.hjo2oa.org.position.assignment.domain.AssignmentType;
import com.hjo2oa.org.position.assignment.domain.AssignmentView;
import com.hjo2oa.org.position.assignment.domain.Position;
import com.hjo2oa.org.position.assignment.domain.PositionAssignmentChangedEvent;
import com.hjo2oa.org.position.assignment.domain.PositionAssignmentRepository;
import com.hjo2oa.org.position.assignment.domain.PositionRole;
import com.hjo2oa.org.position.assignment.domain.PositionRoleView;
import com.hjo2oa.org.position.assignment.domain.PositionStatus;
import com.hjo2oa.org.position.assignment.domain.PositionView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import com.hjo2oa.shared.tenant.TenantContextHolder;
import com.hjo2oa.shared.tenant.TenantRequestContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final DomainEventPublisher domainEventPublisher;
    private final PositionAssignmentReferenceValidator referenceValidator;
    private final AssignmentHistoryRecorder assignmentHistoryRecorder;

    @Autowired
    public PositionAssignmentApplicationService(
            PositionAssignmentRepository repository,
            ObjectProvider<DomainEventPublisher> domainEventPublisherProvider,
            ObjectProvider<PositionAssignmentReferenceValidator> referenceValidatorProvider,
            ObjectProvider<AssignmentHistoryRecorder> assignmentHistoryRecorderProvider
    ) {
        this(
                repository,
                Clock.systemUTC(),
                domainEventPublisherProvider.getIfAvailable(() -> event -> { }),
                referenceValidatorProvider.getIfAvailable(() -> PositionAssignmentReferenceValidator.NOOP),
                assignmentHistoryRecorderProvider.getIfAvailable(() -> AssignmentHistoryRecorder.NOOP)
        );
    }

    public PositionAssignmentApplicationService(PositionAssignmentRepository repository, Clock clock) {
        this(
                repository,
                clock,
                event -> { },
                PositionAssignmentReferenceValidator.NOOP,
                AssignmentHistoryRecorder.NOOP
        );
    }

    public PositionAssignmentApplicationService(
            PositionAssignmentRepository repository,
            Clock clock,
            DomainEventPublisher domainEventPublisher,
            PositionAssignmentReferenceValidator referenceValidator,
            AssignmentHistoryRecorder assignmentHistoryRecorder
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.referenceValidator = Objects.requireNonNull(referenceValidator, "referenceValidator must not be null");
        this.assignmentHistoryRecorder = Objects.requireNonNull(
                assignmentHistoryRecorder,
                "assignmentHistoryRecorder must not be null"
        );
    }

    @Transactional
    public PositionView createPosition(PositionAssignmentCommands.CreatePositionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ensurePositionCodeAvailable(command.tenantId(), command.code(), null);
        referenceValidator.ensurePositionScopeActive(command.tenantId(), command.organizationId(), command.departmentId());
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
        Position saved = repository.savePosition(position);
        publishPositionEvent("org.position.created", saved, payloadOf("departmentId", saved.departmentId()));
        return saved.toView();
    }

    @Transactional
    public PositionView updatePosition(PositionAssignmentCommands.UpdatePositionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Position current = loadPosition(command.tenantId(), command.positionId());
        ensurePositionCodeAvailable(current.tenantId(), command.code(), current.id());
        referenceValidator.ensurePositionScopeActive(
                current.tenantId(),
                command.organizationId(),
                command.departmentId()
        );
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
        Position saved = repository.savePosition(updated);
        publishPositionEvent("org.position.updated", saved, payloadOf("departmentId", saved.departmentId()));
        return saved.toView();
    }

    @Transactional
    public PositionView disablePosition(UUID tenantId, UUID positionId) {
        Position saved = repository.savePosition(loadPosition(tenantId, positionId).disable(now()));
        publishPositionEvent("org.position.disabled", saved, Map.of());
        return saved.toView();
    }

    @Transactional
    public PositionView activatePosition(UUID tenantId, UUID positionId) {
        Position current = loadPosition(tenantId, positionId);
        referenceValidator.ensurePositionScopeActive(current.tenantId(), current.organizationId(), current.departmentId());
        Position saved = repository.savePosition(current.activate(now()));
        publishPositionEvent("org.position.enabled", saved, Map.of());
        return saved.toView();
    }

    public PositionView getPosition(UUID positionId) {
        return loadPosition(positionId).toView();
    }

    public PositionView getPosition(UUID tenantId, UUID positionId) {
        return loadPosition(tenantId, positionId).toView();
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
        Position position = loadPosition(command.tenantId(), command.positionId());
        if (position.status() != PositionStatus.ACTIVE) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Position is disabled");
        }
        referenceValidator.ensurePositionScopeActive(command.tenantId(), position.organizationId(), position.departmentId());
        referenceValidator.ensurePersonAssignable(command.tenantId(), command.personId());
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
        Assignment saved = repository.saveAssignment(assignment);
        assignmentHistoryRecorder.record(saved, "CREATED", now());
        publishAssignmentEvent("org.assignment.created", saved, payloadOf("type", saved.type().name()));
        return saved.toView();
    }

    @Transactional
    public AssignmentView changeAssignmentType(UUID tenantId, UUID assignmentId, AssignmentType type) {
        Assignment assignment = loadAssignment(tenantId, assignmentId);
        Objects.requireNonNull(type, "type must not be null");
        if (assignment.status() != AssignmentStatus.ACTIVE) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Assignment is inactive");
        }
        if (type == AssignmentType.PRIMARY) {
            ensureNoActivePrimary(assignment.tenantId(), assignment.personId(), assignment.id());
        }
        Assignment saved = repository.saveAssignment(assignment.changeType(type, now()));
        assignmentHistoryRecorder.record(saved, "TYPE_CHANGED", now());
        publishAssignmentEvent("org.assignment.updated", saved, payloadOf("type", saved.type().name()));
        return saved.toView();
    }

    @Transactional
    public AssignmentView changePrimaryAssignment(UUID tenantId, UUID personId, UUID assignmentId) {
        Assignment target = loadAssignment(tenantId, assignmentId);
        if (!target.personId().equals(personId)) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Assignment person mismatch");
        }
        if (target.status() != AssignmentStatus.ACTIVE) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Assignment is inactive");
        }
        referenceValidator.ensurePersonAssignable(tenantId, personId);
        repository.findActivePrimaryAssignment(target.tenantId(), personId)
                .filter(current -> !current.id().equals(target.id()))
                .ifPresent(current -> {
                    Assignment changed = repository.saveAssignment(current.changeType(AssignmentType.SECONDARY, now()));
                    assignmentHistoryRecorder.record(changed, "PRIMARY_REPLACED", now());
                    publishAssignmentEvent("org.assignment.updated", changed, payloadOf("type", changed.type().name()));
                });
        Assignment saved = repository.saveAssignment(target.changeType(AssignmentType.PRIMARY, now()));
        assignmentHistoryRecorder.record(saved, "PRIMARY_ASSIGNED", now());
        publishAssignmentEvent("org.assignment.primary_changed", saved, payloadOf("personId", personId));
        return saved.toView();
    }

    @Transactional
    public AssignmentView deactivateAssignment(UUID tenantId, UUID assignmentId, LocalDate endDate) {
        Assignment assignment = loadAssignment(tenantId, assignmentId);
        Assignment saved = repository.saveAssignment(assignment.deactivate(endDate, now()));
        assignmentHistoryRecorder.record(saved, "DEACTIVATED", now());
        publishAssignmentEvent("org.assignment.ended", saved, payloadOf("endDate", saved.endDate()));
        return saved.toView();
    }

    public AssignmentView getAssignment(UUID assignmentId) {
        return loadAssignment(assignmentId).toView();
    }

    public AssignmentView getAssignment(UUID tenantId, UUID assignmentId) {
        return loadAssignment(tenantId, assignmentId).toView();
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
        Position position = loadPosition(command.tenantId(), command.positionId());
        if (!position.tenantId().equals(command.tenantId())) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Position tenant mismatch");
        }
        PositionRole role = repository.findPositionRole(command.tenantId(), command.positionId(), command.roleId())
                .orElseGet(() -> repository.savePositionRole(PositionRole.create(
                        UUID.randomUUID(),
                        command.positionId(),
                        command.roleId(),
                        command.tenantId(),
                        now()
                )));
        publishPositionEvent("org.position.role_bound", position, payloadOf("roleId", command.roleId()));
        return role.toView();
    }

    @Transactional
    public void removePositionRole(UUID tenantId, UUID positionId, UUID roleId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(positionId, "positionId must not be null");
        Objects.requireNonNull(roleId, "roleId must not be null");
        Position position = loadPosition(tenantId, positionId);
        repository.findPositionRole(tenantId, positionId, roleId)
                .ifPresent(positionRole -> {
                    repository.deletePositionRole(positionRole.id());
                    publishPositionEvent("org.position.role_unbound", position, payloadOf("roleId", roleId));
                });
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

    private Position loadPosition(UUID tenantId, UUID positionId) {
        Position position = loadPosition(positionId);
        if (!position.tenantId().equals(tenantId)) {
            throw new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Position not found");
        }
        return position;
    }

    private Assignment loadAssignment(UUID assignmentId) {
        return repository.findAssignmentById(assignmentId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Assignment not found"));
    }

    private Assignment loadAssignment(UUID tenantId, UUID assignmentId) {
        Assignment assignment = loadAssignment(assignmentId);
        if (!assignment.tenantId().equals(tenantId)) {
            throw new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Assignment not found");
        }
        return assignment;
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

    private void publishPositionEvent(String eventType, Position position, Map<String, Object> details) {
        domainEventPublisher.publish(PositionAssignmentChangedEvent.of(
                eventType,
                position.tenantId(),
                position.id(),
                now(),
                eventPayload(details)
        ));
    }

    private void publishAssignmentEvent(String eventType, Assignment assignment, Map<String, Object> details) {
        domainEventPublisher.publish(PositionAssignmentChangedEvent.of(
                eventType,
                assignment.tenantId(),
                assignment.id(),
                now(),
                eventPayload(details)
        ));
    }

    private Map<String, Object> eventPayload(Map<String, Object> details) {
        TenantRequestContext context = TenantContextHolder.current().orElse(null);
        Map<String, Object> payload = new LinkedHashMap<>();
        if (context != null) {
            putIfNotNull(payload, "requestId", context.requestId());
            putIfNotNull(payload, "idempotencyKey", context.idempotencyKey());
            putIfNotNull(payload, "language", context.language().toLanguageTag());
            putIfNotNull(payload, "timezone", context.timezone().getId());
            putIfNotNull(payload, "identityAssignmentId", context.identityAssignmentId());
            putIfNotNull(payload, "identityPositionId", context.identityPositionId());
        }
        if (details != null) {
            details.forEach((key, value) -> putIfNotNull(payload, key, value));
        }
        return payload;
    }

    private Map<String, Object> payloadOf(Object... keysAndValues) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
            Object key = keysAndValues[i];
            if (key instanceof String name) {
                putIfNotNull(payload, name, keysAndValues[i + 1]);
            }
        }
        return payload;
    }

    private void putIfNotNull(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }
}
