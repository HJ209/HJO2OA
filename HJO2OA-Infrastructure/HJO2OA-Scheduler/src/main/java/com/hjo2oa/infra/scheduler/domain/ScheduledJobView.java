package com.hjo2oa.infra.scheduler.domain;

import java.time.Instant;
import java.util.UUID;

public record ScheduledJobView(
        UUID id,
        String jobCode,
        String name,
        TriggerType triggerType,
        String cronExpr,
        String timezoneId,
        ConcurrencyPolicy concurrencyPolicy,
        Integer timeoutSeconds,
        String retryPolicy,
        JobStatus status,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}
