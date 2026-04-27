package com.hjo2oa.org.org.sync.audit.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SyncTask(
        UUID id,
        UUID tenantId,
        UUID sourceId,
        SyncTaskType taskType,
        SyncTaskStatus status,
        UUID retryOfTaskId,
        String triggerSource,
        UUID operatorId,
        Instant startedAt,
        Instant finishedAt,
        int successCount,
        int failureCount,
        int diffCount,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {

    public SyncTask {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        Objects.requireNonNull(taskType, "taskType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        triggerSource = SyncSourceConfig.requireText(triggerSource, "triggerSource");
        if (successCount < 0 || failureCount < 0 || diffCount < 0) {
            throw new IllegalArgumentException("task counters must not be negative");
        }
        failureReason = SyncSourceConfig.normalizeNullable(failureReason);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static SyncTask create(
            UUID id,
            UUID tenantId,
            UUID sourceId,
            SyncTaskType taskType,
            UUID retryOfTaskId,
            String triggerSource,
            UUID operatorId,
            Instant now
    ) {
        return new SyncTask(
                id,
                tenantId,
                sourceId,
                taskType,
                SyncTaskStatus.CREATED,
                retryOfTaskId,
                triggerSource,
                operatorId,
                null,
                null,
                0,
                0,
                0,
                null,
                now,
                now
        );
    }

    public SyncTask start(Instant now) {
        return new SyncTask(
                id,
                tenantId,
                sourceId,
                taskType,
                SyncTaskStatus.RUNNING,
                retryOfTaskId,
                triggerSource,
                operatorId,
                now,
                null,
                successCount,
                failureCount,
                diffCount,
                null,
                createdAt,
                now
        );
    }

    public SyncTask complete(int successCount, int failureCount, int diffCount, Instant now) {
        return finish(SyncTaskStatus.COMPLETED, successCount, failureCount, diffCount, null, now);
    }

    public SyncTask fail(int successCount, int failureCount, int diffCount, String reason, Instant now) {
        return finish(SyncTaskStatus.FAILED, successCount, failureCount, diffCount, reason, now);
    }

    public SyncTaskView toView() {
        return new SyncTaskView(
                id,
                tenantId,
                sourceId,
                taskType,
                status,
                retryOfTaskId,
                triggerSource,
                operatorId,
                startedAt,
                finishedAt,
                successCount,
                failureCount,
                diffCount,
                failureReason,
                createdAt,
                updatedAt
        );
    }

    private SyncTask finish(
            SyncTaskStatus nextStatus,
            int successCount,
            int failureCount,
            int diffCount,
            String reason,
            Instant now
    ) {
        return new SyncTask(
                id,
                tenantId,
                sourceId,
                taskType,
                nextStatus,
                retryOfTaskId,
                triggerSource,
                operatorId,
                startedAt,
                now,
                successCount,
                failureCount,
                diffCount,
                reason,
                createdAt,
                now
        );
    }
}
