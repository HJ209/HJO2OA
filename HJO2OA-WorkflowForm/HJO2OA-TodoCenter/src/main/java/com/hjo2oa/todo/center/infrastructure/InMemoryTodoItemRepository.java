package com.hjo2oa.todo.center.infrastructure;

import com.hjo2oa.todo.center.domain.TodoItem;
import com.hjo2oa.todo.center.domain.TodoItemRepository;
import com.hjo2oa.todo.center.domain.TodoItemStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTodoItemRepository implements TodoItemRepository {

    private final Map<String, TodoItem> itemsByTaskId = new ConcurrentHashMap<>();

    @Override
    public Optional<TodoItem> findByTaskId(String taskId) {
        return Optional.ofNullable(itemsByTaskId.get(taskId));
    }

    @Override
    public Optional<TodoItem> findByTodoId(String todoId) {
        return itemsByTaskId.values().stream()
                .filter(todoItem -> todoItem.todoId().equals(todoId))
                .findFirst();
    }

    @Override
    public TodoItem save(TodoItem todoItem) {
        itemsByTaskId.put(todoItem.taskId(), todoItem);
        return todoItem;
    }

    @Override
    public List<TodoItem> findByAssigneeIdAndStatus(String assigneeId, TodoItemStatus status) {
        return itemsByTaskId.values().stream()
                .filter(todoItem -> todoItem.assigneeId().equals(assigneeId))
                .filter(todoItem -> todoItem.status() == status)
                .toList();
    }

    @Override
    public long countByAssigneeIdAndStatus(String assigneeId, TodoItemStatus status) {
        return findByAssigneeIdAndStatus(assigneeId, status).size();
    }
}
