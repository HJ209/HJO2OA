package com.hjo2oa.todo.center.application;

import com.hjo2oa.todo.center.domain.CopiedTodoReadStatus;
import com.hjo2oa.todo.center.domain.CopiedTodoRepository;
import com.hjo2oa.todo.center.domain.CopiedTodoSummary;
import com.hjo2oa.todo.center.domain.ArchiveProcessSummary;
import com.hjo2oa.todo.center.domain.DraftProcessSummary;
import com.hjo2oa.todo.center.domain.InitiatedProcessSummary;
import com.hjo2oa.todo.center.domain.TodoCounts;
import com.hjo2oa.todo.center.domain.TodoIdentityContext;
import com.hjo2oa.todo.center.domain.TodoIdentityContextProvider;
import com.hjo2oa.todo.center.domain.TodoItemRepository;
import com.hjo2oa.todo.center.domain.TodoItemStatus;
import com.hjo2oa.todo.center.domain.TodoItemSummary;
import com.hjo2oa.todo.center.domain.TodoProcessViewRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final TodoProcessViewRepository processViewRepository;
    private final TodoIdentityContextProvider identityContextProvider;

    @Autowired
    public TodoQueryApplicationService(
            TodoItemRepository todoItemRepository,
            CopiedTodoRepository copiedTodoRepository,
            TodoProcessViewRepository processViewRepository,
            TodoIdentityContextProvider identityContextProvider
    ) {
        this.todoItemRepository = todoItemRepository;
        this.copiedTodoRepository = copiedTodoRepository;
        this.processViewRepository = processViewRepository;
        this.identityContextProvider = identityContextProvider;
    }

    public List<TodoItemSummary> pendingTodos() {
        TodoIdentityContext identityContext = identityContextProvider.currentContext();
        return todoItemRepository.findByTenantIdAndAssigneeIdAndStatus(
                        identityContext.tenantId(),
                        identityContext.assignmentId(),
                        TodoItemStatus.PENDING
                )
                .stream()
                .map(TodoItemSummary::from)
                .sorted(PENDING_ORDER)
                .toList();
    }

    public List<TodoItemSummary> completedTodos() {
        TodoIdentityContext identityContext = identityContextProvider.currentContext();
        return todoItemRepository.findByTenantIdAndAssigneeIdAndStatus(
                        identityContext.tenantId(),
                        identityContext.assignmentId(),
                        TodoItemStatus.COMPLETED
                )
                .stream()
                .map(TodoItemSummary::from)
                .sorted(COMPLETED_ORDER)
                .toList();
    }

    public List<TodoItemSummary> overdueTodos() {
        TodoIdentityContext identityContext = identityContextProvider.currentContext();
        return todoItemRepository.findByTenantIdAndAssigneeIdAndStatus(
                        identityContext.tenantId(),
                        identityContext.assignmentId(),
                        TodoItemStatus.PENDING
                )
                .stream()
                .map(TodoItemSummary::from)
                .filter(TodoItemSummary::overdue)
                .sorted(OVERDUE_ORDER)
                .toList();
    }

    public List<CopiedTodoSummary> copiedTodos(CopiedTodoReadStatus readStatus) {
        TodoIdentityContext identityContext = identityContextProvider.currentContext();
        return copiedTodoRepository.findByTenantIdAndRecipientAssignmentId(
                        identityContext.tenantId(),
                        identityContext.assignmentId()
                )
                .stream()
                .map(CopiedTodoSummary::from)
                .filter(summary -> readStatus == null || summary.readStatus() == readStatus)
                .sorted(COPIED_ORDER)
                .toList();
    }

    public List<InitiatedProcessSummary> initiatedProcesses() {
        TodoIdentityContext identityContext = identityContextProvider.currentContext();
        return processViewRepository.findInitiated(identityContext.tenantId(), identityContext.personId());
    }

    public List<DraftProcessSummary> draftProcesses() {
        TodoIdentityContext identityContext = identityContextProvider.currentContext();
        return processViewRepository.findDrafts(identityContext.tenantId(), identityContext.personId());
    }

    public List<ArchiveProcessSummary> archivedProcesses() {
        TodoIdentityContext identityContext = identityContextProvider.currentContext();
        return processViewRepository.findArchives(identityContext.tenantId(), identityContext.personId());
    }

    public TodoCounts counts() {
        TodoIdentityContext identityContext = identityContextProvider.currentContext();
        long pendingCount = todoItemRepository.countByTenantIdAndAssigneeIdAndStatus(
                identityContext.tenantId(),
                identityContext.assignmentId(),
                TodoItemStatus.PENDING
        );
        long completedCount = todoItemRepository.countByTenantIdAndAssigneeIdAndStatus(
                identityContext.tenantId(),
                identityContext.assignmentId(),
                TodoItemStatus.COMPLETED
        );
        long overdueCount = overdueTodos().size();
        long copiedUnreadCount = copiedTodoRepository.countUnreadByTenantIdAndRecipientAssignmentId(
                identityContext.tenantId(),
                identityContext.assignmentId()
        );
        return new TodoCounts(
                pendingCount,
                completedCount,
                overdueCount,
                initiatedProcesses().size(),
                copiedUnreadCount,
                draftProcesses().size(),
                archivedProcesses().size()
        );
    }
}
