package com.hjo2oa.portal.aggregation.api.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.msg.message.center.application.MessageNotificationQueryApplicationService;
import com.hjo2oa.msg.message.center.domain.MessageIdentityContext;
import com.hjo2oa.msg.message.center.domain.MessageIdentityContextProvider;
import com.hjo2oa.msg.message.center.domain.Notification;
import com.hjo2oa.msg.message.center.domain.NotificationCategory;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryChannel;
import com.hjo2oa.msg.message.center.domain.NotificationDeliveryRecord;
import com.hjo2oa.msg.message.center.domain.NotificationPriority;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationDeliveryRecordRepository;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationRepository;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardState;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalOfficeCenterView;
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.portal.aggregation.api.infrastructure.InMemoryPortalCardSnapshotRepository;
import com.hjo2oa.todo.center.application.TodoQueryApplicationService;
import com.hjo2oa.todo.center.domain.TodoIdentityContext;
import com.hjo2oa.todo.center.domain.TodoIdentityContextProvider;
import com.hjo2oa.todo.center.domain.TodoItem;
import com.hjo2oa.todo.center.domain.TodoItemStatus;
import com.hjo2oa.todo.center.infrastructure.InMemoryCopiedTodoRepository;
import com.hjo2oa.todo.center.infrastructure.InMemoryTodoItemRepository;
import com.hjo2oa.todo.center.infrastructure.InMemoryTodoProcessViewRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PortalOfficeCenterAggregationApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-19T10:00:00Z");

    @Test
    void shouldAggregateOfficeCenterNavigationAndWorkspaceSnapshots() {
        TodoQueryApplicationService todoQueryApplicationService = todoQueryApplicationService();
        MessageNotificationQueryApplicationService messageQueryApplicationService = messageQueryApplicationService();
        PortalDashboardAggregationApplicationService dashboardAggregationApplicationService =
                new PortalDashboardAggregationApplicationService(
                        () -> new PortalIdentityCard(
                                "tenant-1",
                                "person-1",
                                "account-1",
                                "assignment-1",
                                "position-1",
                                "organization-1",
                                "department-1",
                                "Chief Clerk",
                                "Head Office",
                                "General Office",
                                "PRIMARY",
                                FIXED_TIME
                        ),
                        () -> new com.hjo2oa.portal.aggregation.api.infrastructure.TodoCenterPortalTodoCardDataProvider(todoQueryApplicationService).currentTodoCard(),
                        () -> new com.hjo2oa.portal.aggregation.api.infrastructure.MessageCenterPortalMessageCardDataProvider(messageQueryApplicationService).currentMessageCard(),
                        new InMemoryPortalCardSnapshotRepository(),
                        event -> {
                        },
                        Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
                );
        PortalOfficeCenterAggregationApplicationService service =
                new PortalOfficeCenterAggregationApplicationService(
                        dashboardAggregationApplicationService,
                        todoQueryApplicationService,
                        messageQueryApplicationService,
                        Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
                );

        PortalOfficeCenterView officeCenter = service.officeCenter();

        assertThat(officeCenter.sceneType()).isEqualTo(PortalSceneType.OFFICE_CENTER);
        assertThat(officeCenter.navigation())
                .extracting(nav -> nav.code() + ":" + nav.badgeCount())
                .containsExactly("pending:2", "completed:1", "overdue:1", "messages:1");
        assertThat(officeCenter.identity().state()).isEqualTo(PortalCardState.READY);
        assertThat(officeCenter.todo().state()).isEqualTo(PortalCardState.READY);
        assertThat(officeCenter.message().state()).isEqualTo(PortalCardState.READY);
        assertThat(officeCenter.todo().data().totalCount()).isEqualTo(2);
        assertThat(officeCenter.message().data().unreadCount()).isEqualTo(1);
    }

    private TodoQueryApplicationService todoQueryApplicationService() {
        InMemoryTodoItemRepository repository = new InMemoryTodoItemRepository();
        repository.save(new TodoItem(
                "todo-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "APPROVAL",
                "approval",
                "Approve travel request",
                "HIGH",
                TodoItemStatus.PENDING,
                FIXED_TIME.plusSeconds(600),
                FIXED_TIME.minusSeconds(60),
                FIXED_TIME.minusSeconds(300),
                FIXED_TIME.minusSeconds(60),
                null,
                null
        ));
        repository.save(new TodoItem(
                "todo-2",
                "task-2",
                "instance-2",
                "assignment-1",
                "APPROVAL",
                "approval",
                "Review contract",
                "NORMAL",
                TodoItemStatus.PENDING,
                FIXED_TIME.plusSeconds(1200),
                null,
                FIXED_TIME.minusSeconds(600),
                FIXED_TIME.minusSeconds(120),
                null,
                null
        ));
        repository.save(new TodoItem(
                "todo-3",
                "task-3",
                "instance-3",
                "assignment-1",
                "APPROVAL",
                "approval",
                "Completed request",
                "LOW",
                TodoItemStatus.COMPLETED,
                null,
                null,
                FIXED_TIME.minusSeconds(900),
                FIXED_TIME.minusSeconds(180),
                FIXED_TIME.minusSeconds(180),
                null
        ));

        TodoIdentityContextProvider identityContextProvider = () -> new TodoIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        );
        return new TodoQueryApplicationService(
                repository,
                new InMemoryCopiedTodoRepository(),
                new InMemoryTodoProcessViewRepository(),
                identityContextProvider
        );
    }

    private MessageNotificationQueryApplicationService messageQueryApplicationService() {
        InMemoryNotificationRepository notificationRepository = new InMemoryNotificationRepository();
        InMemoryNotificationDeliveryRecordRepository deliveryRecordRepository = new InMemoryNotificationDeliveryRecordRepository();
        Notification notification = Notification.create(
                "notification-1",
                "todo.item.created:todo-1",
                "tenant-1",
                "assignment-1",
                "assignment-1",
                null,
                "Approve travel request",
                "Todo item requires attention",
                "/portal/todo/todo-1",
                NotificationCategory.TODO_CREATED,
                NotificationPriority.NORMAL,
                "todo-center",
                "todo.item.created",
                "todo-1",
                FIXED_TIME.minusSeconds(30)
        );
        notificationRepository.save(notification);
        deliveryRecordRepository.save(NotificationDeliveryRecord.pending(
                        "delivery-1",
                        notification.notificationId(),
                        NotificationDeliveryChannel.INBOX,
                        FIXED_TIME.minusSeconds(30)
                )
                .markDelivered(FIXED_TIME.minusSeconds(30)));

        MessageIdentityContextProvider identityContextProvider = () -> new MessageIdentityContext(
                "tenant-1",
                "assignment-1",
                "assignment-1",
                "position-1"
        );
        return new MessageNotificationQueryApplicationService(
                notificationRepository,
                deliveryRecordRepository,
                identityContextProvider
        );
    }
}
