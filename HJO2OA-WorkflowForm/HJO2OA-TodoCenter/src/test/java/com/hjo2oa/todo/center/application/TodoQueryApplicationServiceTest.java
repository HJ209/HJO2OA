package com.hjo2oa.todo.center.application;

import com.hjo2oa.todo.center.domain.CopiedTodoItem;
import com.hjo2oa.todo.center.domain.CopiedTodoReadStatus;
import com.hjo2oa.todo.center.domain.CopiedTodoSummary;
import com.hjo2oa.todo.center.domain.TodoCounts;
import com.hjo2oa.todo.center.domain.TodoIdentityContext;
import com.hjo2oa.todo.center.domain.TodoIdentityContextProvider;
import com.hjo2oa.todo.center.domain.TodoItem;
import com.hjo2oa.todo.center.domain.TodoItemRepository;
import com.hjo2oa.todo.center.domain.TodoItemStatus;
import com.hjo2oa.todo.center.domain.TodoItemSummary;
import com.hjo2oa.todo.center.infrastructure.InMemoryCopiedTodoRepository;
import com.hjo2oa.todo.center.infrastructure.InMemoryTodoItemRepository;
import com.hjo2oa.todo.center.infrastructure.InMemoryTodoProcessViewRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TodoQueryApplicationServiceTest {

    @Test
    void shouldReturnPendingTodosForCurrentIdentityContext() {
        TodoItemRepository repository = new InMemoryTodoItemRepository();
        repository.save(new TodoItem(
                "todo-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Approve expense request",
                "HIGH",
                TodoItemStatus.PENDING,
                Instant.parse("2026-04-20T10:00:00Z"),
                null,
                Instant.parse("2026-04-19T10:00:00Z"),
                Instant.parse("2026-04-19T10:00:00Z"),
                null,
                null
        ));
        repository.save(new TodoItem(
                "todo-2",
                "task-2",
                "instance-2",
                "assignment-2",
                "APPROVAL",
                "PROCUREMENT",
                "Approve procurement request",
                "MEDIUM",
                TodoItemStatus.PENDING,
                Instant.parse("2026-04-21T10:00:00Z"),
                null,
                Instant.parse("2026-04-19T11:00:00Z"),
                Instant.parse("2026-04-19T11:00:00Z"),
                null,
                null
        ));
        repository.save(new TodoItem(
                "todo-3",
                "task-3",
                "instance-3",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Completed request",
                "LOW",
                TodoItemStatus.COMPLETED,
                null,
                null,
                Instant.parse("2026-04-19T09:00:00Z"),
                Instant.parse("2026-04-19T12:00:00Z"),
                Instant.parse("2026-04-19T12:00:00Z"),
                null
        ));

        TodoIdentityContextProvider identityContextProvider = () -> new TodoIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        );

        TodoQueryApplicationService service = new TodoQueryApplicationService(
                repository,
                new InMemoryCopiedTodoRepository(),
                new InMemoryTodoProcessViewRepository(),
                identityContextProvider
        );

        List<TodoItemSummary> pendingTodos = service.pendingTodos();
        List<TodoItemSummary> completedTodos = service.completedTodos();
        List<TodoItemSummary> overdueTodos = service.overdueTodos();
        TodoCounts counts = service.counts();

        assertEquals(1, pendingTodos.size());
        assertEquals("todo-1", pendingTodos.get(0).todoId());
        assertEquals(1, completedTodos.size());
        assertEquals("todo-3", completedTodos.get(0).todoId());
        assertEquals(0, overdueTodos.size());
        assertEquals(1L, counts.pendingCount());
        assertEquals(1L, counts.completedCount());
        assertEquals(0L, counts.overdueCount());
    }

    @Test
    void shouldReturnCompletedTodosInDescendingCompletionOrder() {
        TodoItemRepository repository = new InMemoryTodoItemRepository();
        repository.save(new TodoItem(
                "todo-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Completed earlier",
                "LOW",
                TodoItemStatus.COMPLETED,
                null,
                null,
                Instant.parse("2026-04-19T09:00:00Z"),
                Instant.parse("2026-04-19T12:00:00Z"),
                Instant.parse("2026-04-19T12:00:00Z"),
                null
        ));
        repository.save(new TodoItem(
                "todo-2",
                "task-2",
                "instance-2",
                "assignment-1",
                "APPROVAL",
                "PROCUREMENT",
                "Completed later",
                "MEDIUM",
                TodoItemStatus.COMPLETED,
                null,
                null,
                Instant.parse("2026-04-19T10:00:00Z"),
                Instant.parse("2026-04-19T13:00:00Z"),
                Instant.parse("2026-04-19T13:00:00Z"),
                null
        ));

        TodoIdentityContextProvider identityContextProvider = () -> new TodoIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        );

        TodoQueryApplicationService service = new TodoQueryApplicationService(
                repository,
                new InMemoryCopiedTodoRepository(),
                new InMemoryTodoProcessViewRepository(),
                identityContextProvider
        );

        List<TodoItemSummary> completedTodos = service.completedTodos();

        assertEquals(2, completedTodos.size());
        assertEquals("todo-2", completedTodos.get(0).todoId());
        assertEquals("todo-1", completedTodos.get(1).todoId());
    }

    @Test
    void shouldReturnOverdueTodosAndExposeOverdueCount() {
        TodoItemRepository repository = new InMemoryTodoItemRepository();
        repository.save(new TodoItem(
                "todo-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Overdue earlier",
                "HIGH",
                TodoItemStatus.PENDING,
                Instant.parse("2026-04-19T09:00:00Z"),
                Instant.parse("2026-04-19T10:00:00Z"),
                Instant.parse("2026-04-19T08:00:00Z"),
                Instant.parse("2026-04-19T10:00:00Z"),
                null,
                null
        ));
        repository.save(new TodoItem(
                "todo-2",
                "task-2",
                "instance-2",
                "assignment-1",
                "APPROVAL",
                "PROCUREMENT",
                "Overdue later",
                "CRITICAL",
                TodoItemStatus.PENDING,
                Instant.parse("2026-04-19T09:30:00Z"),
                Instant.parse("2026-04-19T11:00:00Z"),
                Instant.parse("2026-04-19T08:30:00Z"),
                Instant.parse("2026-04-19T11:00:00Z"),
                null,
                null
        ));
        repository.save(new TodoItem(
                "todo-3",
                "task-3",
                "instance-3",
                "assignment-1",
                "APPROVAL",
                "PROCUREMENT",
                "Not overdue",
                "LOW",
                TodoItemStatus.PENDING,
                Instant.parse("2026-04-20T09:30:00Z"),
                null,
                Instant.parse("2026-04-19T08:45:00Z"),
                Instant.parse("2026-04-19T08:45:00Z"),
                null,
                null
        ));

        TodoIdentityContextProvider identityContextProvider = () -> new TodoIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        );

        TodoQueryApplicationService service = new TodoQueryApplicationService(
                repository,
                new InMemoryCopiedTodoRepository(),
                new InMemoryTodoProcessViewRepository(),
                identityContextProvider
        );

        List<TodoItemSummary> overdueTodos = service.overdueTodos();
        TodoCounts counts = service.counts();

        assertEquals(2, overdueTodos.size());
        assertEquals("todo-2", overdueTodos.get(0).todoId());
        assertEquals("todo-1", overdueTodos.get(1).todoId());
        assertEquals(2L, counts.overdueCount());
    }

    @Test
    void shouldReturnCopiedTodosFilteredByReadStatusAndExposeUnreadCount() {
        TodoItemRepository repository = new InMemoryTodoItemRepository();
        InMemoryCopiedTodoRepository copiedTodoRepository = new InMemoryCopiedTodoRepository();
        copiedTodoRepository.save(CopiedTodoItem.unread(
                "copied-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Newest copied todo",
                "HIGH",
                Instant.parse("2026-04-19T11:00:00Z")
        ));
        copiedTodoRepository.save(CopiedTodoItem.unread(
                "copied-3",
                "task-3",
                "instance-3",
                "assignment-2",
                "APPROVAL",
                "PROCUREMENT",
                "Invisible copied todo",
                "NORMAL",
                Instant.parse("2026-04-19T10:30:00Z")
        ));
        copiedTodoRepository.save(CopiedTodoItem.unread(
                "copied-2",
                "task-2",
                "instance-2",
                "assignment-1",
                "APPROVAL",
                "PROCUREMENT",
                "Older copied todo",
                "NORMAL",
                Instant.parse("2026-04-19T10:00:00Z")
        ).markRead(Instant.parse("2026-04-19T10:15:00Z")));

        TodoIdentityContextProvider identityContextProvider = () -> new TodoIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        );
        TodoQueryApplicationService service = new TodoQueryApplicationService(
                repository,
                copiedTodoRepository,
                new InMemoryTodoProcessViewRepository(),
                identityContextProvider
        );

        List<CopiedTodoSummary> copiedTodos = service.copiedTodos(null);
        List<CopiedTodoSummary> unreadCopiedTodos = service.copiedTodos(CopiedTodoReadStatus.UNREAD);
        List<CopiedTodoSummary> readCopiedTodos = service.copiedTodos(CopiedTodoReadStatus.READ);
        TodoCounts counts = service.counts();

        assertEquals(2, copiedTodos.size());
        assertEquals("copied-1", copiedTodos.get(0).todoId());
        assertEquals("copied-2", copiedTodos.get(1).todoId());
        assertEquals(1, unreadCopiedTodos.size());
        assertEquals("copied-1", unreadCopiedTodos.get(0).todoId());
        assertEquals(1, readCopiedTodos.size());
        assertEquals("copied-2", readCopiedTodos.get(0).todoId());
        assertEquals(1L, counts.copiedUnreadCount());
    }
}
