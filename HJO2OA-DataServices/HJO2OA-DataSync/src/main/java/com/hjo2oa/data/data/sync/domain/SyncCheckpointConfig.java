package com.hjo2oa.data.data.sync.domain;

import java.time.Instant;
import java.util.Objects;

public record SyncCheckpointConfig(
        String checkpointField,
        String idempotencyField,
        boolean allowManualReset,
        String initialValue,
        String manualOverrideValue,
        String lastResetReason,
        String lastResetBy,
        Instant lastResetAt
) {

    public SyncCheckpointConfig {
        checkpointField = normalize(checkpointField);
        idempotencyField = normalize(idempotencyField);
        initialValue = normalize(initialValue);
        manualOverrideValue = normalize(manualOverrideValue);
        lastResetReason = normalize(lastResetReason);
        lastResetBy = normalize(lastResetBy);
        if (lastResetAt != null) {
            Objects.requireNonNull(lastResetReason, "lastResetReason must not be null");
            Objects.requireNonNull(lastResetBy, "lastResetBy must not be null");
        }
    }

    public static SyncCheckpointConfig empty() {
        return new SyncCheckpointConfig(null, null, false, null, null, null, null, null);
    }

    public SyncCheckpointConfig override(String value, String operatorId, String reason, Instant resetAt) {
        if (!allowManualReset) {
            throw new IllegalStateException("manual checkpoint reset is disabled");
        }
        return new SyncCheckpointConfig(
                checkpointField,
                idempotencyField,
                true,
                initialValue,
                requireText(value, "value"),
                requireText(reason, "reason"),
                requireText(operatorId, "operatorId"),
                Objects.requireNonNull(resetAt, "resetAt must not be null")
        );
    }

    public SyncCheckpointConfig clearOverride() {
        if (manualOverrideValue == null) {
            return this;
        }
        return new SyncCheckpointConfig(
                checkpointField,
                idempotencyField,
                allowManualReset,
                initialValue,
                null,
                lastResetReason,
                lastResetBy,
                lastResetAt
        );
    }

    public String resolvedStartCheckpoint() {
        return manualOverrideValue != null ? manualOverrideValue : initialValue;
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
