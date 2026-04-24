package com.hjo2oa.data.data.sync.domain;

import java.time.Instant;
import java.util.UUID;

public record SyncExecutionFilter(
        UUID taskId,
        String taskCode,
        ExecutionStatus executionStatus,
        ExecutionTriggerType triggerType,
        Instant startedFrom,
        Instant startedTo,
        int page,
        int size
) {

    public SyncExecutionFilter {
        taskCode = SyncDomainSupport.normalize(taskCode);
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
    }
}
