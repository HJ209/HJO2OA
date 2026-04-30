package com.hjo2oa.todo.center.domain;

import java.time.Instant;
import java.util.Objects;

public record TodoItem(
        String todoId,
        String taskId,
        String instanceId,
        String tenantId,
        String assigneeId,
        String type,
        String category,
        String title,
        String urgency,
        TodoItemStatus status,
        Instant dueTime,
        Instant overdueAt,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        String cancellationReason
) {

    public TodoItem {
        todoId = requireText(todoId, "todoId");
        taskId = requireText(taskId, "taskId");
        instanceId = requireText(instanceId, "instanceId");
        tenantId = requireText(tenantId, "tenantId");
        assigneeId = requireText(assigneeId, "assigneeId");
        type = requireText(type, "type");
        category = requireText(category, "category");
        title = requireText(title, "title");
        urgency = requireText(urgency, "urgency");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        cancellationReason = normalize(cancellationReason);
    }

    public TodoItem(
            String todoId,
            String taskId,
            String instanceId,
            String assigneeId,
            String type,
            String category,
            String title,
            String urgency,
            TodoItemStatus status,
            Instant dueTime,
            Instant overdueAt,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt,
            String cancellationReason
    ) {
        this(
                todoId,
                taskId,
                instanceId,
                "tenant-1",
                assigneeId,
                type,
                category,
                title,
                urgency,
                status,
                dueTime,
                overdueAt,
                createdAt,
                updatedAt,
                completedAt,
                cancellationReason
        );
    }

    public static TodoItem create(String todoId, ProcessTaskCreatedEvent event) {
        return new TodoItem(
                todoId,
                event.taskId(),
                event.instanceId(),
                event.tenantId(),
                event.assigneeId(),
                event.type(),
                event.category(),
                event.title(),
                event.urgency(),
                TodoItemStatus.PENDING,
                event.dueTime(),
                null,
                event.occurredAt(),
                event.occurredAt(),
                null,
                null
        );
    }

    public TodoItem refreshFrom(ProcessTaskCreatedEvent event) {
        return new TodoItem(
                todoId,
                taskId,
                event.instanceId(),
                event.tenantId(),
                event.assigneeId(),
                event.type(),
                event.category(),
                event.title(),
                event.urgency(),
                status,
                event.dueTime(),
                overdueAt,
                createdAt,
                event.occurredAt(),
                completedAt,
                cancellationReason
        );
    }

    public TodoItem complete(Instant completedTime) {
        return new TodoItem(
                todoId,
                taskId,
                instanceId,
                tenantId,
                assigneeId,
                type,
                category,
                title,
                urgency,
                TodoItemStatus.COMPLETED,
                dueTime,
                overdueAt,
                createdAt,
                Objects.requireNonNull(completedTime, "completedTime must not be null"),
                completedTime,
                cancellationReason
        );
    }

    public TodoItem markOverdue(Instant overdueTime) {
        return new TodoItem(
                todoId,
                taskId,
                instanceId,
                tenantId,
                assigneeId,
                type,
                category,
                title,
                urgency,
                status,
                dueTime,
                Objects.requireNonNull(overdueTime, "overdueTime must not be null"),
                createdAt,
                overdueTime,
                completedAt,
                cancellationReason
        );
    }

    public TodoItem cancel(String reason, Instant cancelledAt) {
        return new TodoItem(
                todoId,
                taskId,
                instanceId,
                tenantId,
                assigneeId,
                type,
                category,
                title,
                urgency,
                TodoItemStatus.CANCELLED,
                dueTime,
                overdueAt,
                createdAt,
                Objects.requireNonNull(cancelledAt, "cancelledAt must not be null"),
                completedAt,
                reason
        );
    }

    public TodoItem transferTo(String newAssigneeId, Instant transferredAt) {
        return new TodoItem(
                todoId,
                taskId,
                instanceId,
                tenantId,
                requireText(newAssigneeId, "newAssigneeId"),
                type,
                category,
                title,
                urgency,
                status,
                dueTime,
                overdueAt,
                createdAt,
                Objects.requireNonNull(transferredAt, "transferredAt must not be null"),
                completedAt,
                cancellationReason
        );
    }

    public boolean isOverdue() {
        return overdueAt != null;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
