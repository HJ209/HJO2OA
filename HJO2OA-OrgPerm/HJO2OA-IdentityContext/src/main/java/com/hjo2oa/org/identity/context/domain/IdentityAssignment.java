package com.hjo2oa.org.identity.context.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public record IdentityAssignment(
        String assignmentId,
        String positionId,
        String organizationId,
        String departmentId,
        String positionName,
        String organizationName,
        String departmentName,
        IdentityAssignmentType assignmentType,
        boolean active,
        List<String> roleIds,
        List<String> permissions,
        String unavailableReason
) {

    public IdentityAssignment {
        assignmentId = requireText(assignmentId, "assignmentId");
        positionId = requireText(positionId, "positionId");
        organizationId = requireText(organizationId, "organizationId");
        departmentId = normalize(departmentId);
        positionName = requireText(positionName, "positionName");
        organizationName = requireText(organizationName, "organizationName");
        departmentName = normalize(departmentName);
        Objects.requireNonNull(assignmentType, "assignmentType must not be null");
        roleIds = List.copyOf(new LinkedHashSet<>(Objects.requireNonNullElse(roleIds, List.of())));
        permissions = List.copyOf(new LinkedHashSet<>(Objects.requireNonNullElse(permissions, List.of())));
        unavailableReason = normalize(unavailableReason);
    }

    public boolean isPrimary() {
        return assignmentType == IdentityAssignmentType.PRIMARY;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
