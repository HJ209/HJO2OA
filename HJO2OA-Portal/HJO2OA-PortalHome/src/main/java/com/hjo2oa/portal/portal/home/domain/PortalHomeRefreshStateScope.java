package com.hjo2oa.portal.portal.home.domain;

import java.util.Objects;

public record PortalHomeRefreshStateScope(
        String tenantId,
        String personId,
        String assignmentId,
        PortalHomeSceneType sceneType
) {

    public PortalHomeRefreshStateScope {
        tenantId = requireText(tenantId, "tenantId");
        personId = normalize(personId);
        assignmentId = normalize(assignmentId);
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        if (assignmentId != null && personId == null) {
            throw new IllegalArgumentException("assignmentId requires personId");
        }
    }

    public static PortalHomeRefreshStateScope ofTenant(
            String tenantId,
            PortalHomeSceneType sceneType
    ) {
        return new PortalHomeRefreshStateScope(tenantId, null, null, sceneType);
    }

    public static PortalHomeRefreshStateScope ofPerson(
            String tenantId,
            String personId,
            PortalHomeSceneType sceneType
    ) {
        return new PortalHomeRefreshStateScope(tenantId, personId, null, sceneType);
    }

    public static PortalHomeRefreshStateScope ofIdentity(
            String tenantId,
            String personId,
            String assignmentId,
            PortalHomeSceneType sceneType
    ) {
        return new PortalHomeRefreshStateScope(tenantId, personId, assignmentId, sceneType);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
