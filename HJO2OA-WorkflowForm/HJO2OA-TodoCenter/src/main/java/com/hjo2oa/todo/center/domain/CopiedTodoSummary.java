package com.hjo2oa.todo.center.domain;

import java.time.Instant;

public record CopiedTodoSummary(
        String todoId,
        String taskId,
        String instanceId,
        String tenantId,
        String type,
        String category,
        String title,
        String urgency,
        CopiedTodoReadStatus readStatus,
        Instant createdAt,
        Instant updatedAt,
        Instant readAt
) {

    public static CopiedTodoSummary from(CopiedTodoItem copiedTodoItem) {
        return new CopiedTodoSummary(
                copiedTodoItem.todoId(),
                copiedTodoItem.taskId(),
                copiedTodoItem.instanceId(),
                copiedTodoItem.tenantId(),
                copiedTodoItem.type(),
                copiedTodoItem.category(),
                copiedTodoItem.title(),
                copiedTodoItem.urgency(),
                copiedTodoItem.readStatus(),
                copiedTodoItem.createdAt(),
                copiedTodoItem.updatedAt(),
                copiedTodoItem.readAt()
        );
    }
}
