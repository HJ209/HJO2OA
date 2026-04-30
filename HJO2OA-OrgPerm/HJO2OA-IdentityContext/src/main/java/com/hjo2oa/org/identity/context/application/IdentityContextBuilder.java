package com.hjo2oa.org.identity.context.application;

import com.hjo2oa.org.identity.context.domain.IdentityAssignment;
import com.hjo2oa.org.identity.context.domain.IdentityAssignmentType;
import com.hjo2oa.org.identity.context.domain.IdentityContextSession;
import com.hjo2oa.org.org.structure.domain.Department;
import com.hjo2oa.org.org.structure.domain.DepartmentRepository;
import com.hjo2oa.org.org.structure.domain.Organization;
import com.hjo2oa.org.org.structure.domain.OrganizationRepository;
import com.hjo2oa.org.person.account.application.PersonAccountApplicationService.AuthenticatedAccount;
import com.hjo2oa.org.position.assignment.domain.Assignment;
import com.hjo2oa.org.position.assignment.domain.AssignmentStatus;
import com.hjo2oa.org.position.assignment.domain.AssignmentType;
import com.hjo2oa.org.position.assignment.domain.Position;
import com.hjo2oa.org.position.assignment.domain.PositionAssignmentRepository;
import com.hjo2oa.org.position.assignment.domain.PositionStatus;
import com.hjo2oa.org.role.resource.auth.application.PermissionCalculator;
import com.hjo2oa.org.role.resource.auth.domain.PermissionSnapshot;
import com.hjo2oa.org.role.resource.auth.domain.ResourcePermissionView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IdentityContextBuilder {

    private final PositionAssignmentRepository positionAssignmentRepository;
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final PermissionCalculator permissionCalculator;
    private final Clock clock;

    @Autowired
    public IdentityContextBuilder(
            PositionAssignmentRepository positionAssignmentRepository,
            OrganizationRepository organizationRepository,
            DepartmentRepository departmentRepository,
            PermissionCalculator permissionCalculator
    ) {
        this(
                positionAssignmentRepository,
                organizationRepository,
                departmentRepository,
                permissionCalculator,
                Clock.systemUTC()
        );
    }

    public IdentityContextBuilder(
            PositionAssignmentRepository positionAssignmentRepository,
            OrganizationRepository organizationRepository,
            DepartmentRepository departmentRepository,
            PermissionCalculator permissionCalculator,
            Clock clock
    ) {
        this.positionAssignmentRepository = Objects.requireNonNull(
                positionAssignmentRepository,
                "positionAssignmentRepository must not be null"
        );
        this.organizationRepository = Objects.requireNonNull(
                organizationRepository,
                "organizationRepository must not be null"
        );
        this.departmentRepository = Objects.requireNonNull(departmentRepository, "departmentRepository must not be null");
        this.permissionCalculator = Objects.requireNonNull(permissionCalculator, "permissionCalculator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public IdentityContextSession build(AuthenticatedAccount account, String preferredAssignmentId) {
        Objects.requireNonNull(account, "account must not be null");
        List<Assignment> assignments = positionAssignmentRepository
                .findAssignmentsByPerson(account.tenantId(), account.personId())
                .stream()
                .sorted(Comparator.comparing(Assignment::type).thenComparing(Assignment::createdAt))
                .toList();
        Assignment primary = assignments.stream()
                .filter(assignment -> assignment.type() == AssignmentType.PRIMARY)
                .filter(this::assignmentActive)
                .findFirst()
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.CONFLICT,
                        "Current person has no active primary assignment"
                ));
        String currentAssignmentId = selectCurrentAssignment(assignments, primary, preferredAssignmentId);
        List<IdentityAssignment> identityAssignments = assignments.stream()
                .map(assignment -> toIdentityAssignment(account, assignment))
                .toList();
        PermissionSnapshot currentSnapshot = permissionSnapshot(account, UUID.fromString(
                identityAssignments.stream()
                        .filter(assignment -> assignment.assignmentId().equals(currentAssignmentId))
                        .findFirst()
                        .orElseThrow()
                        .positionId()
        ));
        return new IdentityContextSession(
                account.tenantId().toString(),
                account.personId().toString(),
                account.accountId().toString(),
                currentAssignmentId,
                currentSnapshot.version(),
                Instant.now(clock),
                identityAssignments
        );
    }

    private String selectCurrentAssignment(
            List<Assignment> assignments,
            Assignment primary,
            String preferredAssignmentId
    ) {
        if (preferredAssignmentId == null || preferredAssignmentId.isBlank()) {
            return primary.id().toString();
        }
        return assignments.stream()
                .filter(assignment -> assignment.id().toString().equals(preferredAssignmentId))
                .filter(this::assignmentActive)
                .findFirst()
                .map(assignment -> assignment.id().toString())
                .orElse(primary.id().toString());
    }

    private IdentityAssignment toIdentityAssignment(AuthenticatedAccount account, Assignment assignment) {
        Position position = positionAssignmentRepository.findPositionById(assignment.positionId())
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Position not found for identity context"
                ));
        Organization organization = organizationRepository.findById(position.organizationId())
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Organization not found for identity context"
                ));
        Department department = position.departmentId() == null
                ? null
                : departmentRepository.findById(position.departmentId()).orElse(null);
        PermissionSnapshot snapshot = permissionSnapshot(account, position.id());
        boolean active = assignmentActive(assignment) && position.status() == PositionStatus.ACTIVE;
        return new IdentityAssignment(
                assignment.id().toString(),
                position.id().toString(),
                organization.id().toString(),
                department == null ? null : department.id().toString(),
                position.name(),
                organization.name(),
                department == null ? null : department.name(),
                assignment.type() == AssignmentType.PRIMARY ? IdentityAssignmentType.PRIMARY : IdentityAssignmentType.SECONDARY,
                active,
                snapshot.roleIds().stream().map(UUID::toString).toList(),
                snapshot.resourcePermissions().stream().map(this::permissionCode).toList(),
                active ? null : "ASSIGNMENT_OR_POSITION_INACTIVE"
        );
    }

    private PermissionSnapshot permissionSnapshot(AuthenticatedAccount account, UUID positionId) {
        return permissionCalculator.calculate(account.tenantId(), account.personId(), positionId);
    }

    private boolean assignmentActive(Assignment assignment) {
        LocalDate today = LocalDate.now(clock);
        return assignment.status() == AssignmentStatus.ACTIVE
                && (assignment.startDate() == null || !assignment.startDate().isAfter(today))
                && (assignment.endDate() == null || !assignment.endDate().isBefore(today));
    }

    private String permissionCode(ResourcePermissionView permission) {
        return permission.resourceType() + ":" + permission.resourceCode() + ":" + permission.action();
    }
}
