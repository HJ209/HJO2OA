package com.hjo2oa.infra.scheduler.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record JobExecutionRecord(
        UUID id,
        UUID scheduledJobId,
        TriggerSource triggerSource,
        ExecutionStatus executionStatus,
        Instant startedAt,
        Instant finishedAt,
        String errorCode,
        String errorMessage,
        String executionLog
) {

    public JobExecutionRecord {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(scheduledJobId, "scheduledJobId must not be null");
        Objects.requireNonNull(triggerSource, "triggerSource must not be null");
        Objects.requireNonNull(executionStatus, "executionStatus must not be null");
        Objects.requireNonNull(startedAt, "startedAt must not be null");
        errorCode = normalizeNullableText(errorCode);
        errorMessage = normalizeNullableText(errorMessage);
        executionLog = normalizeNullableText(executionLog);

        if (executionStatus == ExecutionStatus.RUNNING && finishedAt != null) {
            throw new IllegalArgumentException("finishedAt must be null while execution is RUNNING");
        }
        if (executionStatus != ExecutionStatus.RUNNING && finishedAt == null) {
            throw new IllegalArgumentException("finishedAt must not be null when execution is completed");
        }
    }

    public static JobExecutionRecord start(UUID scheduledJobId, TriggerSource triggerSource, Instant startedAt) {
        return new JobExecutionRecord(
                UUID.randomUUID(),
                scheduledJobId,
                triggerSource,
                ExecutionStatus.RUNNING,
                startedAt,
                null,
                null,
                null,
                null
        );
    }

    public JobExecutionRecord markSuccess(String executionLog, Instant finishedAt) {
        ensureRunning();
        return new JobExecutionRecord(
                id,
                scheduledJobId,
                triggerSource,
                ExecutionStatus.SUCCESS,
                startedAt,
                finishedAt,
                null,
                null,
                executionLog
        );
    }

    public JobExecutionRecord markFailure(String errorCode, String errorMessage, Instant finishedAt) {
        ensureRunning();
        return new JobExecutionRecord(
                id,
                scheduledJobId,
                triggerSource,
                ExecutionStatus.FAILED,
                startedAt,
                finishedAt,
                requireText(errorCode, "errorCode"),
                requireText(errorMessage, "errorMessage"),
                null
        );
    }

    public JobExecutionRecord markCancelled(
            String errorCode,
            String errorMessage,
            String executionLog,
            Instant finishedAt
    ) {
        ensureRunning();
        return new JobExecutionRecord(
                id,
                scheduledJobId,
                triggerSource,
                ExecutionStatus.CANCELLED,
                startedAt,
                finishedAt,
                requireText(errorCode, "errorCode"),
                requireText(errorMessage, "errorMessage"),
                executionLog
        );
    }

    public JobExecutionRecordView toView() {
        return new JobExecutionRecordView(
                id,
                scheduledJobId,
                triggerSource,
                executionStatus,
                startedAt,
                finishedAt,
                errorCode,
                errorMessage,
                executionLog
        );
    }

    private void ensureRunning() {
        if (executionStatus != ExecutionStatus.RUNNING) {
            throw new IllegalStateException("Only running execution can be completed");
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
