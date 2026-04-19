package com.hjo2oa.todo.center.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TodoItemOverdueEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String todoId,
        String taskId,
        String instanceId,
        String assigneeId,
        String category,
        String title,
        Instant dueTime,
        Duration overdueDuration
) implements DomainEvent {

    public static final String EVENT_TYPE = "todo.item.overdue";

    public TodoItemOverdueEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        todoId = requireText(todoId, "todoId");
        taskId = requireText(taskId, "taskId");
        instanceId = requireText(instanceId, "instanceId");
        assigneeId = requireText(assigneeId, "assigneeId");
        category = requireText(category, "category");
        title = requireText(title, "title");
        Objects.requireNonNull(dueTime, "dueTime must not be null");
        Objects.requireNonNull(overdueDuration, "overdueDuration must not be null");
    }

    public static TodoItemOverdueEvent from(TodoItem todoItem, Instant occurredAt, String tenantId) {
        Instant dueTime = Objects.requireNonNull(todoItem.dueTime(), "dueTime must not be null");
        Instant overdueAt = Objects.requireNonNull(todoItem.overdueAt(), "overdueAt must not be null");
        return new TodoItemOverdueEvent(
                UUID.randomUUID(),
                occurredAt,
                tenantId,
                todoItem.todoId(),
                todoItem.taskId(),
                todoItem.instanceId(),
                todoItem.assigneeId(),
                todoItem.category(),
                todoItem.title(),
                dueTime,
                Duration.between(dueTime, overdueAt)
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
