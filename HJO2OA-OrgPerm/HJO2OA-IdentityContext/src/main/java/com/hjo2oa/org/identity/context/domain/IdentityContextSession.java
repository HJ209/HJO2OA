package com.hjo2oa.org.identity.context.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record IdentityContextSession(
        String tenantId,
        String personId,
        String accountId,
        String currentAssignmentId,
        long permissionSnapshotVersion,
        Instant effectiveAt,
        List<IdentityAssignment> assignments
) {

    public IdentityContextSession {
        tenantId = requireText(tenantId, "tenantId");
        personId = requireText(personId, "personId");
        accountId = requireText(accountId, "accountId");
        currentAssignmentId = requireText(currentAssignmentId, "currentAssignmentId");
        if (permissionSnapshotVersion < 0) {
            throw new IllegalArgumentException("permissionSnapshotVersion must not be negative");
        }
        Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");
        assignments = List.copyOf(Objects.requireNonNull(assignments, "assignments must not be null"));
        if (assignments.isEmpty()) {
            throw new IllegalArgumentException("assignments must not be empty");
        }
    }

    public IdentityContextView currentContext() {
        IdentityAssignment currentAssignment = requireCurrentAssignment();
        return new IdentityContextView(
                tenantId,
                personId,
                accountId,
                currentAssignment.assignmentId(),
                currentAssignment.positionId(),
                currentAssignment.organizationId(),
                currentAssignment.departmentId(),
                currentAssignment.positionName(),
                currentAssignment.organizationName(),
                currentAssignment.departmentName(),
                currentAssignment.assignmentType(),
                currentAssignment.roleIds(),
                currentAssignment.permissions(),
                permissionSnapshotVersion,
                effectiveAt
        );
    }

    public List<AvailableIdentityOption> availableOptions(boolean includePrimary) {
        return assignments.stream()
                .filter(assignment -> includePrimary || !assignment.isPrimary())
                .map(this::toAvailableOption)
                .toList();
    }

    public Optional<IdentityAssignment> findByPositionId(String positionId) {
        return assignments.stream()
                .filter(assignment -> assignment.positionId().equals(positionId))
                .findFirst();
    }

    public Optional<IdentityAssignment> findByAssignmentId(String assignmentId) {
        return assignments.stream()
                .filter(assignment -> assignment.assignmentId().equals(assignmentId))
                .findFirst();
    }

    public Optional<IdentityAssignment> primaryAssignment() {
        return assignments.stream()
                .filter(IdentityAssignment::isPrimary)
                .findFirst();
    }

    public IdentityContextSession withCurrentAssignment(String assignmentId, long newVersion, Instant newEffectiveAt) {
        IdentityAssignment assignment = findByAssignmentId(requireText(assignmentId, "assignmentId"))
                .orElseThrow(() -> new IllegalArgumentException("assignmentId does not exist: " + assignmentId));
        return new IdentityContextSession(
                tenantId,
                personId,
                accountId,
                assignment.assignmentId(),
                newVersion,
                Objects.requireNonNull(newEffectiveAt, "newEffectiveAt must not be null"),
                assignments
        );
    }

    public IdentityContextSession withPermissionSnapshotVersion(long newVersion, Instant newEffectiveAt) {
        return new IdentityContextSession(
                tenantId,
                personId,
                accountId,
                currentAssignmentId,
                newVersion,
                Objects.requireNonNull(newEffectiveAt, "newEffectiveAt must not be null"),
                assignments
        );
    }

    private AvailableIdentityOption toAvailableOption(IdentityAssignment assignment) {
        boolean current = assignment.assignmentId().equals(currentAssignmentId);
        boolean switchable = assignment.active() && assignment.assignmentType() == IdentityAssignmentType.SECONDARY && !current;

        String unavailableReason = assignment.unavailableReason();
        if (unavailableReason == null) {
            if (current) {
                unavailableReason = "CURRENT_ASSIGNMENT";
            } else if (assignment.isPrimary()) {
                unavailableReason = "USE_RESET_PRIMARY";
            }
        }

        return new AvailableIdentityOption(
                assignment.assignmentId(),
                assignment.positionId(),
                assignment.organizationId(),
                assignment.departmentId(),
                assignment.positionName(),
                assignment.organizationName(),
                assignment.departmentName(),
                assignment.assignmentType(),
                current,
                switchable,
                switchable ? null : unavailableReason
        );
    }

    private IdentityAssignment requireCurrentAssignment() {
        IdentityAssignment currentAssignment = findByAssignmentId(currentAssignmentId)
                .orElseThrow(() -> new IllegalStateException("Current assignment is not present in session"));
        if (!currentAssignment.active()) {
            throw new IllegalStateException("Current assignment is inactive");
        }
        return currentAssignment;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
