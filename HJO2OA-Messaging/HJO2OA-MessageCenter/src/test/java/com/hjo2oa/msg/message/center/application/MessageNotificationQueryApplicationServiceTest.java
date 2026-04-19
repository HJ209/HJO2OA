package com.hjo2oa.msg.message.center.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hjo2oa.msg.message.center.domain.MessageIdentityContext;
import com.hjo2oa.msg.message.center.domain.MessageIdentityContextProvider;
import com.hjo2oa.msg.message.center.domain.Notification;
import com.hjo2oa.msg.message.center.domain.NotificationDetail;
import com.hjo2oa.msg.message.center.domain.NotificationCategory;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryChannel;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecord;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecordRepository;
import com.hjo2oa.msg.message.center.domain.NotificationPriority;
import com.hjo2oa.msg.message.center.domain.NotificationRepository;
import com.hjo2oa.msg.message.center.domain.NotificationSummary;
import com.hjo2oa.msg.message.center.domain.NotificationUnreadSummary;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationDeliveryRecordRepository;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageNotificationQueryApplicationServiceTest {

    @Test
    void shouldReturnVisibleInboxAndUnreadSummary() {
        NotificationRepository notificationRepository = new InMemoryNotificationRepository();
        NotificationDeliveryRecordRepository deliveryRecordRepository = new InMemoryNotificationDeliveryRecordRepository();
        MessageIdentityContextProvider identityContextProvider = () -> new MessageIdentityContext(
                "tenant-1",
                "assignment-1",
                "assignment-1",
                "position-1"
        );

        Notification visibleNotification = Notification.create(
                "notification-1",
                "todo.item.created:todo-1",
                "tenant-1",
                "assignment-1",
                "assignment-1",
                null,
                "Approve expense request",
                "Todo item requires attention",
                "/portal/todo/todo-1",
                NotificationCategory.TODO_CREATED,
                NotificationPriority.NORMAL,
                "todo-center",
                "todo.item.created",
                "todo-1",
                Instant.parse("2026-04-19T16:00:00Z")
        );
        Notification hiddenNotification = Notification.create(
                "notification-2",
                "todo.item.created:todo-2",
                "tenant-1",
                "assignment-2",
                "assignment-2",
                null,
                "Approve another request",
                "Todo item requires attention",
                "/portal/todo/todo-2",
                NotificationCategory.TODO_CREATED,
                NotificationPriority.NORMAL,
                "todo-center",
                "todo.item.created",
                "todo-2",
                Instant.parse("2026-04-19T15:00:00Z")
        );

        notificationRepository.save(visibleNotification);
        notificationRepository.save(hiddenNotification);
        deliveryRecordRepository.save(
                NotificationDeliveryRecord.pending(
                                UUID.randomUUID().toString(),
                                visibleNotification.notificationId(),
                                NotificationDeliveryChannel.INBOX,
                                visibleNotification.createdAt()
                        )
                        .markDelivered(visibleNotification.createdAt())
        );

        MessageNotificationQueryApplicationService service = new MessageNotificationQueryApplicationService(
                notificationRepository,
                deliveryRecordRepository,
                identityContextProvider
        );

        List<NotificationSummary> inbox = service.inbox();
        NotificationUnreadSummary unreadSummary = service.unreadSummary();

        assertEquals(1, inbox.size());
        assertEquals("notification-1", inbox.get(0).notificationId());
        assertEquals(1, unreadSummary.totalUnreadCount());
        assertEquals(1L, unreadSummary.categoryUnreadCounts().get("TODO_CREATED"));
        assertEquals(List.of("notification-1"), unreadSummary.latestNotificationIds());
    }

    @Test
    void shouldReturnVisibleNotificationDetail() {
        NotificationRepository notificationRepository = new InMemoryNotificationRepository();
        NotificationDeliveryRecordRepository deliveryRecordRepository = new InMemoryNotificationDeliveryRecordRepository();
        MessageIdentityContextProvider identityContextProvider = () -> new MessageIdentityContext(
                "tenant-1",
                "assignment-1",
                "assignment-1",
                "position-1"
        );

        Notification notification = Notification.create(
                "notification-1",
                "todo.item.created:todo-1",
                "tenant-1",
                "assignment-1",
                "assignment-1",
                null,
                "Approve expense request",
                "Todo item requires attention",
                "/portal/todo/todo-1",
                NotificationCategory.TODO_CREATED,
                NotificationPriority.NORMAL,
                "todo-center",
                "todo.item.created",
                "todo-1",
                Instant.parse("2026-04-19T16:00:00Z")
        );
        notificationRepository.save(notification.markRead(Instant.parse("2026-04-19T16:05:00Z")));
        deliveryRecordRepository.save(
                NotificationDeliveryRecord.pending(
                                UUID.randomUUID().toString(),
                                notification.notificationId(),
                                NotificationDeliveryChannel.INBOX,
                                notification.createdAt()
                        )
                        .markDelivered(notification.createdAt())
        );

        MessageNotificationQueryApplicationService service = new MessageNotificationQueryApplicationService(
                notificationRepository,
                deliveryRecordRepository,
                identityContextProvider
        );

        NotificationDetail detail = service.detail("notification-1").orElseThrow();

        assertEquals("notification-1", detail.notificationId());
        assertEquals("todo.item.created", detail.sourceEventType());
        assertEquals("todo-1", detail.sourceBusinessId());
        assertEquals("assignment-1", detail.targetAssignmentId());
        assertEquals(Instant.parse("2026-04-19T16:05:00Z"), detail.readAt());
    }
}
