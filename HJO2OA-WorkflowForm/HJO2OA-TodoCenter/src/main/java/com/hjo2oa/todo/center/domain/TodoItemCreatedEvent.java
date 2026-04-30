package com.hjo2oa.todo.center.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TodoItemCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String todoId,
        String taskId,
        String instanceId,
        String assigneeId,
        String type,
        String category,
        String title,
        String urgency,
        Instant dueTime
) implements DomainEvent {

    public static final String EVENT_TYPE = "todo.item.created";

    public TodoItemCreatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        todoId = requireText(todoId, "todoId");
        taskId = requireText(taskId, "taskId");
        instanceId = requireText(instanceId, "instanceId");
        assigneeId = requireText(assigneeId, "assigneeId");
        type = requireText(type, "type");
        category = requireText(category, "category");
        title = requireText(title, "title");
        urgency = requireText(urgency, "urgency");
    }

    public static TodoItemCreatedEvent from(TodoItem todoItem, Instant occurredAt, String tenantId) {
        return new TodoItemCreatedEvent(
                UUID.randomUUID(),
                occurredAt,
                todoItem.tenantId(),
                todoItem.todoId(),
                todoItem.taskId(),
                todoItem.instanceId(),
                todoItem.assigneeId(),
                todoItem.type(),
                todoItem.category(),
                todoItem.title(),
                todoItem.urgency(),
                todoItem.dueTime()
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
