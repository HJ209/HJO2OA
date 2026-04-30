package com.hjo2oa.infra.scheduler.application;

import com.hjo2oa.infra.scheduler.domain.TriggerSource;
import java.util.UUID;

public record SchedulerJobExecutionContext(
        UUID executionId,
        UUID jobId,
        String jobCode,
        UUID tenantId,
        TriggerSource triggerSource,
        int attemptNo,
        int maxAttempts,
        String requestId,
        String idempotencyKey,
        String language,
        String timezone,
        String triggerContext
) {
}
