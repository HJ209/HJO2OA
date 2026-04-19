package com.hjo2oa.portal.aggregation.api.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.hjo2oa.msg.message.center.domain.MsgNotificationReadEvent;
import com.hjo2oa.msg.message.center.domain.MsgNotificationSentEvent;
import com.hjo2oa.org.identity.context.domain.IdentityAssignmentType;
import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidatedEvent;
import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidationReason;
import com.hjo2oa.org.identity.context.domain.IdentitySwitchedEvent;
import com.hjo2oa.portal.aggregation.api.application.PortalSnapshotInvalidationApplicationService;
import com.hjo2oa.todo.center.domain.TodoItemCreatedEvent;
import com.hjo2oa.todo.center.domain.TodoItemOverdueEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class PortalSnapshotInvalidationListenerBindingTest {

    @Test
    void shouldDispatchPublishedEventsToInvalidationApplicationService() {
        PortalSnapshotInvalidationApplicationService invalidationApplicationService =
                mock(PortalSnapshotInvalidationApplicationService.class);

        IdentitySwitchedEvent identitySwitchedEvent = new IdentitySwitchedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:00:00Z"),
                "tenant-1",
                "person-1",
                "account-1",
                "assignment-1",
                "assignment-2",
                "position-1",
                "position-2",
                IdentityAssignmentType.PRIMARY,
                IdentityAssignmentType.SECONDARY,
                "manual-switch"
        );
        IdentityContextInvalidatedEvent invalidatedEvent = new IdentityContextInvalidatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:01:00Z"),
                "tenant-1",
                "person-1",
                "account-1",
                "assignment-1",
                "assignment-2",
                IdentityContextInvalidationReason.PRIMARY_CHANGED,
                false,
                2L,
                "org.assignment.changed"
        );
        TodoItemCreatedEvent todoItemCreatedEvent = new TodoItemCreatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:02:00Z"),
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
        TodoItemOverdueEvent todoItemOverdueEvent = new TodoItemOverdueEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:02:30Z"),
                "tenant-1",
                "todo-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "EXPENSE",
                "Approve expense request",
                Instant.parse("2026-04-19T10:00:00Z"),
                Duration.ofMinutes(122)
        );
        MsgNotificationSentEvent notificationSentEvent = new MsgNotificationSentEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:03:00Z"),
                "tenant-1",
                "notification-1",
                "assignment-1",
                "INBOX",
                "TODO_CREATED",
                "todo-center"
        );
        MsgNotificationReadEvent notificationReadEvent = new MsgNotificationReadEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:04:00Z"),
                "tenant-1",
                "notification-1",
                "assignment-1",
                Instant.parse("2026-04-19T12:04:00Z")
        );

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(PortalSnapshotInvalidationApplicationService.class, () -> invalidationApplicationService);
            context.registerBean(PortalSnapshotInvalidationListener.class);
            context.refresh();

            context.publishEvent(identitySwitchedEvent);
            context.publishEvent(invalidatedEvent);
            context.publishEvent(todoItemCreatedEvent);
            context.publishEvent(todoItemOverdueEvent);
            context.publishEvent(notificationSentEvent);
            context.publishEvent(notificationReadEvent);
        }

        verify(invalidationApplicationService, times(6))
                .markStale(any(), anySet(), anyString());
    }
}
