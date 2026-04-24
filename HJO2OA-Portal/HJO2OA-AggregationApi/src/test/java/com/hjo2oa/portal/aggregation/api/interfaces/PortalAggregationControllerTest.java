package com.hjo2oa.portal.aggregation.api.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.aggregation.api.application.PortalDashboardAggregationApplicationService;
import com.hjo2oa.portal.aggregation.api.application.PortalMessageListAggregationApplicationService;
import com.hjo2oa.portal.aggregation.api.application.PortalOfficeCenterAggregationApplicationService;
import com.hjo2oa.portal.aggregation.api.application.PortalTodoListAggregationApplicationService;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageItem;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoItem;
import com.hjo2oa.portal.aggregation.api.infrastructure.InMemoryPortalCardSnapshotRepository;
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
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import com.hjo2oa.todo.center.application.TodoQueryApplicationService;
import com.hjo2oa.todo.center.domain.CopiedTodoItem;
import com.hjo2oa.todo.center.domain.TodoIdentityContext;
import com.hjo2oa.todo.center.domain.TodoIdentityContextProvider;
import com.hjo2oa.todo.center.domain.TodoItem;
import com.hjo2oa.todo.center.domain.TodoItemStatus;
import com.hjo2oa.todo.center.infrastructure.InMemoryCopiedTodoRepository;
import com.hjo2oa.todo.center.infrastructure.InMemoryTodoItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PortalAggregationControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-19T10:00:00Z");

    @Test
    void shouldReturnDashboardUsingSharedResponseContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(get("/api/v1/portal/aggregation/dashboard")
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-portal-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.identity.data.personId").value("person-1"))
                .andExpect(jsonPath("$.data.todo.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.message.data.unreadCount").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("req-portal-1"));
    }

    @Test
    void shouldReturnOfficeCenterUsingSharedResponseContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(get("/api/v1/portal/aggregation/office-center")
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-office-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.sceneType").value("OFFICE_CENTER"))
                .andExpect(jsonPath("$.data.navigation[0].code").value("pending"))
                .andExpect(jsonPath("$.data.navigation[0].badgeCount").value(2))
                .andExpect(jsonPath("$.data.navigation[1].badgeCount").value(1))
                .andExpect(jsonPath("$.data.navigation[2].badgeCount").value(1))
                .andExpect(jsonPath("$.data.navigation[3].badgeCount").value(1))
                .andExpect(jsonPath("$.data.navigation[3].actionLink").value("/api/v1/portal/aggregation/office-center/messages"))
                .andExpect(jsonPath("$.data.todo.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.message.data.unreadCount").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("req-office-1"));
    }

    @Test
    void shouldReturnOfficeCenterMessageListUsingSharedResponseContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(get("/api/v1/portal/aggregation/office-center/messages")
                        .param("page", "1")
                        .param("size", "1")
                        .param("readStatus", "UNREAD")
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-office-msg-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.unreadSummary.totalUnreadCount").value(1))
                .andExpect(jsonPath("$.data.messages.pagination.page").value(1))
                .andExpect(jsonPath("$.data.messages.pagination.size").value(1))
                .andExpect(jsonPath("$.data.messages.pagination.total").value(1))
                .andExpect(jsonPath("$.data.messages.items[0].notificationId").value("notification-1"))
                .andExpect(jsonPath("$.data.messages.items[0].inboxStatus").value("UNREAD"))
                .andExpect(jsonPath("$.meta.requestId").value("req-office-msg-1"));
    }

    @Test
    void shouldReturnOfficeCenterTodoListUsingSharedResponseContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(get("/api/v1/portal/aggregation/office-center/todos")
                        .param("viewType", "COPIED")
                        .param("copiedReadStatus", "UNREAD")
                        .param("page", "1")
                        .param("size", "1")
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-office-todo-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.viewType").value("COPIED"))
                .andExpect(jsonPath("$.data.summary.copiedUnreadCount").value(1))
                .andExpect(jsonPath("$.data.todos.pagination.total").value(1))
                .andExpect(jsonPath("$.data.todos.items[0].todoId").value("copied-1"))
                .andExpect(jsonPath("$.data.todos.items[0].status").value("UNREAD"))
                .andExpect(jsonPath("$.meta.requestId").value("req-office-todo-1"));
    }

    @Test
    void shouldMapInvalidTodoListPageSizeToBadRequest() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(get("/api/v1/portal/aggregation/office-center/todos")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldMapInvalidMessageListPageSizeToBadRequest() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(get("/api/v1/portal/aggregation/office-center/messages")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldMapInvalidCardTypeToBadRequest() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(get("/api/v1/portal/aggregation/card/UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldRefreshSingleCardUsingRefreshEndpoint() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(get("/api/v1/portal/aggregation/card/TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.state").value("READY"))
                .andExpect(jsonPath("$.data.cardType").value("TODO"));
    }

    private MockMvc buildMockMvc() {
        TodoQueryApplicationService todoQueryApplicationService = todoQueryApplicationService();
        MessageNotificationQueryApplicationService messageQueryApplicationService = messageQueryApplicationService();
        PortalDashboardAggregationApplicationService service = new PortalDashboardAggregationApplicationService(
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
                () -> new PortalTodoCard(
                        2,
                        1,
                        Map.of("approval", 2L),
                        List.of(
                                new PortalTodoItem(
                                        "todo-1",
                                        "Approve travel request",
                                        "approval",
                                        "HIGH",
                                        FIXED_TIME.plusSeconds(600),
                                        FIXED_TIME.minusSeconds(60)
                                ),
                                new PortalTodoItem(
                                        "todo-2",
                                        "Review contract",
                                        "approval",
                                        "NORMAL",
                                        FIXED_TIME.plusSeconds(1200),
                                        FIXED_TIME.minusSeconds(120)
                                )
                        )
                ),
                () -> new PortalMessageCard(
                        1,
                        Map.of("TODO_CREATED", 1L),
                        List.of(new PortalMessageItem(
                                "notification-1",
                                "Approve travel request",
                                "TODO_CREATED",
                                "HIGH",
                                "/portal/todo/todo-1",
                                FIXED_TIME.minusSeconds(30)
                        ))
                ),
                new InMemoryPortalCardSnapshotRepository(),
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        PortalOfficeCenterAggregationApplicationService officeCenterAggregationApplicationService =
                new PortalOfficeCenterAggregationApplicationService(
                        service,
                        todoQueryApplicationService,
                        messageQueryApplicationService,
                        Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
                );
        PortalMessageListAggregationApplicationService messageListAggregationApplicationService =
                new PortalMessageListAggregationApplicationService(
                        messageQueryApplicationService,
                        Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
                );
        PortalTodoListAggregationApplicationService todoListAggregationApplicationService =
                new PortalTodoListAggregationApplicationService(
                        todoQueryApplicationService,
                        Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
                );
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();

        return MockMvcBuilders.standaloneSetup(new PortalAggregationController(
                        service,
                        officeCenterAggregationApplicationService,
                        messageListAggregationApplicationService,
                        todoListAggregationApplicationService,
                        responseMetaFactory
                ))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
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
        InMemoryCopiedTodoRepository copiedTodoRepository = new InMemoryCopiedTodoRepository();
        copiedTodoRepository.save(CopiedTodoItem.unread(
                "copied-1",
                "task-4",
                "instance-4",
                "assignment-1",
                "APPROVAL",
                "approval",
                "Copied travel notice",
                "HIGH",
                FIXED_TIME.minusSeconds(90)
        ));
        copiedTodoRepository.save(CopiedTodoItem.unread(
                "copied-2",
                "task-5",
                "instance-5",
                "assignment-1",
                "APPROVAL",
                "approval",
                "Read copied notice",
                "NORMAL",
                FIXED_TIME.minusSeconds(120)
        ).markRead(FIXED_TIME.minusSeconds(30)));

        TodoIdentityContextProvider identityContextProvider = () -> new TodoIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        );
        return new TodoQueryApplicationService(
                repository,
                copiedTodoRepository,
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
        Notification readNotification = Notification.create(
                "notification-2",
                "todo.item.created:todo-2",
                "tenant-1",
                "assignment-1",
                "assignment-1",
                null,
                "Filed request",
                "Todo item already reviewed",
                "/portal/todo/todo-2",
                NotificationCategory.TODO_CREATED,
                NotificationPriority.NORMAL,
                "todo-center",
                "todo.item.created",
                "todo-2",
                FIXED_TIME.minusSeconds(60)
        ).markRead(FIXED_TIME.minusSeconds(20));
        notificationRepository.save(notification);
        notificationRepository.save(readNotification);
        deliveryRecordRepository.save(NotificationDeliveryRecord.pending(
                        "delivery-1",
                        notification.notificationId(),
                        NotificationDeliveryChannel.INBOX,
                        FIXED_TIME.minusSeconds(30)
                )
                .markDelivered(FIXED_TIME.minusSeconds(30)));
        deliveryRecordRepository.save(NotificationDeliveryRecord.pending(
                        "delivery-2",
                        readNotification.notificationId(),
                        NotificationDeliveryChannel.INBOX,
                        FIXED_TIME.minusSeconds(60)
                )
                .markDelivered(FIXED_TIME.minusSeconds(60)));

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
