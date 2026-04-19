package com.hjo2oa.org.identity.context.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public record IdentityContextView(
        String tenantId,
        String personId,
        String accountId,
        String currentAssignmentId,
        String currentPositionId,
        String currentOrganizationId,
        String currentDepartmentId,
        String currentPositionName,
        String currentOrganizationName,
        String currentDepartmentName,
        IdentityAssignmentType assignmentType,
        List<String> roleIds,
        long permissionSnapshotVersion,
        Instant effectiveAt
) {

    public IdentityContextView {
        tenantId = requireText(tenantId, "tenantId");
        personId = requireText(personId, "personId");
        accountId = requireText(accountId, "accountId");
        currentAssignmentId = requireText(currentAssignmentId, "currentAssignmentId");
        currentPositionId = requireText(currentPositionId, "currentPositionId");
        currentOrganizationId = requireText(currentOrganizationId, "currentOrganizationId");
        currentDepartmentId = requireText(currentDepartmentId, "currentDepartmentId");
        currentPositionName = requireText(currentPositionName, "currentPositionName");
        currentOrganizationName = requireText(currentOrganizationName, "currentOrganizationName");
        currentDepartmentName = requireText(currentDepartmentName, "currentDepartmentName");
        Objects.requireNonNull(assignmentType, "assignmentType must not be null");
        roleIds = List.copyOf(new LinkedHashSet<>(Objects.requireNonNullElse(roleIds, List.of())));
        if (permissionSnapshotVersion < 0) {
            throw new IllegalArgumentException("permissionSnapshotVersion must not be negative");
        }
        Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
