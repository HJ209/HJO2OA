package com.hjo2oa.todo.center.domain;

import java.time.Instant;
import java.util.Objects;

public record CopiedTodoItem(
        String todoId,
        String taskId,
        String instanceId,
        String recipientAssignmentId,
        String type,
        String category,
        String title,
        String urgency,
        CopiedTodoReadStatus readStatus,
        Instant createdAt,
        Instant updatedAt,
        Instant readAt
) {

    public CopiedTodoItem {
        todoId = requireText(todoId, "todoId");
        taskId = requireText(taskId, "taskId");
        instanceId = requireText(instanceId, "instanceId");
        recipientAssignmentId = requireText(recipientAssignmentId, "recipientAssignmentId");
        type = requireText(type, "type");
        category = requireText(category, "category");
        title = requireText(title, "title");
        urgency = requireText(urgency, "urgency");
        Objects.requireNonNull(readStatus, "readStatus must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static CopiedTodoItem unread(
            String todoId,
            String taskId,
            String instanceId,
            String recipientAssignmentId,
            String type,
            String category,
            String title,
            String urgency,
            Instant createdAt
    ) {
        return new CopiedTodoItem(
                todoId,
                taskId,
                instanceId,
                recipientAssignmentId,
                type,
                category,
                title,
                urgency,
                CopiedTodoReadStatus.UNREAD,
                createdAt,
                createdAt,
                null
        );
    }

    public boolean isVisibleTo(TodoIdentityContext identityContext) {
        Objects.requireNonNull(identityContext, "identityContext must not be null");
        return recipientAssignmentId.equals(identityContext.assignmentId());
    }

    public boolean isUnread() {
        return readStatus == CopiedTodoReadStatus.UNREAD;
    }

    public CopiedTodoItem markRead(Instant readAt) {
        Objects.requireNonNull(readAt, "readAt must not be null");
        if (!isUnread()) {
            return this;
        }
        return new CopiedTodoItem(
                todoId,
                taskId,
                instanceId,
                recipientAssignmentId,
                type,
                category,
                title,
                urgency,
                CopiedTodoReadStatus.READ,
                createdAt,
                readAt,
                readAt
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
