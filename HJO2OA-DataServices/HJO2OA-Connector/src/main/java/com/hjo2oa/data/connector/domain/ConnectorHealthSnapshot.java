package com.hjo2oa.data.connector.domain;

import java.time.Instant;
import java.util.Objects;

public record ConnectorHealthSnapshot(
        String snapshotId,
        String connectorId,
        ConnectorCheckType checkType,
        ConnectorHealthStatus healthStatus,
        long latencyMs,
        String errorCode,
        String errorSummary,
        String operatorId,
        String targetEnvironment,
        String confirmedBy,
        String confirmationNote,
        Instant confirmedAt,
        long changeSequence,
        Instant checkedAt
) {

    public ConnectorHealthSnapshot {
        snapshotId = requireText(snapshotId, "snapshotId");
        connectorId = requireText(connectorId, "connectorId");
        Objects.requireNonNull(checkType, "checkType must not be null");
        Objects.requireNonNull(healthStatus, "healthStatus must not be null");
        if (latencyMs < 0) {
            throw new IllegalArgumentException("latencyMs must not be negative");
        }
        operatorId = operatorId == null ? null : operatorId.trim();
        targetEnvironment = requireText(targetEnvironment, "targetEnvironment");
        confirmedBy = normalizeNullable(confirmedBy);
        confirmationNote = normalizeNullable(confirmationNote);
        if (changeSequence < 0) {
            throw new IllegalArgumentException("changeSequence must not be negative");
        }
        Objects.requireNonNull(checkedAt, "checkedAt must not be null");
    }

    public static ConnectorHealthSnapshot from(
            String snapshotId,
            String connectorId,
            ConnectorCheckType checkType,
            String operatorId,
            String targetEnvironment,
            long changeSequence,
            ConnectorTestResult testResult,
            Instant checkedAt
    ) {
        return new ConnectorHealthSnapshot(
                snapshotId,
                connectorId,
                checkType,
                testResult.healthStatus(),
                testResult.latencyMs(),
                testResult.errorCode(),
                testResult.errorSummary(),
                operatorId,
                targetEnvironment,
                null,
                null,
                null,
                changeSequence,
                checkedAt
        );
    }

    public boolean healthy() {
        return healthStatus == ConnectorHealthStatus.HEALTHY;
    }

    public ConnectorHealthSnapshotView toView() {
        return new ConnectorHealthSnapshotView(
                snapshotId,
                connectorId,
                checkType,
                healthStatus,
                latencyMs,
                errorCode,
                errorSummary,
                operatorId,
                targetEnvironment,
                confirmedBy,
                confirmationNote,
                confirmedAt,
                changeSequence,
                checkedAt
        );
    }

    public ConnectorHealthSnapshot confirmAbnormal(String operatorId, String confirmationNote, Instant confirmedAt) {
        if (healthy()) {
            throw new IllegalStateException("Healthy snapshot does not require abnormal confirmation");
        }
        return new ConnectorHealthSnapshot(
                snapshotId,
                connectorId,
                checkType,
                healthStatus,
                latencyMs,
                errorCode,
                errorSummary,
                this.operatorId,
                targetEnvironment,
                requireText(operatorId, "confirmedBy"),
                confirmationNote,
                Objects.requireNonNull(confirmedAt, "confirmedAt must not be null"),
                changeSequence,
                checkedAt
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
