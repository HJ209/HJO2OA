package com.hjo2oa.todo.center.application;

import com.hjo2oa.shared.messaging.DomainEvent;
import com.hjo2oa.todo.center.domain.ProcessTaskCompletedEvent;
import com.hjo2oa.todo.center.domain.ProcessTaskCreatedEvent;
import com.hjo2oa.todo.center.domain.ProcessTaskOverdueEvent;
import com.hjo2oa.todo.center.domain.ProcessTaskTerminatedEvent;
import com.hjo2oa.todo.center.domain.ProcessTaskTransferredEvent;
import com.hjo2oa.todo.center.domain.TodoItem;
import com.hjo2oa.todo.center.domain.TodoItemCancelledEvent;
import com.hjo2oa.todo.center.domain.TodoItemCompletedEvent;
import com.hjo2oa.todo.center.domain.TodoItemCreatedEvent;
import com.hjo2oa.todo.center.domain.TodoItemOverdueEvent;
import com.hjo2oa.todo.center.domain.TodoItemRepository;
import com.hjo2oa.todo.center.domain.TodoItemStatus;
import com.hjo2oa.todo.center.domain.TodoProjectionEventLog;
import com.hjo2oa.todo.center.infrastructure.InMemoryTodoItemRepository;
import com.hjo2oa.todo.center.infrastructure.InMemoryTodoProjectionEventLog;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TodoProjectionApplicationServiceTest {

    @Test
    void shouldCreateProjectionAndPublishCreatedEvent() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        TodoItemRepository repository = new InMemoryTodoItemRepository();
        TodoProjectionEventLog eventLog = new InMemoryTodoProjectionEventLog();
        TodoProjectionApplicationService service = new TodoProjectionApplicationService(
                repository,
                eventLog,
                publishedEvents::add
        );

        ProcessTaskCreatedEvent event = new ProcessTaskCreatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T16:00:00Z"),
                "tenant-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Approve expense request",
                "HIGH",
                Instant.parse("2026-04-20T10:00:00Z")
        );

        service.onTaskCreated(event);

        TodoItem todoItem = repository.findByTaskId("task-1").orElseThrow();
        assertEquals(TodoItemStatus.PENDING, todoItem.status());
        assertEquals("assignment-1", todoItem.assigneeId());
        assertEquals(1, publishedEvents.size());
        assertInstanceOf(TodoItemCreatedEvent.class, publishedEvents.get(0));
    }

    @Test
    void shouldIgnoreDuplicateCreatedEvent() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        TodoItemRepository repository = new InMemoryTodoItemRepository();
        TodoProjectionEventLog eventLog = new InMemoryTodoProjectionEventLog();
        TodoProjectionApplicationService service = new TodoProjectionApplicationService(
                repository,
                eventLog,
                publishedEvents::add
        );

        UUID eventId = UUID.randomUUID();
        ProcessTaskCreatedEvent event = new ProcessTaskCreatedEvent(
                eventId,
                Instant.parse("2026-04-19T16:00:00Z"),
                "tenant-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Approve expense request",
                "HIGH",
                Instant.parse("2026-04-20T10:00:00Z")
        );

        service.onTaskCreated(event);
        service.onTaskCreated(event);

        assertEquals(1, publishedEvents.size());
        assertTrue(repository.findByTaskId("task-1").isPresent());
    }

    @Test
    void shouldCompleteProjectionAndPublishCompletedEvent() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        TodoItemRepository repository = new InMemoryTodoItemRepository();
        TodoProjectionEventLog eventLog = new InMemoryTodoProjectionEventLog();
        TodoProjectionApplicationService service = new TodoProjectionApplicationService(
                repository,
                eventLog,
                publishedEvents::add
        );

        service.onTaskCreated(new ProcessTaskCreatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T16:00:00Z"),
                "tenant-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Approve expense request",
                "HIGH",
                Instant.parse("2026-04-20T10:00:00Z")
        ));

        service.onTaskCompleted(new ProcessTaskCompletedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T17:00:00Z"),
                "tenant-1",
                "task-1",
                "instance-1",
                Instant.parse("2026-04-19T17:00:00Z")
        ));

        TodoItem todoItem = repository.findByTaskId("task-1").orElseThrow();
        assertEquals(TodoItemStatus.COMPLETED, todoItem.status());
        assertEquals(2, publishedEvents.size());
        assertInstanceOf(TodoItemCompletedEvent.class, publishedEvents.get(1));
    }

    @Test
    void shouldTransferAndTerminateProjection() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        TodoItemRepository repository = new InMemoryTodoItemRepository();
        TodoProjectionEventLog eventLog = new InMemoryTodoProjectionEventLog();
        TodoProjectionApplicationService service = new TodoProjectionApplicationService(
                repository,
                eventLog,
                publishedEvents::add
        );

        service.onTaskCreated(new ProcessTaskCreatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T16:00:00Z"),
                "tenant-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Approve expense request",
                "HIGH",
                Instant.parse("2026-04-20T10:00:00Z")
        ));

        service.onTaskTransferred(new ProcessTaskTransferredEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T16:30:00Z"),
                "tenant-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "assignment-2"
        ));
        service.onTaskTerminated(new ProcessTaskTerminatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T17:30:00Z"),
                "tenant-1",
                "task-1",
                "instance-1",
                "PROCESS_TERMINATED"
        ));

        TodoItem todoItem = repository.findByTaskId("task-1").orElseThrow();
        assertEquals("assignment-2", todoItem.assigneeId());
        assertEquals(TodoItemStatus.CANCELLED, todoItem.status());
        assertEquals(2, publishedEvents.size());
        assertInstanceOf(TodoItemCreatedEvent.class, publishedEvents.get(0));
        assertInstanceOf(TodoItemCancelledEvent.class, publishedEvents.get(1));
    }

    @Test
    void shouldMarkProjectionOverdueAndPublishOverdueEventOnce() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        TodoItemRepository repository = new InMemoryTodoItemRepository();
        TodoProjectionEventLog eventLog = new InMemoryTodoProjectionEventLog();
        TodoProjectionApplicationService service = new TodoProjectionApplicationService(
                repository,
                eventLog,
                publishedEvents::add
        );

        service.onTaskCreated(new ProcessTaskCreatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T16:00:00Z"),
                "tenant-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Approve expense request",
                "HIGH",
                Instant.parse("2026-04-19T15:30:00Z")
        ));

        service.onTaskOverdue(new ProcessTaskOverdueEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T16:05:00Z"),
                "tenant-1",
                "task-1",
                "instance-1",
                Instant.parse("2026-04-19T15:30:00Z"),
                Duration.ofMinutes(35)
        ));
        service.onTaskOverdue(new ProcessTaskOverdueEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T16:10:00Z"),
                "tenant-1",
                "task-1",
                "instance-1",
                Instant.parse("2026-04-19T15:30:00Z"),
                Duration.ofMinutes(40)
        ));

        TodoItem todoItem = repository.findByTaskId("task-1").orElseThrow();
        assertTrue(todoItem.isOverdue());
        assertEquals(2, publishedEvents.size());
        assertInstanceOf(TodoItemCreatedEvent.class, publishedEvents.get(0));
        assertInstanceOf(TodoItemOverdueEvent.class, publishedEvents.get(1));
    }
}
