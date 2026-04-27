package com.hjo2oa.wf.action.engine.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProcessTaskTerminatedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String taskId,
        String instanceId,
        String reason
) implements DomainEvent {

    public static final String EVENT_TYPE = "process.task.terminated";

    public ProcessTaskTerminatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        taskId = requireText(taskId, "taskId");
        instanceId = requireText(instanceId, "instanceId");
        reason = requireText(reason, "reason");
    }

    public static ProcessTaskTerminatedEvent from(TaskAction action, Instant occurredAt) {
        return new ProcessTaskTerminatedEvent(
                UUID.randomUUID(),
                occurredAt,
                action.tenantId(),
                action.taskId().toString(),
                action.instanceId().toString(),
                action.opinion() == null ? action.actionCode() : action.opinion()
        );
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
