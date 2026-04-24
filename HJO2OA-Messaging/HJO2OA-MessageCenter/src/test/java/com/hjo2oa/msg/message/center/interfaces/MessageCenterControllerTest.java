package com.hjo2oa.msg.message.center.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.hjo2oa.msg.message.center.domain.NotificationPriority;
import com.hjo2oa.msg.message.center.domain.NotificationRepository;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationActionRepository;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationDeliveryRecordRepository;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MessageCenterControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-04-19T16:00:00Z");
    private static final Instant READ_AT = Instant.parse("2026-04-19T16:05:00Z");

    @Test
    void shouldMarkMessageAsRead() throws Exception {
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
                        Clock.fixed(READ_AT, ZoneOffset.UTC)
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
                CREATED_AT
        );
        notificationRepository.save(notification);
        deliveryRecordRepository.save(NotificationDeliveryRecord.pending(
                        "delivery-1",
                        notification.notificationId(),
                        NotificationDeliveryChannel.INBOX,
                        CREATED_AT
                )
                .markDelivered(CREATED_AT));

        MockMvc mockMvc = mockMvc(queryApplicationService, actionApplicationService);

        mockMvc.perform(post("/api/v1/msg/messages/notification-1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.notificationId").value("notification-1"))
                .andExpect(jsonPath("$.data.inboxStatus").value("READ"));
    }

    @Test
    void shouldBulkMarkMessagesAsRead() throws Exception {
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
                        Clock.fixed(READ_AT, ZoneOffset.UTC)
                );

        Notification unreadNotification = Notification.create(
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
        deliveryRecordRepository.save(NotificationDeliveryRecord.pending(
                        "delivery-1",
                        unreadNotification.notificationId(),
                        NotificationDeliveryChannel.INBOX,
                        CREATED_AT
                )
                .markDelivered(CREATED_AT));
        deliveryRecordRepository.save(NotificationDeliveryRecord.pending(
                        "delivery-2",
                        readNotification.notificationId(),
                        NotificationDeliveryChannel.INBOX,
                        CREATED_AT
                )
                .markDelivered(CREATED_AT));

        MockMvc mockMvc = mockMvc(queryApplicationService, actionApplicationService);

        mockMvc.perform(post("/api/v1/msg/messages/bulk-read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"notificationIds":["notification-1","notification-2","missing","notification-1"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.requestedCount").value(3))
                .andExpect(jsonPath("$.data.readCount").value(1))
                .andExpect(jsonPath("$.data.alreadyReadCount").value(1))
                .andExpect(jsonPath("$.data.missingCount").value(1))
                .andExpect(jsonPath("$.data.missingNotificationIds[0]").value("missing"));
    }

    @Test
    void shouldReturnMessageDetail() throws Exception {
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
                        Clock.fixed(READ_AT, ZoneOffset.UTC)
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
                CREATED_AT
        );
        notificationRepository.save(notification.markRead(READ_AT));
        deliveryRecordRepository.save(NotificationDeliveryRecord.pending(
                        "delivery-1",
                        notification.notificationId(),
                        NotificationDeliveryChannel.INBOX,
                        CREATED_AT
                )
                .markDelivered(CREATED_AT));

        MockMvc mockMvc = mockMvc(queryApplicationService, actionApplicationService);

        mockMvc.perform(get("/api/v1/msg/messages/notification-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.notificationId").value("notification-1"))
                .andExpect(jsonPath("$.data.sourceEventType").value("todo.item.created"))
                .andExpect(jsonPath("$.data.sourceBusinessId").value("todo-1"))
                .andExpect(jsonPath("$.data.readAt").exists());
    }

    @Test
    void shouldReturnNotFoundWhenMessageDoesNotExist() throws Exception {
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
                        Clock.fixed(READ_AT, ZoneOffset.UTC)
                );

        MockMvc mockMvc = mockMvc(queryApplicationService, actionApplicationService);

        mockMvc.perform(post("/api/v1/msg/messages/missing/read"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/msg/messages/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldRejectOversizedBulkReadRequest() throws Exception {
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
                        Clock.fixed(READ_AT, ZoneOffset.UTC)
                );
        String payload = IntStream.range(0, 101)
                .mapToObj(index -> "\"notification-" + index + "\"")
                .collect(Collectors.joining(",", "{\"notificationIds\":[", "]}"));

        MockMvc mockMvc = mockMvc(queryApplicationService, actionApplicationService);

        mockMvc.perform(post("/api/v1/msg/messages/bulk-read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private static MockMvc mockMvc(
            MessageNotificationQueryApplicationService queryApplicationService,
            MessageNotificationActionApplicationService actionApplicationService
    ) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(new MessageCenterController(
                        queryApplicationService,
                        actionApplicationService,
                        responseMetaFactory
                ))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }
}
