package com.hjo2oa.msg.event.subscription.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.msg.channel.sender.application.ChannelSenderApplicationService;
import com.hjo2oa.msg.channel.sender.application.ChannelSenderCommands;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import com.hjo2oa.msg.channel.sender.domain.MessageCategory;
import com.hjo2oa.msg.channel.sender.domain.MessagePriority;
import com.hjo2oa.msg.channel.sender.infrastructure.InMemoryChannelSenderRepository;
import com.hjo2oa.msg.event.subscription.application.EventSubscriptionApplicationService;
import com.hjo2oa.msg.event.subscription.application.EventSubscriptionCommands;
import com.hjo2oa.msg.event.subscription.domain.DigestMode;
import com.hjo2oa.msg.event.subscription.domain.NotificationCategory;
import com.hjo2oa.msg.event.subscription.domain.NotificationPriority;
import com.hjo2oa.msg.event.subscription.domain.TargetResolverType;
import com.hjo2oa.msg.event.subscription.infrastructure.InMemoryEventSubscriptionRepository;
import com.hjo2oa.msg.event.subscription.infrastructure.InMemorySubscriptionExecutionLogRepository;
import com.hjo2oa.msg.message.center.application.MessageNotificationCommandApplicationService;
import com.hjo2oa.msg.message.center.application.MessageNotificationQueryApplicationService;
import com.hjo2oa.msg.message.center.domain.MessageIdentityContext;
import com.hjo2oa.msg.message.center.domain.NotificationRepository;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationDeliveryRecordRepository;
import com.hjo2oa.msg.message.center.infrastructure.InMemoryNotificationRepository;
import com.hjo2oa.msg.message.center.infrastructure.InboxNotificationChannelDispatcher;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainEventSubscriptionListenerTest {

    private static final Instant NOW = Instant.parse("2026-04-29T02:00:00Z");
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PERSON_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void shouldRenderTemplateAndCreateMessageFromDomainEvent() {
        EventSubscriptionApplicationService subscriptionService = new EventSubscriptionApplicationService(
                new InMemoryEventSubscriptionRepository(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        subscriptionService.createRule(new EventSubscriptionCommands.SaveRuleCommand(
                "todo-created",
                "todo.item.created",
                NotificationCategory.TODO_CREATED,
                TargetResolverType.PAYLOAD_PERSON,
                "{\"personField\":\"recipientPersonId\"}",
                "todo-created",
                null,
                null,
                NotificationPriority.NORMAL,
                true,
                TENANT_ID
        ));
        subscriptionService.savePreference(new EventSubscriptionCommands.SavePreferenceCommand(
                PERSON_ID,
                NotificationCategory.TODO_CREATED,
                List.of(com.hjo2oa.msg.event.subscription.domain.ChannelType.INBOX),
                null,
                DigestMode.IMMEDIATE,
                true,
                false,
                true,
                TENANT_ID
        ));
        ChannelSenderApplicationService channelSenderService = new ChannelSenderApplicationService(
                new InMemoryChannelSenderRepository(),
                List.of(),
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        var template = channelSenderService.createTemplate(new ChannelSenderCommands.CreateTemplateCommand(
                "todo-created",
                ChannelType.INBOX,
                "zh-CN",
                null,
                MessageCategory.TODO_CREATED,
                "待办 {{title}}",
                "请处理 {{title}}",
                null,
                false,
                TENANT_ID
        ));
        channelSenderService.publishTemplate(template.id());
        NotificationRepository notificationRepository = new InMemoryNotificationRepository();
        InMemoryNotificationDeliveryRecordRepository deliveryRepository =
                new InMemoryNotificationDeliveryRecordRepository();
        MessageNotificationQueryApplicationService queryService = new MessageNotificationQueryApplicationService(
                notificationRepository,
                deliveryRepository,
                () -> new MessageIdentityContext(TENANT_ID.toString(), PERSON_ID.toString(), PERSON_ID.toString(), "pos-1")
        );
        MessageNotificationCommandApplicationService messageService = new MessageNotificationCommandApplicationService(
                notificationRepository,
                deliveryRepository,
                List.of(new InboxNotificationChannelDispatcher()),
                event -> {
                },
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        InMemorySubscriptionExecutionLogRepository executionLogRepository =
                new InMemorySubscriptionExecutionLogRepository();
        DomainEventSubscriptionListener listener = new DomainEventSubscriptionListener(
                subscriptionService,
                messageService,
                channelSenderService,
                new ObjectMapper(),
                executionLogRepository
        );
        UUID eventId = UUID.randomUUID();

        listener.onDomainEvent(new GenericTestEvent(
                eventId,
                "todo.item.created",
                NOW,
                TENANT_ID.toString(),
                Map.of(
                        "recipientPersonId", PERSON_ID.toString(),
                        "title", "费用审批",
                        "todoId", "todo-1",
                        "deepLink", "/todo/todo-1"
                )
        ));

        assertThat(notificationRepository.findAll()).singleElement().satisfies(notification -> {
            assertThat(notification.title()).isEqualTo("待办 费用审批");
            assertThat(notification.bodySummary()).isEqualTo("请处理 费用审批");
            assertThat(notification.recipientId()).isEqualTo(PERSON_ID.toString());
        });
        assertThat(queryService.unreadSummary().totalUnreadCount()).isEqualTo(1);
        assertThat(executionLogRepository.findByEventId(eventId))
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.ruleCode()).isEqualTo("todo-created");
                    assertThat(log.recipientId()).isEqualTo(PERSON_ID.toString());
                    assertThat(log.result()).isEqualTo("CREATED");
                });
    }

    private record GenericTestEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            Map<String, Object> payload
    ) implements DomainEvent {
    }
}
