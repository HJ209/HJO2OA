package com.hjo2oa.todo.center.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TodoItemCancelledEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String todoId,
        String taskId,
        String instanceId,
        String reason
) implements DomainEvent {

    public static final String EVENT_TYPE = "todo.item.cancelled";

    public TodoItemCancelledEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        todoId = requireText(todoId, "todoId");
        taskId = requireText(taskId, "taskId");
        instanceId = requireText(instanceId, "instanceId");
        reason = requireText(reason, "reason");
    }

    public static TodoItemCancelledEvent from(TodoItem todoItem, Instant occurredAt, String tenantId) {
        return new TodoItemCancelledEvent(
                UUID.randomUUID(),
                occurredAt,
                tenantId,
                todoItem.todoId(),
                todoItem.taskId(),
                todoItem.instanceId(),
                todoItem.cancellationReason()
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
