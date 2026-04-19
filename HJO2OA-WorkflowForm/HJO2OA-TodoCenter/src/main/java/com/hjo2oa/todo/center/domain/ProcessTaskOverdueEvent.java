package com.hjo2oa.todo.center.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProcessTaskOverdueEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String taskId,
        String instanceId,
        Instant dueTime,
        Duration overdueDuration
) implements DomainEvent {

    public static final String EVENT_TYPE = "process.task.overdue";

    public ProcessTaskOverdueEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        taskId = requireText(taskId, "taskId");
        instanceId = requireText(instanceId, "instanceId");
        Objects.requireNonNull(dueTime, "dueTime must not be null");
        Objects.requireNonNull(overdueDuration, "overdueDuration must not be null");
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
