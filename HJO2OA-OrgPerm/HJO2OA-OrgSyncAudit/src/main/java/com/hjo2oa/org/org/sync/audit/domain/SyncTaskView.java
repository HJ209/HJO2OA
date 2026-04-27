package com.hjo2oa.org.org.sync.audit.domain;

import java.time.Instant;
import java.util.UUID;

public record SyncTaskView(
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
}
