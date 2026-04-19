package com.hjo2oa.todo.center.domain;

import java.util.Objects;

public record TodoIdentityContext(
        String tenantId,
        String personId,
        String assignmentId,
        String positionId
) {

    public TodoIdentityContext {
        tenantId = requireText(tenantId, "tenantId");
        personId = requireText(personId, "personId");
        assignmentId = requireText(assignmentId, "assignmentId");
        positionId = requireText(positionId, "positionId");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
