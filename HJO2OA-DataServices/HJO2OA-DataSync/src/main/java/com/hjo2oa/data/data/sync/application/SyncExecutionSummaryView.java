package com.hjo2oa.data.data.sync.application;

import com.hjo2oa.data.data.sync.domain.ExecutionStatus;
import com.hjo2oa.data.data.sync.domain.ExecutionTriggerType;
import com.hjo2oa.data.data.sync.domain.ReconciliationStatus;
import com.hjo2oa.data.data.sync.domain.SyncDiffSummary;
import com.hjo2oa.data.data.sync.domain.SyncResultSummary;
import java.time.Instant;
import java.util.UUID;

public record SyncExecutionSummaryView(
        UUID executionId,
        UUID taskId,
        UUID parentExecutionId,
        String taskCode,
        String executionBatchNo,
        ExecutionTriggerType triggerType,
        ExecutionStatus executionStatus,
        String checkpointValue,
        boolean retryable,
        int retryCount,
        String failureCode,
        String failureMessage,
        SyncResultSummary resultSummary,
        SyncDiffSummary diffSummary,
        ReconciliationStatus reconciliationStatus,
        Instant startedAt,
        Instant finishedAt
) {
}
