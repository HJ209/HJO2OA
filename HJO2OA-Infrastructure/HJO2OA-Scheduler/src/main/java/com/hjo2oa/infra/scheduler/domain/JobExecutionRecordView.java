package com.hjo2oa.infra.scheduler.domain;

import java.time.Instant;
import java.util.UUID;

public record JobExecutionRecordView(
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
}
