package com.hjo2oa.portal.aggregation.api.domain;

import java.util.Objects;

public record PortalSnapshotScope(
        String tenantId,
        String personId,
        String assignmentId,
        String positionId
) {

    public PortalSnapshotScope {
        tenantId = requireText(tenantId, "tenantId");
        personId = normalize(personId);
        assignmentId = normalize(assignmentId);
        positionId = normalize(positionId);
    }

    public static PortalSnapshotScope ofIdentity(
            String tenantId,
            String personId,
            String assignmentId,
            String positionId
    ) {
        return new PortalSnapshotScope(tenantId, personId, assignmentId, positionId);
    }

    public static PortalSnapshotScope ofAssignment(String tenantId, String assignmentId) {
        return new PortalSnapshotScope(tenantId, null, assignmentId, null);
    }

    public boolean matches(PortalAggregationSnapshotKey snapshotKey) {
        Objects.requireNonNull(snapshotKey, "snapshotKey must not be null");
        if (!tenantId.equals(snapshotKey.tenantId())) {
            return false;
        }
        if (personId != null && !personId.equals(snapshotKey.personId())) {
            return false;
        }
        if (assignmentId != null && !assignmentId.equals(snapshotKey.assignmentId())) {
            return false;
        }
        return positionId == null || positionId.equals(snapshotKey.positionId());
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
