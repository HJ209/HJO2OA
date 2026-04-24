package com.hjo2oa.infra.scheduler.domain;

import java.time.Instant;
import java.util.UUID;

public record JobExecutionRecordView(
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
}
