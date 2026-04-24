package com.hjo2oa.portal.personalization.domain;

import java.util.Objects;

public record PersonalizationProfileKey(
        String tenantId,
        String personId,
        String assignmentId,
        PersonalizationSceneType sceneType
) {

    public PersonalizationProfileKey {
        tenantId = requireText(tenantId, "tenantId");
        personId = requireText(personId, "personId");
        assignmentId = normalizeOptional(assignmentId);
        Objects.requireNonNull(sceneType, "sceneType must not be null");
    }

    public static PersonalizationProfileKey ofAssignment(
            String tenantId,
            String personId,
            String assignmentId,
            PersonalizationSceneType sceneType
    ) {
        return new PersonalizationProfileKey(tenantId, personId, requireText(assignmentId, "assignmentId"), sceneType);
    }

    public static PersonalizationProfileKey ofGlobal(
            String tenantId,
            String personId,
            PersonalizationSceneType sceneType
    ) {
        return new PersonalizationProfileKey(tenantId, personId, null, sceneType);
    }

    public PersonalizationProfileScope scope() {
        return assignmentId == null ? PersonalizationProfileScope.GLOBAL : PersonalizationProfileScope.ASSIGNMENT;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
