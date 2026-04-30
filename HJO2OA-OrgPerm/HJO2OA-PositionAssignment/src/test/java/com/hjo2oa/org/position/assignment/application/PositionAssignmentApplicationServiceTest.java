package com.hjo2oa.org.position.assignment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.org.position.assignment.domain.Assignment;
import com.hjo2oa.org.position.assignment.domain.AssignmentType;
import com.hjo2oa.org.position.assignment.domain.AssignmentView;
import com.hjo2oa.org.position.assignment.domain.Position;
import com.hjo2oa.org.position.assignment.domain.PositionAssignmentRepository;
import com.hjo2oa.org.position.assignment.domain.PositionCategory;
import com.hjo2oa.org.position.assignment.domain.PositionRole;
import com.hjo2oa.org.position.assignment.domain.PositionView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PositionAssignmentApplicationServiceTest {

    private static final UUID TENANT_A = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ORG_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID PERSON_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private PositionAssignmentApplicationService service;
    private ScopeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ScopeValidator();
        service = new PositionAssignmentApplicationService(
                new InMemoryPositionAssignmentRepository(),
                CLOCK,
                new RecordingPublisher(),
                validator,
                (assignment, action, changedAt) -> { }
        );
    }

    @Test
    void primaryAssignmentIsUniquePerPersonAndTenant() {
        PositionView manager = createPosition("MGR");
        PositionView engineer = createPosition("ENG");

        AssignmentView primary = service.createAssignment(new PositionAssignmentCommands.CreateAssignmentCommand(
                PERSON_ID,
                manager.id(),
                AssignmentType.PRIMARY,
                null,
                null,
                TENANT_A
        ));

        assertThat(primary.type()).isEqualTo(AssignmentType.PRIMARY);
        assertThatThrownBy(() -> service.createAssignment(new PositionAssignmentCommands.CreateAssignmentCommand(
                PERSON_ID,
                engineer.id(),
                AssignmentType.PRIMARY,
                null,
                null,
                TENANT_A
        ))).isInstanceOf(BizException.class)
                .hasMessageContaining("Primary assignment exists");
    }

    @Test
    void assignmentCannotCrossTenant() {
        PositionView position = createPosition("MGR");

        assertThatThrownBy(() -> service.createAssignment(new PositionAssignmentCommands.CreateAssignmentCommand(
                PERSON_ID,
                position.id(),
                AssignmentType.PRIMARY,
                null,
                null,
                TENANT_B
        ))).isInstanceOf(BizException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void inactivePersonCannotBeAssigned() {
        PositionView position = createPosition("MGR");
        validator.personActive = false;

        assertThatThrownBy(() -> service.createAssignment(new PositionAssignmentCommands.CreateAssignmentCommand(
                PERSON_ID,
                position.id(),
                AssignmentType.PRIMARY,
                null,
                null,
                TENANT_A
        ))).isInstanceOf(BizException.class)
                .hasMessageContaining("Person is missing or not active");
    }

    private PositionView createPosition(String code) {
        return service.createPosition(new PositionAssignmentCommands.CreatePositionCommand(
                code,
                code + " Position",
                ORG_ID,
                null,
                PositionCategory.MANAGEMENT,
                1,
                0,
                TENANT_A
        ));
    }

    private static final class ScopeValidator implements PositionAssignmentReferenceValidator {

        private boolean personActive = true;

        @Override
        public void ensurePersonAssignable(UUID tenantId, UUID personId) {
            if (!personActive) {
                throw new BizException(
                        com.hjo2oa.shared.kernel.SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                        "Person is missing or not active"
                );
            }
        }
    }

    private static final class InMemoryPositionAssignmentRepository implements PositionAssignmentRepository {

        private final Map<UUID, Position> positions = new HashMap<>();
        private final Map<UUID, Assignment> assignments = new HashMap<>();
        private final Map<UUID, PositionRole> roles = new HashMap<>();

        @Override
        public Optional<Position> findPositionById(UUID positionId) {
            return Optional.ofNullable(positions.get(positionId));
        }

        @Override
        public Optional<Position> findPositionByCode(UUID tenantId, String code) {
            return positions.values().stream()
                    .filter(position -> position.tenantId().equals(tenantId))
                    .filter(position -> position.code().equals(code))
                    .findFirst();
        }

        @Override
        public List<Position> findPositions(UUID tenantId, UUID organizationId, UUID departmentId) {
            return positions.values().stream()
                    .filter(position -> position.tenantId().equals(tenantId))
                    .toList();
        }

        @Override
        public Position savePosition(Position position) {
            positions.put(position.id(), position);
            return position;
        }

        @Override
        public Optional<Assignment> findAssignmentById(UUID assignmentId) {
            return Optional.ofNullable(assignments.get(assignmentId));
        }

        @Override
        public List<Assignment> findAssignmentsByPerson(UUID tenantId, UUID personId) {
            return assignments.values().stream()
                    .filter(assignment -> assignment.tenantId().equals(tenantId))
                    .filter(assignment -> assignment.personId().equals(personId))
                    .toList();
        }

        @Override
        public List<Assignment> findAssignmentsByPosition(UUID tenantId, UUID positionId) {
            return assignments.values().stream()
                    .filter(assignment -> assignment.tenantId().equals(tenantId))
                    .filter(assignment -> assignment.positionId().equals(positionId))
                    .toList();
        }

        @Override
        public Optional<Assignment> findActiveAssignment(UUID tenantId, UUID personId, UUID positionId) {
            return assignments.values().stream()
                    .filter(assignment -> assignment.tenantId().equals(tenantId))
                    .filter(assignment -> assignment.personId().equals(personId))
                    .filter(assignment -> assignment.positionId().equals(positionId))
                    .filter(assignment -> assignment.status().name().equals("ACTIVE"))
                    .findFirst();
        }

        @Override
        public Optional<Assignment> findActivePrimaryAssignment(UUID tenantId, UUID personId) {
            return assignments.values().stream()
                    .filter(assignment -> assignment.tenantId().equals(tenantId))
                    .filter(assignment -> assignment.personId().equals(personId))
                    .filter(assignment -> assignment.type() == AssignmentType.PRIMARY)
                    .filter(assignment -> assignment.status().name().equals("ACTIVE"))
                    .findFirst();
        }

        @Override
        public Assignment saveAssignment(Assignment assignment) {
            assignments.put(assignment.id(), assignment);
            return assignment;
        }

        @Override
        public List<PositionRole> findRolesByPosition(UUID tenantId, UUID positionId) {
            return roles.values().stream()
                    .filter(role -> role.tenantId().equals(tenantId))
                    .filter(role -> role.positionId().equals(positionId))
                    .toList();
        }

        @Override
        public Optional<PositionRole> findPositionRole(UUID tenantId, UUID positionId, UUID roleId) {
            return roles.values().stream()
                    .filter(role -> role.tenantId().equals(tenantId))
                    .filter(role -> role.positionId().equals(positionId))
                    .filter(role -> role.roleId().equals(roleId))
                    .findFirst();
        }

        @Override
        public PositionRole savePositionRole(PositionRole positionRole) {
            roles.put(positionRole.id(), positionRole);
            return positionRole;
        }

        @Override
        public void deletePositionRole(UUID positionRoleId) {
            roles.remove(positionRoleId);
        }
    }

    private static final class RecordingPublisher implements com.hjo2oa.shared.messaging.DomainEventPublisher {

        @Override
        public void publish(DomainEvent event) {
        }
    }
}
