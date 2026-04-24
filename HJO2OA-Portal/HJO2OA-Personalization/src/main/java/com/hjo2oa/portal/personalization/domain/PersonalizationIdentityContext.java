package com.hjo2oa.portal.personalization.domain;

import java.util.Objects;

public record PersonalizationIdentityContext(
        String tenantId,
        String personId,
        String assignmentId,
        String positionId
) {

    public PersonalizationIdentityContext {
        tenantId = requireText(tenantId, "tenantId");
        personId = requireText(personId, "personId");
        assignmentId = requireText(assignmentId, "assignmentId");
        positionId = requireText(positionId, "positionId");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
