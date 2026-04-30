package com.hjo2oa.infra.scheduler.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record JobExecutionRecord(
        UUID id,
        UUID scheduledJobId,
        UUID parentExecutionId,
        TriggerSource triggerSource,
        ExecutionStatus executionStatus,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs,
        Integer attemptNo,
        Integer maxAttempts,
        String errorCode,
        String errorMessage,
        String errorStack,
        String executionLog,
        String triggerContext,
        String idempotencyKey,
        Instant nextRetryAt
) {

    public JobExecutionRecord {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(scheduledJobId, "scheduledJobId must not be null");
        Objects.requireNonNull(triggerSource, "triggerSource must not be null");
        Objects.requireNonNull(executionStatus, "executionStatus must not be null");
        Objects.requireNonNull(startedAt, "startedAt must not be null");
        if (durationMs != null && durationMs < 0) {
            throw new IllegalArgumentException("durationMs must not be negative");
        }
        if (attemptNo == null || attemptNo <= 0) {
            throw new IllegalArgumentException("attemptNo must be greater than zero");
        }
        if (maxAttempts == null || maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be greater than zero");
        }
        if (attemptNo > maxAttempts) {
            throw new IllegalArgumentException("attemptNo must not be greater than maxAttempts");
        }
        errorCode = normalizeNullableText(errorCode);
        errorMessage = normalizeNullableText(errorMessage);
        errorStack = normalizeNullableText(errorStack);
        executionLog = normalizeNullableText(executionLog);
        triggerContext = normalizeNullableText(triggerContext);
        idempotencyKey = normalizeNullableText(idempotencyKey);

        if (executionStatus == ExecutionStatus.RUNNING && finishedAt != null) {
            throw new IllegalArgumentException("finishedAt must be null while execution is RUNNING");
        }
        if (executionStatus != ExecutionStatus.RUNNING && finishedAt == null) {
            throw new IllegalArgumentException("finishedAt must not be null when execution is completed");
        }
        if (executionStatus == ExecutionStatus.RUNNING && durationMs != null) {
            throw new IllegalArgumentException("durationMs must be null while execution is RUNNING");
        }
        if (executionStatus == ExecutionStatus.RETRYING && nextRetryAt == null) {
            throw new IllegalArgumentException("nextRetryAt must not be null while execution is RETRYING");
        }
    }

    public static JobExecutionRecord start(UUID scheduledJobId, TriggerSource triggerSource, Instant startedAt) {
        return start(scheduledJobId, triggerSource, startedAt, null, 1, 1, null, null);
    }

    public static JobExecutionRecord start(
            UUID scheduledJobId,
            TriggerSource triggerSource,
            Instant startedAt,
            UUID parentExecutionId,
            int attemptNo,
            int maxAttempts,
            String triggerContext,
            String idempotencyKey
    ) {
        return new JobExecutionRecord(
                UUID.randomUUID(),
                scheduledJobId,
                parentExecutionId,
                triggerSource,
                ExecutionStatus.RUNNING,
                startedAt,
                null,
                null,
                attemptNo,
                maxAttempts,
                null,
                null,
                null,
                null,
                triggerContext,
                idempotencyKey,
                null
        );
    }

    public JobExecutionRecord markSuccess(String executionLog, Instant finishedAt) {
        ensureRunning();
        return new JobExecutionRecord(
                id,
                scheduledJobId,
                parentExecutionId,
                triggerSource,
                ExecutionStatus.SUCCESS,
                startedAt,
                finishedAt,
                durationBetween(startedAt, finishedAt),
                attemptNo,
                maxAttempts,
                null,
                null,
                null,
                executionLog,
                triggerContext,
                idempotencyKey,
                null
        );
    }

    public JobExecutionRecord markFailure(String errorCode, String errorMessage, Instant finishedAt) {
        return markFailure(errorCode, errorMessage, null, finishedAt);
    }

    public JobExecutionRecord markFailure(
            String errorCode,
            String errorMessage,
            String errorStack,
            Instant finishedAt
    ) {
        ensureRunning();
        return new JobExecutionRecord(
                id,
                scheduledJobId,
                parentExecutionId,
                triggerSource,
                ExecutionStatus.FAILED,
                startedAt,
                finishedAt,
                durationBetween(startedAt, finishedAt),
                attemptNo,
                maxAttempts,
                requireText(errorCode, "errorCode"),
                requireText(errorMessage, "errorMessage"),
                errorStack,
                null,
                triggerContext,
                idempotencyKey,
                null
        );
    }

    public JobExecutionRecord markTimeout(
            String errorCode,
            String errorMessage,
            String errorStack,
            Instant finishedAt
    ) {
        ensureRunning();
        return new JobExecutionRecord(
                id,
                scheduledJobId,
                parentExecutionId,
                triggerSource,
                ExecutionStatus.TIMEOUT,
                startedAt,
                finishedAt,
                durationBetween(startedAt, finishedAt),
                attemptNo,
                maxAttempts,
                requireText(errorCode, "errorCode"),
                requireText(errorMessage, "errorMessage"),
                errorStack,
                null,
                triggerContext,
                idempotencyKey,
                null
        );
    }

    public JobExecutionRecord markRetrying(
            String errorCode,
            String errorMessage,
            String errorStack,
            String executionLog,
            Instant nextRetryAt,
            Instant finishedAt
    ) {
        ensureRunning();
        return new JobExecutionRecord(
                id,
                scheduledJobId,
                parentExecutionId,
                triggerSource,
                ExecutionStatus.RETRYING,
                startedAt,
                finishedAt,
                durationBetween(startedAt, finishedAt),
                attemptNo,
                maxAttempts,
                requireText(errorCode, "errorCode"),
                requireText(errorMessage, "errorMessage"),
                errorStack,
                executionLog,
                triggerContext,
                idempotencyKey,
                Objects.requireNonNull(nextRetryAt, "nextRetryAt must not be null")
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
                parentExecutionId,
                triggerSource,
                ExecutionStatus.CANCELLED,
                startedAt,
                finishedAt,
                durationBetween(startedAt, finishedAt),
                attemptNo,
                maxAttempts,
                requireText(errorCode, "errorCode"),
                requireText(errorMessage, "errorMessage"),
                null,
                executionLog,
                triggerContext,
                idempotencyKey,
                null
        );
    }

    public JobExecutionRecordView toView() {
        return new JobExecutionRecordView(
                id,
                scheduledJobId,
                parentExecutionId,
                triggerSource,
                executionStatus,
                startedAt,
                finishedAt,
                durationMs,
                attemptNo,
                maxAttempts,
                errorCode,
                errorMessage,
                errorStack,
                executionLog,
                triggerContext,
                idempotencyKey,
                nextRetryAt
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

    private static long durationBetween(Instant startedAt, Instant finishedAt) {
        return Duration.between(startedAt, finishedAt).toMillis();
    }
}
