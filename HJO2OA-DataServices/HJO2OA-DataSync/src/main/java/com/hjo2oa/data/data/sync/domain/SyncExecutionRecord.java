package com.hjo2oa.data.data.sync.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record SyncExecutionRecord(
        UUID executionId,
        UUID syncTaskId,
        UUID tenantId,
        String taskCode,
        UUID parentExecutionId,
        String executionBatchNo,
        ExecutionTriggerType triggerType,
        ExecutionStatus executionStatus,
        String idempotencyKey,
        String checkpointValue,
        int retryCount,
        boolean retryable,
        SyncResultSummary resultSummary,
        SyncDiffSummary diffSummary,
        List<SyncDifferenceItem> differenceItems,
        Map<String, Object> triggerContext,
        String failureCode,
        String failureMessage,
        ReconciliationStatus reconciliationStatus,
        String operatorAccountId,
        String operatorPersonId,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public SyncExecutionRecord {
        executionId = SyncDomainSupport.requireId(executionId, "executionId");
        syncTaskId = SyncDomainSupport.requireId(syncTaskId, "syncTaskId");
        tenantId = SyncDomainSupport.requireId(tenantId, "tenantId");
        taskCode = SyncDomainSupport.requireText(taskCode, "taskCode");
        executionBatchNo = SyncDomainSupport.requireText(executionBatchNo, "executionBatchNo");
        Objects.requireNonNull(triggerType, "triggerType must not be null");
        Objects.requireNonNull(executionStatus, "executionStatus must not be null");
        idempotencyKey = SyncDomainSupport.normalize(idempotencyKey);
        checkpointValue = SyncDomainSupport.normalize(checkpointValue);
        if (retryCount < 0) {
            throw new IllegalArgumentException("retryCount must not be negative");
        }
        resultSummary = resultSummary == null ? SyncResultSummary.empty() : resultSummary;
        diffSummary = diffSummary == null ? SyncDiffSummary.clean() : diffSummary;
        differenceItems = differenceItems == null ? List.of() : List.copyOf(differenceItems);
        triggerContext = triggerContext == null ? Map.of() : Map.copyOf(triggerContext);
        failureCode = SyncDomainSupport.normalize(failureCode);
        failureMessage = SyncDomainSupport.normalize(failureMessage);
        reconciliationStatus = reconciliationStatus == null
                ? ReconciliationStatus.NOT_CHECKED
                : reconciliationStatus;
        operatorAccountId = SyncDomainSupport.normalize(operatorAccountId);
        operatorPersonId = SyncDomainSupport.normalize(operatorPersonId);
        Objects.requireNonNull(startedAt, "startedAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static SyncExecutionRecord start(
            SyncExchangeTask task,
            ExecutionTriggerType triggerType,
            UUID parentExecutionId,
            String idempotencyKey,
            int retryCount,
            String operatorAccountId,
            String operatorPersonId,
            Map<String, Object> triggerContext,
            Instant now
    ) {
        Objects.requireNonNull(task, "task must not be null");
        Objects.requireNonNull(triggerType, "triggerType must not be null");
        Instant startedAt = Objects.requireNonNull(now, "now must not be null");
        return new SyncExecutionRecord(
                UUID.randomUUID(),
                task.taskId(),
                task.tenantId(),
                task.code(),
                parentExecutionId,
                task.code() + "-" + startedAt.toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8),
                triggerType,
                triggerType == ExecutionTriggerType.COMPENSATION
                        ? ExecutionStatus.COMPENSATING
                        : ExecutionStatus.RUNNING,
                idempotencyKey,
                null,
                retryCount,
                false,
                SyncResultSummary.empty(),
                SyncDiffSummary.clean(),
                List.of(),
                triggerContext,
                null,
                null,
                ReconciliationStatus.NOT_CHECKED,
                operatorAccountId,
                operatorPersonId,
                startedAt,
                null,
                startedAt,
                startedAt
        );
    }

    public SyncExecutionRecord succeed(
            String resolvedCheckpoint,
            SyncResultSummary resolvedResultSummary,
            List<SyncDifferenceItem> resolvedDifferences,
            ReconciliationStatus resolvedReconciliationStatus,
            Instant now
    ) {
        Instant finished = Objects.requireNonNull(now, "now must not be null");
        List<SyncDifferenceItem> differences = resolvedDifferences == null ? List.of() : List.copyOf(resolvedDifferences);
        return new SyncExecutionRecord(
                executionId,
                syncTaskId,
                tenantId,
                taskCode,
                parentExecutionId,
                executionBatchNo,
                triggerType,
                ExecutionStatus.SUCCESS,
                idempotencyKey,
                resolvedCheckpoint,
                retryCount,
                false,
                resolvedResultSummary,
                SyncDiffSummary.from(differences),
                differences,
                triggerContext,
                null,
                null,
                resolvedReconciliationStatus,
                operatorAccountId,
                operatorPersonId,
                startedAt,
                finished,
                createdAt,
                finished
        );
    }

    public SyncExecutionRecord fail(
            String resolvedCheckpoint,
            SyncResultSummary resolvedResultSummary,
            List<SyncDifferenceItem> resolvedDifferences,
            String resolvedFailureCode,
            String resolvedFailureMessage,
            boolean resolvedRetryable,
            ReconciliationStatus resolvedReconciliationStatus,
            Instant now
    ) {
        Instant finished = Objects.requireNonNull(now, "now must not be null");
        List<SyncDifferenceItem> differences = resolvedDifferences == null ? List.of() : List.copyOf(resolvedDifferences);
        return new SyncExecutionRecord(
                executionId,
                syncTaskId,
                tenantId,
                taskCode,
                parentExecutionId,
                executionBatchNo,
                triggerType,
                ExecutionStatus.FAILED,
                idempotencyKey,
                resolvedCheckpoint,
                retryCount,
                resolvedRetryable,
                resolvedResultSummary,
                SyncDiffSummary.from(differences),
                differences,
                triggerContext,
                SyncDomainSupport.requireText(resolvedFailureCode, "resolvedFailureCode"),
                resolvedFailureMessage,
                resolvedReconciliationStatus,
                operatorAccountId,
                operatorPersonId,
                startedAt,
                finished,
                createdAt,
                finished
        );
    }

    public boolean failed() {
        return executionStatus == ExecutionStatus.FAILED;
    }

    public boolean canRetry() {
        return failed() && retryable;
    }
}
