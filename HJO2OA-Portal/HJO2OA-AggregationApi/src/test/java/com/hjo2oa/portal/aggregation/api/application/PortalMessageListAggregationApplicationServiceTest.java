package com.hjo2oa.portal.aggregation.api.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.msg.message.center.application.MessageNotificationActionApplicationService;
import com.hjo2oa.msg.message.center.application.MessageNotificationQueryApplicationService;
import com.hjo2oa.msg.message.center.domain.MessageIdentityContext;
import com.hjo2oa.msg.message.center.domain.MessageIdentityContextProvider;
import com.hjo2oa.msg.message.center.domain.Notification;
import com.hjo2oa.msg.message.center.domain.NotificationActionRepository;
import com.hjo2oa.msg.message.center.domain.NotificationCategory;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryChannel;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecord;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecordRepository;
import com.hjo2oa.msg.message.center.domain.NotificationInboxStatus;
import com.hjo2oa.msg.message.center.domain.NotificationPriority;
import com.hjo2oa.msg.message.center.domain.NotificationRepository;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationActionRepository;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationDeliveryRecordRepository;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationRepository;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageListView;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PortalMessageListAggregationApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-19T10:00:00Z");

    @Test
    void shouldReturnPagedMessageListWithUnreadSummaryAndFilters() {
        MessageFixtures fixtures = buildMessageFixtures();
        PortalMessageListAggregationApplicationService service = new PortalMessageListAggregationApplicationService(
                fixtures.queryApplicationService(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalMessageListView view = service.officeCenterMessages(
                1,
                1,
                NotificationCategory.TODO_CREATED,
                NotificationInboxStatus.UNREAD,
                "travel"
        );

        assertThat(view.unreadSummary().totalUnreadCount()).isEqualTo(2);
        assertThat(view.unreadSummary().categoryUnreadCounts())
                .containsEntry("TODO_CREATED", 1L)
                .containsEntry("TODO_OVERDUE", 1L);
        assertThat(view.messages().pagination().page()).isEqualTo(1);
        assertThat(view.messages().pagination().size()).isEqualTo(1);
        assertThat(view.messages().pagination().total()).isEqualTo(1);
        assertThat(view.messages().pagination().totalPages()).isEqualTo(1);
        assertThat(view.messages().items())
                .extracting(item -> item.notificationId() + ":" + item.inboxStatus())
                .containsExactly("notification-2:UNREAD");
    }

    @Test
    void shouldReturnEmptyMessagePageWhenFiltersDoNotMatch() {
        MessageFixtures fixtures = buildMessageFixtures();
        PortalMessageListAggregationApplicationService service = new PortalMessageListAggregationApplicationService(
                fixtures.queryApplicationService(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalMessageListView view = service.officeCenterMessages(
                1,
                20,
                NotificationCategory.TODO_OVERDUE,
                NotificationInboxStatus.READ,
                "missing"
        );

        assertThat(view.unreadSummary().totalUnreadCount()).isEqualTo(2);
        assertThat(view.messages().items()).isEmpty();
        assertThat(view.messages().pagination().total()).isEqualTo(0);
        assertThat(view.messages().pagination().totalPages()).isEqualTo(0);
    }

    @Test
    void shouldRefreshUnreadSummaryAndStatusesAfterReadActions() {
        MessageFixtures fixtures = buildMessageFixtures();
        PortalMessageListAggregationApplicationService service = new PortalMessageListAggregationApplicationService(
                fixtures.queryApplicationService(),
                Clock.fixed(FIXED_TIME.plusSeconds(300), ZoneOffset.UTC)
        );

        PortalMessageListView beforeRead = service.officeCenterMessages(1, 20, null, null, null);
        fixtures.actionApplicationService().markRead("notification-1");
        fixtures.actionApplicationService().bulkMarkRead(List.of("notification-2"));
        PortalMessageListView afterRead = service.officeCenterMessages(1, 20, null, null, null);

        assertThat(beforeRead.unreadSummary().totalUnreadCount()).isEqualTo(2);
        assertThat(afterRead.unreadSummary().totalUnreadCount()).isEqualTo(0);
        assertThat(afterRead.messages().items())
                .extracting(item -> item.notificationId() + ":" + item.inboxStatus())
                .containsExactly(
                        "notification-1:READ",
                        "notification-2:READ",
                        "notification-3:READ"
                );
    }

    private MessageFixtures buildMessageFixtures() {
        NotificationRepository notificationRepository = new InMemoryNotificationRepository();
        NotificationActionRepository actionRepository = new InMemoryNotificationActionRepository();
        NotificationDeliveryRecordRepository deliveryRecordRepository = new InMemoryNotificationDeliveryRecordRepository();
        MessageIdentityContextProvider identityContextProvider = () -> new MessageIdentityContext(
                "tenant-1",
                "assignment-1",
                "assignment-1",
                "position-1"
        );
        MessageNotificationQueryApplicationService queryApplicationService =
                new MessageNotificationQueryApplicationService(
                        notificationRepository,
                        deliveryRecordRepository,
                        identityContextProvider
                );
        MessageNotificationActionApplicationService actionApplicationService =
                new MessageNotificationActionApplicationService(
                        notificationRepository,
                        actionRepository,
                        identityContextProvider,
                        queryApplicationService,
                        event -> {
                        },
                        Clock.fixed(FIXED_TIME.plusSeconds(120), ZoneOffset.UTC)
                );

        Notification newestUnread = Notification.create(
                "notification-1",
                "todo.item.overdue:todo-1",
                "tenant-1",
                "assignment-1",
                "assignment-1",
                null,
                "Overdue: Approve travel request",
                "Todo item is overdue",
                "/portal/todo/todo-1",
                NotificationCategory.TODO_OVERDUE,
                NotificationPriority.CRITICAL,
                "todo-center",
                "todo.item.overdue",
                "todo-1",
                FIXED_TIME.minusSeconds(30)
        );
        Notification unreadCreated = Notification.create(
                "notification-2",
                "todo.item.created:todo-2",
                "tenant-1",
                "assignment-1",
                "assignment-1",
                null,
                "Approve travel reimbursement",
                "Todo item requires attention",
                "/portal/todo/todo-2",
                NotificationCategory.TODO_CREATED,
                NotificationPriority.NORMAL,
                "todo-center",
                "todo.item.created",
                "todo-2",
                FIXED_TIME.minusSeconds(60)
        );
        Notification readCreated = Notification.create(
                "notification-3",
                "todo.item.created:todo-3",
                "tenant-1",
                "assignment-1",
                "assignment-1",
                null,
                "Filed travel request",
                "Todo item already reviewed",
                "/portal/todo/todo-3",
                NotificationCategory.TODO_CREATED,
                NotificationPriority.NORMAL,
                "todo-center",
                "todo.item.created",
                "todo-3",
                FIXED_TIME.minusSeconds(90)
        ).markRead(FIXED_TIME.minusSeconds(10));

        notificationRepository.save(newestUnread);
        notificationRepository.save(unreadCreated);
        notificationRepository.save(readCreated);
        saveDeliveryRecord(deliveryRecordRepository, newestUnread, "delivery-1");
        saveDeliveryRecord(deliveryRecordRepository, unreadCreated, "delivery-2");
        saveDeliveryRecord(deliveryRecordRepository, readCreated, "delivery-3");

        return new MessageFixtures(queryApplicationService, actionApplicationService);
    }

    private void saveDeliveryRecord(
            NotificationDeliveryRecordRepository deliveryRecordRepository,
            Notification notification,
            String deliveryId
    ) {
        deliveryRecordRepository.save(NotificationDeliveryRecord.pending(
                        deliveryId,
                        notification.notificationId(),
                        NotificationDeliveryChannel.INBOX,
                        notification.createdAt()
                )
                .markDelivered(notification.createdAt()));
    }

    private record MessageFixtures(
            MessageNotificationQueryApplicationService queryApplicationService,
            MessageNotificationActionApplicationService actionApplicationService
    ) {
    }
}
