package com.hjo2oa.todo.center.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.shared.messaging.DomainEvent;
import com.hjo2oa.todo.center.domain.TodoBatchActionResult;
import com.hjo2oa.todo.center.domain.TodoIdentityContext;
import com.hjo2oa.todo.center.domain.TodoItem;
import com.hjo2oa.todo.center.domain.TodoItemCompletedEvent;
import com.hjo2oa.todo.center.domain.TodoItemRemindedEvent;
import com.hjo2oa.todo.center.domain.TodoItemRepository;
import com.hjo2oa.todo.center.domain.TodoItemStatus;
import com.hjo2oa.todo.center.infrastructure.InMemoryTodoActionLogRepository;
import com.hjo2oa.todo.center.infrastructure.InMemoryTodoItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TodoActionApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-29T08:00:00Z");

    @Test
    void shouldCompleteTodoAndPublishCompletionNotificationEvent() {
        List<DomainEvent> events = new ArrayList<>();
        TodoItemRepository repository = new InMemoryTodoItemRepository();
        repository.save(todo("todo-1", "task-1"));
        TodoActionApplicationService service = service(repository, events);

        var completed = service.complete("todo-1", "complete-1");
        var replay = service.complete("todo-1", "complete-1");

        assertThat(completed.status()).isEqualTo(TodoItemStatus.COMPLETED);
        assertThat(completed.completedAt()).isEqualTo(FIXED_TIME);
        assertThat(replay.status()).isEqualTo(TodoItemStatus.COMPLETED);
        assertThat(events).singleElement().isInstanceOf(TodoItemCompletedEvent.class);
    }

    @Test
    void shouldBatchCompleteAndRemindVisibleTodos() {
        List<DomainEvent> events = new ArrayList<>();
        TodoItemRepository repository = new InMemoryTodoItemRepository();
        repository.save(todo("todo-1", "task-1"));
        repository.save(todo("todo-2", "task-2"));
        repository.save(todo("todo-other", "task-other").transferTo("assignment-2", FIXED_TIME));
        TodoActionApplicationService service = service(repository, events);

        service.remind("todo-1", "Please handle it", "remind-1");
        TodoBatchActionResult result = service.batchComplete(
                List.of("todo-1", "todo-2", "todo-other"),
                "batch-complete-1"
        );

        assertThat(result.requestedCount()).isEqualTo(3);
        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.skippedTodoIds()).containsExactly("todo-other");
        assertThat(events).anyMatch(event -> event instanceof TodoItemRemindedEvent);
        assertThat(events.stream().filter(event -> event instanceof TodoItemCompletedEvent)).hasSize(2);
    }

    private TodoActionApplicationService service(TodoItemRepository repository, List<DomainEvent> events) {
        return new TodoActionApplicationService(
                repository,
                new InMemoryTodoActionLogRepository(),
                () -> new TodoIdentityContext("tenant-1", "person-1", "assignment-1", "position-1"),
                events::add,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    private TodoItem todo(String todoId, String taskId) {
        return new TodoItem(
                todoId,
                taskId,
                "instance-1",
                "tenant-1",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Approve expense request",
                "HIGH",
                TodoItemStatus.PENDING,
                Instant.parse("2026-04-30T08:00:00Z"),
                null,
                Instant.parse("2026-04-29T07:00:00Z"),
                Instant.parse("2026-04-29T07:00:00Z"),
                null,
                null
        );
    }
}
