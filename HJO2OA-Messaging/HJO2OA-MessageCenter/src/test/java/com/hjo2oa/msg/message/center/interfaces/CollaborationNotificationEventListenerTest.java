package com.hjo2oa.msg.message.center.interfaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hjo2oa.biz.collaboration.hub.domain.CollaborationEvents;
import com.hjo2oa.msg.message.center.application.MessageNotificationProjectionApplicationService;
import com.hjo2oa.msg.message.center.domain.Notification;
import com.hjo2oa.msg.message.center.domain.NotificationCategory;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecordRepository;
import com.hjo2oa.msg.message.center.domain.NotificationProjectionEventLog;
import com.hjo2oa.msg.message.center.domain.NotificationRepository;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationDeliveryRecordRepository;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationProjectionEventLog;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationRepository;
import com.hjo2oa.msg.message.center.infrastructure.InboxNotificationChannelDispatcher;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CollaborationNotificationEventListenerTest {

    @Test
    void shouldCreateRealInboxNotificationFromMentionEvent() {
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
        CollaborationNotificationEventListener listener = new CollaborationNotificationEventListener(service);

        listener.onMention(new CollaborationEvents.MentionCreatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-29T01:00:00Z"),
                "tenant-1",
                "assignment-1",
                "owner-1",
                "workspace-1",
                "DISCUSSION",
                "discussion-1",
                "Launch plan",
                "/collaboration?discussionId=discussion-1"
        ));

        Notification notification = notificationRepository.findAll().get(0);
        assertEquals("assignment-1", notification.recipientId());
        assertEquals(NotificationCategory.COLLAB_MENTION, notification.category());
        assertTrue(notification.deepLink().contains("discussionId=discussion-1"));
        assertEquals(2, publishedEvents.size());
    }
}
