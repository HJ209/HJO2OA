package com.hjo2oa.todo.center.domain;

import java.util.List;
import java.util.Optional;

public interface CopiedTodoRepository {

    Optional<CopiedTodoItem> findByTodoId(String todoId);

    CopiedTodoItem save(CopiedTodoItem copiedTodoItem);

    List<CopiedTodoItem> findByRecipientAssignmentId(String recipientAssignmentId);

    long countUnreadByRecipientAssignmentId(String recipientAssignmentId);
}
