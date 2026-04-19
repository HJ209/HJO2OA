package com.hjo2oa.todo.center.interfaces;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.hjo2oa.todo.center.application.TodoProjectionApplicationService;
import com.hjo2oa.todo.center.domain.ProcessTaskCompletedEvent;
import com.hjo2oa.todo.center.domain.ProcessTaskCreatedEvent;
import com.hjo2oa.todo.center.domain.ProcessTaskOverdueEvent;
import com.hjo2oa.todo.center.domain.ProcessTaskTerminatedEvent;
import com.hjo2oa.todo.center.domain.ProcessTaskTransferredEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class TodoProcessEventListenerBindingTest {

    @Test
    void shouldDispatchPublishedProcessEventsToProjectionService() {
        TodoProjectionApplicationService projectionApplicationService = mock(TodoProjectionApplicationService.class);

        ProcessTaskCreatedEvent createdEvent = new ProcessTaskCreatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T10:00:00Z"),
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
        ProcessTaskCompletedEvent completedEvent = new ProcessTaskCompletedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T10:05:00Z"),
                "tenant-1",
                "task-1",
                "instance-1",
                Instant.parse("2026-04-19T10:05:00Z")
        );
        ProcessTaskTerminatedEvent terminatedEvent = new ProcessTaskTerminatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T10:06:00Z"),
                "tenant-1",
                "task-2",
                "instance-2",
                "instance-cancelled"
        );
        ProcessTaskTransferredEvent transferredEvent = new ProcessTaskTransferredEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T10:07:00Z"),
                "tenant-1",
                "task-3",
                "instance-3",
                "assignment-1",
                "assignment-2"
        );
        ProcessTaskOverdueEvent overdueEvent = new ProcessTaskOverdueEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T10:08:00Z"),
                "tenant-1",
                "task-4",
                "instance-4",
                Instant.parse("2026-04-19T09:30:00Z"),
                Duration.ofMinutes(38)
        );

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(TodoProjectionApplicationService.class, () -> projectionApplicationService);
            context.registerBean(TodoProcessEventListener.class);
            context.refresh();

            context.publishEvent(createdEvent);
            context.publishEvent(completedEvent);
            context.publishEvent(terminatedEvent);
            context.publishEvent(transferredEvent);
            context.publishEvent(overdueEvent);
        }

        verify(projectionApplicationService).onTaskCreated(createdEvent);
        verify(projectionApplicationService).onTaskCompleted(completedEvent);
        verify(projectionApplicationService).onTaskTerminated(terminatedEvent);
        verify(projectionApplicationService).onTaskTransferred(transferredEvent);
        verify(projectionApplicationService).onTaskOverdue(overdueEvent);
    }
}
