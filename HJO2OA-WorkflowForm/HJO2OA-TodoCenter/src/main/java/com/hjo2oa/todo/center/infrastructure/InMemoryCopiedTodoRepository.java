package com.hjo2oa.todo.center.infrastructure;

import com.hjo2oa.todo.center.domain.CopiedTodoItem;
import com.hjo2oa.todo.center.domain.CopiedTodoRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryCopiedTodoRepository implements CopiedTodoRepository {

    private final Map<String, CopiedTodoItem> copiedTodosById = new ConcurrentHashMap<>();

    @Override
    public Optional<CopiedTodoItem> findByTodoId(String todoId) {
        return Optional.ofNullable(copiedTodosById.get(todoId));
    }

    @Override
    public CopiedTodoItem save(CopiedTodoItem copiedTodoItem) {
        copiedTodosById.put(copiedTodoItem.todoId(), copiedTodoItem);
        return copiedTodoItem;
    }

    @Override
    public List<CopiedTodoItem> findByRecipientAssignmentId(String recipientAssignmentId) {
        return copiedTodosById.values().stream()
                .filter(copiedTodoItem -> copiedTodoItem.recipientAssignmentId().equals(recipientAssignmentId))
                .toList();
    }

    @Override
    public List<CopiedTodoItem> findByTenantIdAndRecipientAssignmentId(String tenantId, String recipientAssignmentId) {
        return copiedTodosById.values().stream()
                .filter(copiedTodoItem -> copiedTodoItem.tenantId().equals(tenantId))
                .filter(copiedTodoItem -> copiedTodoItem.recipientAssignmentId().equals(recipientAssignmentId))
                .toList();
    }

    @Override
    public long countUnreadByRecipientAssignmentId(String recipientAssignmentId) {
        return findByRecipientAssignmentId(recipientAssignmentId).stream()
                .filter(CopiedTodoItem::isUnread)
                .count();
    }

    @Override
    public long countUnreadByTenantIdAndRecipientAssignmentId(String tenantId, String recipientAssignmentId) {
        return findByTenantIdAndRecipientAssignmentId(tenantId, recipientAssignmentId).stream()
                .filter(CopiedTodoItem::isUnread)
                .count();
    }
}
