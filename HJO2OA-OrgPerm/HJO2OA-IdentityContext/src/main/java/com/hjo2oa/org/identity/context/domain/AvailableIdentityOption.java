package com.hjo2oa.org.identity.context.domain;

import java.util.Objects;

public record AvailableIdentityOption(
        String assignmentId,
        String positionId,
        String organizationId,
        String departmentId,
        String positionName,
        String organizationName,
        String departmentName,
        IdentityAssignmentType assignmentType,
        boolean current,
        boolean switchable,
        String unavailableReason
) {

    public AvailableIdentityOption {
        assignmentId = requireText(assignmentId, "assignmentId");
        positionId = requireText(positionId, "positionId");
        organizationId = requireText(organizationId, "organizationId");
        departmentId = requireText(departmentId, "departmentId");
        positionName = requireText(positionName, "positionName");
        organizationName = requireText(organizationName, "organizationName");
        departmentName = requireText(departmentName, "departmentName");
        Objects.requireNonNull(assignmentType, "assignmentType must not be null");
        unavailableReason = normalize(unavailableReason);
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
