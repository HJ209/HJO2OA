package com.hjo2oa.todo.center.domain;

import java.util.List;
import java.util.Optional;

public interface TodoItemRepository {

    Optional<TodoItem> findByTaskId(String taskId);

    Optional<TodoItem> findByTodoId(String todoId);

    TodoItem save(TodoItem todoItem);

    List<TodoItem> findByAssigneeIdAndStatus(String assigneeId, TodoItemStatus status);

    List<TodoItem> findByTenantIdAndAssigneeIdAndStatus(String tenantId, String assigneeId, TodoItemStatus status);

    long countByAssigneeIdAndStatus(String assigneeId, TodoItemStatus status);

    long countByTenantIdAndAssigneeIdAndStatus(String tenantId, String assigneeId, TodoItemStatus status);
}
