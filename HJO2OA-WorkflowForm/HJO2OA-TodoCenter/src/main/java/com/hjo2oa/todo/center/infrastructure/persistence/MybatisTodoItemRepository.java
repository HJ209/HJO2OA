package com.hjo2oa.todo.center.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hjo2oa.todo.center.domain.TodoItem;
import com.hjo2oa.todo.center.domain.TodoItemRepository;
import com.hjo2oa.todo.center.domain.TodoItemStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisTodoItemRepository implements TodoItemRepository {

    private final TodoItemMapper mapper;

    public MybatisTodoItemRepository(TodoItemMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<TodoItem> findByTaskId(String taskId) {
        return Optional.ofNullable(mapper.selectOne(new QueryWrapper<TodoItemEntity>()
                .eq("task_id", taskId))).map(this::toDomain);
    }

    @Override
    public Optional<TodoItem> findByTodoId(String todoId) {
        return Optional.ofNullable(mapper.selectById(todoId)).map(this::toDomain);
    }

    @Override
    public TodoItem save(TodoItem todoItem) {
        TodoItemEntity existing = mapper.selectById(todoItem.todoId());
        TodoItemEntity entity = toEntity(todoItem, existing);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findByTodoId(todoItem.todoId()).orElseThrow();
    }

    @Override
    public List<TodoItem> findByAssigneeIdAndStatus(String assigneeId, TodoItemStatus status) {
        return mapper.selectList(new QueryWrapper<TodoItemEntity>()
                        .eq("assignee_id", assigneeId)
                        .eq("status", status.name())
                        .orderByDesc("updated_at"))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByAssigneeIdAndStatus(String assigneeId, TodoItemStatus status) {
        return mapper.selectCount(new QueryWrapper<TodoItemEntity>()
                .eq("assignee_id", assigneeId)
                .eq("status", status.name()));
    }

    private TodoItem toDomain(TodoItemEntity entity) {
        return new TodoItem(
                entity.getTodoId(),
                entity.getTaskId(),
                entity.getInstanceId(),
                entity.getAssigneeId(),
                entity.getType(),
                entity.getCategory(),
                entity.getTitle(),
                entity.getUrgency(),
                TodoItemStatus.valueOf(entity.getStatus()),
                entity.getDueTime(),
                entity.getOverdueAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCompletedAt(),
                entity.getCancellationReason()
        );
    }

    private TodoItemEntity toEntity(TodoItem todoItem, TodoItemEntity existing) {
        TodoItemEntity entity = existing == null ? new TodoItemEntity() : existing;
        entity.setTodoId(todoItem.todoId());
        entity.setTaskId(todoItem.taskId());
        entity.setInstanceId(todoItem.instanceId());
        entity.setAssigneeId(todoItem.assigneeId());
        entity.setType(todoItem.type());
        entity.setCategory(todoItem.category());
        entity.setTitle(todoItem.title());
        entity.setUrgency(todoItem.urgency());
        entity.setStatus(todoItem.status().name());
        entity.setDueTime(todoItem.dueTime());
        entity.setOverdueAt(todoItem.overdueAt());
        entity.setCreatedAt(todoItem.createdAt());
        entity.setUpdatedAt(todoItem.updatedAt());
        entity.setCompletedAt(todoItem.completedAt());
        entity.setCancellationReason(todoItem.cancellationReason());
        return entity;
    }
}
