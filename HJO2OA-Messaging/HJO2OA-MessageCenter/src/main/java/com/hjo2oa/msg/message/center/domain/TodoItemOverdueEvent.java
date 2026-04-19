package com.hjo2oa.msg.message.center.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TodoItemOverdueEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String todoId,
        String taskId,
        String assigneeId,
        String category,
        String title,
        Instant dueTime
) implements DomainEvent {

    public static final String EVENT_TYPE = "todo.item.overdue";

    public TodoItemOverdueEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        todoId = requireText(todoId, "todoId");
        taskId = requireText(taskId, "taskId");
        assigneeId = requireText(assigneeId, "assigneeId");
        category = requireText(category, "category");
        title = requireText(title, "title");
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
