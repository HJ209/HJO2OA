package com.hjo2oa.todo.center.application;

import com.hjo2oa.todo.center.domain.CopiedTodoReadStatus;
import com.hjo2oa.todo.center.domain.CopiedTodoRepository;
import com.hjo2oa.todo.center.domain.CopiedTodoSummary;
import com.hjo2oa.todo.center.domain.TodoCounts;
import com.hjo2oa.todo.center.domain.TodoIdentityContext;
import com.hjo2oa.todo.center.domain.TodoIdentityContextProvider;
import com.hjo2oa.todo.center.domain.TodoItemRepository;
import com.hjo2oa.todo.center.domain.TodoItemStatus;
import com.hjo2oa.todo.center.domain.TodoItemSummary;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TodoQueryApplicationService {

    private static final Comparator<TodoItemSummary> PENDING_ORDER =
            Comparator.comparing(TodoItemSummary::dueTime, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(TodoItemSummary::createdAt, Comparator.reverseOrder());
    private static final Comparator<TodoItemSummary> COMPLETED_ORDER =
            Comparator.comparing(TodoItemSummary::completedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(TodoItemSummary::updatedAt, Comparator.reverseOrder());
    private static final Comparator<TodoItemSummary> OVERDUE_ORDER =
            Comparator.comparing(TodoItemSummary::overdueAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(TodoItemSummary::dueTime, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(TodoItemSummary::updatedAt, Comparator.reverseOrder());
    private static final Comparator<CopiedTodoSummary> COPIED_ORDER =
            Comparator.comparing(CopiedTodoSummary::createdAt, Comparator.reverseOrder())
                    .thenComparing(CopiedTodoSummary::updatedAt, Comparator.reverseOrder());

    private final TodoItemRepository todoItemRepository;
    private final CopiedTodoRepository copiedTodoRepository;
    private final TodoIdentityContextProvider identityContextProvider;

    public TodoQueryApplicationService(
            TodoItemRepository todoItemRepository,
            CopiedTodoRepository copiedTodoRepository,
            TodoIdentityContextProvider identityContextProvider
    ) {
        this.todoItemRepository = todoItemRepository;
        this.copiedTodoRepository = copiedTodoRepository;
        this.identityContextProvider = identityContextProvider;
    }

    public List<TodoItemSummary> pendingTodos() {
        TodoIdentityContext identityContext = identityContextProvider.currentContext();
        return todoItemRepository.findByAssigneeIdAndStatus(identityContext.assignmentId(), TodoItemStatus.PENDING)
                .stream()
                .map(TodoItemSummary::from)
                .sorted(PENDING_ORDER)
                .toList();
    }

    public List<TodoItemSummary> completedTodos() {
        TodoIdentityContext identityContext = identityContextProvider.currentContext();
        return todoItemRepository.findByAssigneeIdAndStatus(identityContext.assignmentId(), TodoItemStatus.COMPLETED)
                .stream()
                .map(TodoItemSummary::from)
                .sorted(COMPLETED_ORDER)
                .toList();
    }

    public List<TodoItemSummary> overdueTodos() {
        TodoIdentityContext identityContext = identityContextProvider.currentContext();
        return todoItemRepository.findByAssigneeIdAndStatus(identityContext.assignmentId(), TodoItemStatus.PENDING)
                .stream()
                .map(TodoItemSummary::from)
                .filter(TodoItemSummary::overdue)
                .sorted(OVERDUE_ORDER)
                .toList();
    }

    public List<CopiedTodoSummary> copiedTodos(CopiedTodoReadStatus readStatus) {
        TodoIdentityContext identityContext = identityContextProvider.currentContext();
        return copiedTodoRepository.findByRecipientAssignmentId(identityContext.assignmentId()).stream()
                .map(CopiedTodoSummary::from)
                .filter(summary -> readStatus == null || summary.readStatus() == readStatus)
                .sorted(COPIED_ORDER)
                .toList();
    }

    public TodoCounts counts() {
        TodoIdentityContext identityContext = identityContextProvider.currentContext();
        long pendingCount = todoItemRepository.countByAssigneeIdAndStatus(identityContext.assignmentId(), TodoItemStatus.PENDING);
        long completedCount = todoItemRepository.countByAssigneeIdAndStatus(identityContext.assignmentId(), TodoItemStatus.COMPLETED);
        long overdueCount = overdueTodos().size();
        long copiedUnreadCount = copiedTodoRepository.countUnreadByRecipientAssignmentId(identityContext.assignmentId());
        return new TodoCounts(
                pendingCount,
                completedCount,
                overdueCount,
                0,
                copiedUnreadCount,
                0,
                0
        );
    }
}
