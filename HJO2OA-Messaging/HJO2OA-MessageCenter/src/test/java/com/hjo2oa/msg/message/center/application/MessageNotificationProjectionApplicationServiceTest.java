package com.hjo2oa.msg.message.center.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hjo2oa.msg.message.center.domain.MsgNotificationDeliveredEvent;
import com.hjo2oa.msg.message.center.domain.MsgNotificationSentEvent;
import com.hjo2oa.msg.message.center.domain.Notification;
import com.hjo2oa.msg.message.center.domain.NotificationCategory;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryChannel;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecord;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecordRepository;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryStatus;
import com.hjo2oa.msg.message.center.domain.NotificationPriority;
import com.hjo2oa.msg.message.center.domain.NotificationProjectionEventLog;
import com.hjo2oa.msg.message.center.domain.NotificationRepository;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationDeliveryRecordRepository;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationProjectionEventLog;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationRepository;
import com.hjo2oa.msg.message.center.infrastructure.InboxNotificationChannelDispatcher;
import com.hjo2oa.shared.messaging.DomainEvent;
import com.hjo2oa.todo.center.domain.TodoItemCreatedEvent;
import com.hjo2oa.todo.center.domain.TodoItemCompletedEvent;
import com.hjo2oa.todo.center.domain.TodoItemOverdueEvent;
import com.hjo2oa.todo.center.domain.TodoItemRemindedEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageNotificationProjectionApplicationServiceTest {

    @Test
    void shouldCreateInboxNotificationAndPublishNotificationEvents() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        NotificationRepository notificationRepository = new InMemoryNotificationRepository();
        NotificationDeliveryRecordRepository deliveryRecordRepository = new InMemoryNotificationDeliveryRecordRepository();
        NotificationProjectionEventLog eventLog = new InMemoryNotificationProjectionEventLog();
        MessageNotificationProjectionApplicationService service = new MessageNotificationProjectionApplicationService(
                notificationRepository,
                deliveryRecordRepository,
                eventLog,
                List.of(new InboxNotificationChannelDispatcher()),
                publishedEvents::add
        );

        service.onTodoItemCreated(new TodoItemCreatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T16:00:00Z"),
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
        ));

        Notification notification = notificationRepository.findByDedupKey("todo.item.created:todo-1").orElseThrow();
        assertEquals("assignment-1", notification.recipientId());
        assertEquals(NotificationCategory.TODO_CREATED, notification.category());
        assertEquals(NotificationPriority.URGENT, notification.priority());

        NotificationDeliveryRecord deliveryRecord = deliveryRecordRepository
                .findByNotificationIdAndChannel(notification.notificationId(), NotificationDeliveryChannel.INBOX)
                .orElseThrow();
        assertEquals(NotificationDeliveryStatus.DELIVERED, deliveryRecord.status());
        assertEquals(2, publishedEvents.size());
        assertInstanceOf(MsgNotificationSentEvent.class, publishedEvents.get(0));
        assertInstanceOf(MsgNotificationDeliveredEvent.class, publishedEvents.get(1));
    }

    @Test
    void shouldIgnoreDuplicateTodoReminderByDedupKey() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        NotificationRepository notificationRepository = new InMemoryNotificationRepository();
        NotificationDeliveryRecordRepository deliveryRecordRepository = new InMemoryNotificationDeliveryRecordRepository();
        NotificationProjectionEventLog eventLog = new InMemoryNotificationProjectionEventLog();
        MessageNotificationProjectionApplicationService service = new MessageNotificationProjectionApplicationService(
                notificationRepository,
                deliveryRecordRepository,
                eventLog,
                List.of(new InboxNotificationChannelDispatcher()),
                publishedEvents::add
        );

        service.onTodoItemCreated(new TodoItemCreatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T16:00:00Z"),
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
        ));
        service.onTodoItemCreated(new TodoItemCreatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T16:01:00Z"),
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
        ));

        assertEquals(1, notificationRepository.findAll().size());
        assertEquals(2, publishedEvents.size());
    }

    @Test
    void shouldCreateOverdueReminderWithCriticalPriority() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        NotificationRepository notificationRepository = new InMemoryNotificationRepository();
        NotificationDeliveryRecordRepository deliveryRecordRepository = new InMemoryNotificationDeliveryRecordRepository();
        NotificationProjectionEventLog eventLog = new InMemoryNotificationProjectionEventLog();
        MessageNotificationProjectionApplicationService service = new MessageNotificationProjectionApplicationService(
                notificationRepository,
                deliveryRecordRepository,
                eventLog,
                List.of(new InboxNotificationChannelDispatcher()),
                publishedEvents::add
        );

        service.onTodoItemOverdue(new TodoItemOverdueEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T18:00:00Z"),
                "tenant-1",
                "todo-2",
                "task-2",
                "instance-2",
                "assignment-1",
                "EXPENSE",
                "Approve expense request",
                Instant.parse("2026-04-19T17:30:00Z"),
                Duration.ofMinutes(30)
        ));

        Notification notification = notificationRepository.findByDedupKey("todo.item.overdue:todo-2").orElseThrow();
        assertEquals(NotificationCategory.TODO_OVERDUE, notification.category());
        assertEquals(NotificationPriority.CRITICAL, notification.priority());
        assertTrue(notification.title().startsWith("Overdue: "));
        assertEquals(2, publishedEvents.size());
    }

    @Test
    void shouldCreateReminderAndCompletionNotifications() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        NotificationRepository notificationRepository = new InMemoryNotificationRepository();
        NotificationDeliveryRecordRepository deliveryRecordRepository = new InMemoryNotificationDeliveryRecordRepository();
        NotificationProjectionEventLog eventLog = new InMemoryNotificationProjectionEventLog();
        MessageNotificationProjectionApplicationService service = new MessageNotificationProjectionApplicationService(
                notificationRepository,
                deliveryRecordRepository,
                eventLog,
                List.of(new InboxNotificationChannelDispatcher()),
                publishedEvents::add
        );

        service.onTodoItemReminded(new TodoItemRemindedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T18:00:00Z"),
                "tenant-1",
                "todo-3",
                "task-3",
                "instance-3",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Approve expense request",
                "Please handle it"
        ));
        service.onTodoItemCompleted(new TodoItemCompletedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T18:10:00Z"),
                "tenant-1",
                "todo-3",
                "task-3",
                "instance-3",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Approve expense request",
                Instant.parse("2026-04-19T18:10:00Z")
        ));

        assertEquals(2, notificationRepository.findAll().size());
        assertTrue(notificationRepository.findAll().stream()
                .anyMatch(notification -> notification.category() == NotificationCategory.TODO_REMINDER));
        assertTrue(notificationRepository.findAll().stream()
                .anyMatch(notification -> notification.category() == NotificationCategory.TODO_COMPLETED));
        assertEquals(4, publishedEvents.size());
    }

    @Test
    void shouldIgnoreRepeatedOverdueReminderByDedupKey() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        NotificationRepository notificationRepository = new InMemoryNotificationRepository();
        NotificationDeliveryRecordRepository deliveryRecordRepository = new InMemoryNotificationDeliveryRecordRepository();
        NotificationProjectionEventLog eventLog = new InMemoryNotificationProjectionEventLog();
        MessageNotificationProjectionApplicationService service = new MessageNotificationProjectionApplicationService(
                notificationRepository,
                deliveryRecordRepository,
                eventLog,
                List.of(new InboxNotificationChannelDispatcher()),
                publishedEvents::add
        );

        service.onTodoItemOverdue(new TodoItemOverdueEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T18:00:00Z"),
                "tenant-1",
                "todo-2",
                "task-2",
                "instance-2",
                "assignment-1",
                "EXPENSE",
                "Approve expense request",
                Instant.parse("2026-04-19T17:30:00Z"),
                Duration.ofMinutes(30)
        ));
        service.onTodoItemOverdue(new TodoItemOverdueEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T18:05:00Z"),
                "tenant-1",
                "todo-2",
                "task-2",
                "instance-2",
                "assignment-1",
                "EXPENSE",
                "Approve expense request",
                Instant.parse("2026-04-19T17:30:00Z"),
                Duration.ofMinutes(35)
        ));

        assertEquals(1, notificationRepository.findAll().size());
        assertEquals(2, publishedEvents.size());
    }
}
