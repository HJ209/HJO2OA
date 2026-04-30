package com.hjo2oa.todo.center.domain;

import java.time.Instant;

public record TodoItemSummary(
        String todoId,
        String taskId,
        String instanceId,
        String tenantId,
        String title,
        String category,
        String urgency,
        TodoItemStatus status,
        String assigneeId,
        Instant dueTime,
        Instant overdueAt,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {

    public static TodoItemSummary from(TodoItem todoItem) {
        return new TodoItemSummary(
                todoItem.todoId(),
                todoItem.taskId(),
                todoItem.instanceId(),
                todoItem.tenantId(),
                todoItem.title(),
                todoItem.category(),
                todoItem.urgency(),
                todoItem.status(),
                todoItem.assigneeId(),
                todoItem.dueTime(),
                todoItem.overdueAt(),
                todoItem.createdAt(),
                todoItem.updatedAt(),
                todoItem.completedAt()
        );
    }

    public boolean overdue() {
        return overdueAt != null;
    }
}
