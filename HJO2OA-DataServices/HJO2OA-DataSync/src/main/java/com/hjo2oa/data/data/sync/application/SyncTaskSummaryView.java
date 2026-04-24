package com.hjo2oa.data.data.sync.application;

import com.hjo2oa.data.data.sync.domain.CheckpointMode;
import com.hjo2oa.data.data.sync.domain.ConnectorDependencyStatus;
import com.hjo2oa.data.data.sync.domain.SyncMode;
import com.hjo2oa.data.data.sync.domain.SyncTaskStatus;
import com.hjo2oa.data.data.sync.domain.SyncTaskType;
import java.time.Instant;
import java.util.UUID;

public record SyncTaskSummaryView(
        UUID taskId,
        UUID tenantId,
        String code,
        String name,
        String description,
        SyncTaskType taskType,
        SyncMode syncMode,
        UUID sourceConnectorId,
        UUID targetConnectorId,
        ConnectorDependencyStatus dependencyStatus,
        CheckpointMode checkpointMode,
        SyncTaskStatus status,
        String latestCheckpoint,
        SyncExecutionSummaryView latestExecution,
        Instant createdAt,
        Instant updatedAt
) {
}
