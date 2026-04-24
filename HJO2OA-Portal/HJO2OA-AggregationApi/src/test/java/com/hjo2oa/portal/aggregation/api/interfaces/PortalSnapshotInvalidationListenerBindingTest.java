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
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.personalization.domain.PortalPersonalizationResetEvent;
import com.hjo2oa.portal.personalization.domain.PortalPersonalizationSavedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationActivatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationOfflinedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
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
        PortalPublicationActivatedEvent publicationActivatedEvent = new PortalPublicationActivatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:05:00Z"),
                "tenant-1",
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC
        );
        PortalPublicationOfflinedEvent publicationOfflinedEvent = new PortalPublicationOfflinedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:06:00Z"),
                "tenant-1",
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME
        );
        PortalPersonalizationSavedEvent personalizationSavedEvent = new PortalPersonalizationSavedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:07:00Z"),
                "tenant-1",
                "profile-1",
                "person-1",
                PersonalizationSceneType.HOME
        );
        PortalPersonalizationResetEvent personalizationResetEvent = new PortalPersonalizationResetEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:08:00Z"),
                "tenant-1",
                "profile-1",
                "person-1",
                PersonalizationSceneType.HOME
        );
        PortalWidgetUpdatedEvent widgetUpdatedEvent = new PortalWidgetUpdatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:09:00Z"),
                "tenant-1",
                "widget-1",
                "todo-card",
                WidgetCardType.TODO,
                WidgetSceneType.OFFICE_CENTER,
                java.util.List.of("displayName")
        );
        PortalWidgetDisabledEvent widgetDisabledEvent = new PortalWidgetDisabledEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:10:00Z"),
                "tenant-1",
                "widget-2",
                "message-card",
                WidgetCardType.MESSAGE,
                null
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
            context.publishEvent(publicationActivatedEvent);
            context.publishEvent(publicationOfflinedEvent);
            context.publishEvent(personalizationSavedEvent);
            context.publishEvent(personalizationResetEvent);
            context.publishEvent(widgetUpdatedEvent);
            context.publishEvent(widgetDisabledEvent);
        }

        verify(invalidationApplicationService, times(14))
                .markStale(any(), anySet(), anyString());
    }
}
