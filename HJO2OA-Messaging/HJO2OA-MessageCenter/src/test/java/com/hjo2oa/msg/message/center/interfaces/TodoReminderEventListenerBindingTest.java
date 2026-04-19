package com.hjo2oa.msg.message.center.interfaces;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.hjo2oa.msg.message.center.application.MessageNotificationProjectionApplicationService;
import com.hjo2oa.todo.center.domain.TodoItemCreatedEvent;
import com.hjo2oa.todo.center.domain.TodoItemOverdueEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class TodoReminderEventListenerBindingTest {

    @Test
    void shouldDispatchPublishedTodoReminderEventsToProjectionService() {
        MessageNotificationProjectionApplicationService projectionApplicationService =
                mock(MessageNotificationProjectionApplicationService.class);

        TodoItemCreatedEvent createdEvent = new TodoItemCreatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T11:00:00Z"),
                "tenant-1",
                "todo-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Approve expense request",
                "HIGH",
                Instant.parse("2026-04-20T10:00:00Z")
        );
        TodoItemOverdueEvent overdueEvent = new TodoItemOverdueEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T11:05:00Z"),
                "tenant-1",
                "todo-2",
                "task-2",
                "instance-2",
                "assignment-1",
                "EXPENSE",
                "Approve expense request",
                Instant.parse("2026-04-19T10:30:00Z"),
                Duration.ofMinutes(35)
        );

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(MessageNotificationProjectionApplicationService.class, () -> projectionApplicationService);
            context.registerBean(TodoReminderEventListener.class);
            context.refresh();

            context.publishEvent(createdEvent);
            context.publishEvent(overdueEvent);
        }

        verify(projectionApplicationService).onTodoItemCreated(createdEvent);
        verify(projectionApplicationService).onTodoItemOverdue(overdueEvent);
    }
}
