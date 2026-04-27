package com.hjo2oa.wf.action.engine.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProcessTaskActionExecutedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String taskId,
        String instanceId,
        String actionCode,
        String category,
        String taskStatus
) implements DomainEvent {

    public static final String EVENT_TYPE = "process.task.action-executed";

    public ProcessTaskActionExecutedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        taskId = requireText(taskId, "taskId");
        instanceId = requireText(instanceId, "instanceId");
        actionCode = requireText(actionCode, "actionCode");
        category = requireText(category, "category");
        taskStatus = requireText(taskStatus, "taskStatus");
    }

    public static ProcessTaskActionExecutedEvent from(TaskAction action, TaskStatus taskStatus, Instant occurredAt) {
        return new ProcessTaskActionExecutedEvent(
                UUID.randomUUID(),
                occurredAt,
                action.tenantId(),
                action.taskId().toString(),
                action.instanceId().toString(),
                action.actionCode(),
                action.category().name(),
                taskStatus.name()
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
