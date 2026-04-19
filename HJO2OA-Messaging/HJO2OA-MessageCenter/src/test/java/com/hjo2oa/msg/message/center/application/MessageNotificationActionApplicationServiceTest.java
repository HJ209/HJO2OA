package com.hjo2oa.msg.message.center.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.msg.message.center.domain.MessageIdentityContext;
import com.hjo2oa.msg.message.center.domain.MessageIdentityContextProvider;
import com.hjo2oa.msg.message.center.domain.MsgNotificationReadEvent;
import com.hjo2oa.msg.message.center.domain.Notification;
import com.hjo2oa.msg.message.center.domain.NotificationActionRepository;
import com.hjo2oa.msg.message.center.domain.NotificationActionType;
import com.hjo2oa.msg.message.center.domain.NotificationBulkReadResult;
import com.hjo2oa.msg.message.center.domain.NotificationCategory;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryChannel;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecord;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecordRepository;
import com.hjo2oa.msg.message.center.domain.NotificationInboxStatus;
import com.hjo2oa.msg.message.center.domain.NotificationPriority;
import com.hjo2oa.msg.message.center.domain.NotificationRepository;
import com.hjo2oa.msg.message.center.domain.NotificationSummary;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationActionRepository;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationDeliveryRecordRepository;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationRepository;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class MessageNotificationActionApplicationServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-04-19T16:00:00Z");
    private static final Instant READ_AT = Instant.parse("2026-04-19T16:05:00Z");

    @Test
    void shouldMarkUnreadNotificationAsReadAndPublishReadEvent() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        NotificationRepository notificationRepository = new InMemoryNotificationRepository();
        NotificationActionRepository actionRepository = new InMemoryNotificationActionRepository();
        NotificationDeliveryRecordRepository deliveryRecordRepository = new InMemoryNotificationDeliveryRecordRepository();
        MessageIdentityContextProvider identityContextProvider = identityContextProvider();
        MessageNotificationQueryApplicationService queryApplicationService =
                new MessageNotificationQueryApplicationService(
                        notificationRepository,
                        deliveryRecordRepository,
                        identityContextProvider
                );
        MessageNotificationActionApplicationService service = new MessageNotificationActionApplicationService(
                notificationRepository,
                actionRepository,
                identityContextProvider,
                queryApplicationService,
                publishedEvents::add,
                Clock.fixed(READ_AT, ZoneOffset.UTC)
        );

        Notification notification = unreadNotification();
        notificationRepository.save(notification);
        deliveryRecordRepository.save(NotificationDeliveryRecord.pending(
                        "delivery-1",
                        notification.notificationId(),
                        NotificationDeliveryChannel.INBOX,
                        CREATED_AT
                )
                .markDelivered(CREATED_AT));

        NotificationSummary summary = service.markRead(notification.notificationId()).orElseThrow();

        Notification persistedNotification = notificationRepository.findByNotificationId(notification.notificationId()).orElseThrow();
        assertThat(summary.inboxStatus()).isEqualTo(NotificationInboxStatus.READ);
        assertThat(persistedNotification.inboxStatus()).isEqualTo(NotificationInboxStatus.READ);
        assertThat(persistedNotification.readAt()).isEqualTo(READ_AT);
        assertThat(actionRepository.findByNotificationId(notification.notificationId()))
                .singleElement()
                .satisfies(action -> {
                    assertThat(action.actionType()).isEqualTo(NotificationActionType.READ);
                    assertThat(action.operatorId()).isEqualTo("assignment-1");
                });
        assertThat(publishedEvents)
                .singleElement()
                .isInstanceOf(MsgNotificationReadEvent.class);
    }

    @Test
    void shouldTreatRepeatedReadAsIdempotentWithoutDuplicateActionOrEvent() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        NotificationRepository notificationRepository = new InMemoryNotificationRepository();
        NotificationActionRepository actionRepository = new InMemoryNotificationActionRepository();
        NotificationDeliveryRecordRepository deliveryRecordRepository = new InMemoryNotificationDeliveryRecordRepository();
        MessageIdentityContextProvider identityContextProvider = identityContextProvider();
        MessageNotificationQueryApplicationService queryApplicationService =
                new MessageNotificationQueryApplicationService(
                        notificationRepository,
                        deliveryRecordRepository,
                        identityContextProvider
                );
        MessageNotificationActionApplicationService service = new MessageNotificationActionApplicationService(
                notificationRepository,
                actionRepository,
                identityContextProvider,
                queryApplicationService,
                publishedEvents::add,
                Clock.fixed(READ_AT, ZoneOffset.UTC)
        );

        Notification notification = unreadNotification();
        notificationRepository.save(notification);

        service.markRead(notification.notificationId());
        NotificationSummary summary = service.markRead(notification.notificationId()).orElseThrow();

        assertThat(summary.inboxStatus()).isEqualTo(NotificationInboxStatus.READ);
        assertThat(actionRepository.findByNotificationId(notification.notificationId())).hasSize(1);
        assertThat(publishedEvents).hasSize(1);
    }

    @Test
    void shouldBulkMarkReadWithPartialSuccess() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        NotificationRepository notificationRepository = new InMemoryNotificationRepository();
        NotificationActionRepository actionRepository = new InMemoryNotificationActionRepository();
        NotificationDeliveryRecordRepository deliveryRecordRepository = new InMemoryNotificationDeliveryRecordRepository();
        MessageIdentityContextProvider identityContextProvider = identityContextProvider();
        MessageNotificationQueryApplicationService queryApplicationService =
                new MessageNotificationQueryApplicationService(
                        notificationRepository,
                        deliveryRecordRepository,
                        identityContextProvider
                );
        MessageNotificationActionApplicationService service = new MessageNotificationActionApplicationService(
                notificationRepository,
                actionRepository,
                identityContextProvider,
                queryApplicationService,
                publishedEvents::add,
                Clock.fixed(READ_AT, ZoneOffset.UTC)
        );

        Notification unreadNotification = unreadNotification();
        Notification readNotification = Notification.create(
                "notification-2",
                "todo.item.created:todo-2",
                "tenant-1",
                "assignment-1",
                "assignment-1",
                null,
                "Approve procurement request",
                "Todo item requires attention",
                "/portal/todo/todo-2",
                NotificationCategory.TODO_CREATED,
                NotificationPriority.NORMAL,
                "todo-center",
                "todo.item.created",
                "todo-2",
                CREATED_AT
        ).markRead(CREATED_AT.plusSeconds(60));
        notificationRepository.save(unreadNotification);
        notificationRepository.save(readNotification);
        deliveryRecordRepository.save(deliveredRecord("delivery-1", unreadNotification.notificationId()));
        deliveryRecordRepository.save(deliveredRecord("delivery-2", readNotification.notificationId()));

        NotificationBulkReadResult result = service.bulkMarkRead(List.of(
                unreadNotification.notificationId(),
                readNotification.notificationId(),
                "missing",
                unreadNotification.notificationId()
        ));

        assertThat(result.requestedCount()).isEqualTo(3);
        assertThat(result.processedCount()).isEqualTo(2);
        assertThat(result.readCount()).isEqualTo(1);
        assertThat(result.alreadyReadCount()).isEqualTo(1);
        assertThat(result.missingCount()).isEqualTo(1);
        assertThat(result.notifications()).extracting(NotificationSummary::notificationId)
                .containsExactly("notification-1", "notification-2");
        assertThat(result.missingNotificationIds()).containsExactly("missing");
        assertThat(actionRepository.findByNotificationId(unreadNotification.notificationId())).hasSize(1);
        assertThat(actionRepository.findByNotificationId(readNotification.notificationId())).isEmpty();
        assertThat(publishedEvents).singleElement().isInstanceOf(MsgNotificationReadEvent.class);
    }

    @Test
    void shouldRejectBulkReadWhenRequestExceedsLimit() {
        NotificationRepository notificationRepository = new InMemoryNotificationRepository();
        NotificationActionRepository actionRepository = new InMemoryNotificationActionRepository();
        NotificationDeliveryRecordRepository deliveryRecordRepository = new InMemoryNotificationDeliveryRecordRepository();
        MessageIdentityContextProvider identityContextProvider = identityContextProvider();
        MessageNotificationQueryApplicationService queryApplicationService =
                new MessageNotificationQueryApplicationService(
                        notificationRepository,
                        deliveryRecordRepository,
                        identityContextProvider
                );
        MessageNotificationActionApplicationService service = new MessageNotificationActionApplicationService(
                notificationRepository,
                actionRepository,
                identityContextProvider,
                queryApplicationService,
                event -> {
                },
                Clock.fixed(READ_AT, ZoneOffset.UTC)
        );

        List<String> request = IntStream.range(0, 101)
                .mapToObj(index -> "notification-" + index)
                .toList();

        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.bulkMarkRead(request)
        )).hasMessageContaining("max bulk size");
    }

    private MessageIdentityContextProvider identityContextProvider() {
        return () -> new MessageIdentityContext(
                "tenant-1",
                "assignment-1",
                "assignment-1",
                "position-1"
        );
    }

    private Notification unreadNotification() {
        return Notification.create(
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
                CREATED_AT
        );
    }

    private NotificationDeliveryRecord deliveredRecord(String deliveryId, String notificationId) {
        return NotificationDeliveryRecord.pending(
                        deliveryId,
                        notificationId,
                        NotificationDeliveryChannel.INBOX,
                        CREATED_AT
                )
                .markDelivered(CREATED_AT);
    }
}
