package com.hjo2oa.infra.scheduler.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SchedulerTaskSucceededEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String jobCode,
        UUID executionId,
        Long durationMs,
        Integer attemptNo
) implements DomainEvent {

    public static final String EVENT_TYPE = "infra.scheduler.task-succeeded";

    public SchedulerTaskSucceededEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (tenantId != null && tenantId.isBlank()) {
            tenantId = null;
        }
        jobCode = requireText(jobCode, "jobCode");
        Objects.requireNonNull(executionId, "executionId must not be null");
        Objects.requireNonNull(attemptNo, "attemptNo must not be null");
    }

    public static SchedulerTaskSucceededEvent from(
            ScheduledJob scheduledJob,
            JobExecutionRecord executionRecord,
            Instant occurredAt
    ) {
        return new SchedulerTaskSucceededEvent(
                UUID.randomUUID(),
                occurredAt,
                scheduledJob.tenantId() == null ? null : scheduledJob.tenantId().toString(),
                scheduledJob.jobCode(),
                executionRecord.id(),
                executionRecord.durationMs(),
                executionRecord.attemptNo()
        );
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
