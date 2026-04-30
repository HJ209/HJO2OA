package com.hjo2oa.msg.event.subscription.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.msg.channel.sender.application.ChannelSenderApplicationService;
import com.hjo2oa.msg.channel.sender.application.ChannelSenderCommands.RenderTemplateCommand;
import com.hjo2oa.msg.channel.sender.application.ChannelSenderCommands.DispatchNotificationCommand;
import com.hjo2oa.msg.channel.sender.application.RenderedMessageView;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import com.hjo2oa.msg.channel.sender.domain.MessageCategory;
import com.hjo2oa.msg.channel.sender.domain.MessagePriority;
import com.hjo2oa.msg.event.subscription.application.EventSubscriptionApplicationService;
import com.hjo2oa.msg.event.subscription.domain.EventMatchView;
import com.hjo2oa.msg.event.subscription.domain.SubscriptionExecutionLog;
import com.hjo2oa.msg.event.subscription.domain.SubscriptionExecutionLogRepository;
import com.hjo2oa.msg.event.subscription.domain.TargetResolverType;
import com.hjo2oa.msg.event.subscription.infrastructure.InMemorySubscriptionExecutionLogRepository;
import com.hjo2oa.msg.message.center.application.MessageNotificationCommandApplicationService;
import com.hjo2oa.msg.message.center.application.MessageNotificationCommands;
import com.hjo2oa.msg.message.center.application.MessageNotificationCreateResult;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DomainEventSubscriptionListener {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final EventSubscriptionApplicationService subscriptionService;
    private final MessageNotificationCommandApplicationService messageService;
    private final ChannelSenderApplicationService channelSenderService;
    private final SubscriptionExecutionLogRepository executionLogRepository;
    private final ObjectMapper objectMapper;

    public DomainEventSubscriptionListener(
            EventSubscriptionApplicationService subscriptionService,
            MessageNotificationCommandApplicationService messageService,
            ChannelSenderApplicationService channelSenderService,
            ObjectMapper objectMapper
    ) {
        this(
                subscriptionService,
                messageService,
                channelSenderService,
                objectMapper,
                new InMemorySubscriptionExecutionLogRepository()
        );
    }

    @Autowired
    public DomainEventSubscriptionListener(
            EventSubscriptionApplicationService subscriptionService,
            MessageNotificationCommandApplicationService messageService,
            ChannelSenderApplicationService channelSenderService,
            ObjectMapper objectMapper,
            SubscriptionExecutionLogRepository executionLogRepository
    ) {
        this.subscriptionService = subscriptionService;
        this.messageService = messageService;
        this.channelSenderService = channelSenderService;
        this.executionLogRepository = executionLogRepository;
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void onDomainEvent(DomainEvent event) {
        if (event == null || event.eventType() == null || event.eventType().startsWith("msg.")) {
            return;
        }
        Map<String, Object> payload = payloadOf(event);
        UUID tenantId = toUuid(event.tenantId());
        UUID recipientPersonId = toNullableUuid(firstText(payload, "recipientPersonId", "personId", "assigneeId"));
        List<EventMatchView> matches = subscriptionService.matchEvent(
                event.eventType(),
                tenantId,
                recipientPersonId,
                null,
                LocalTime.ofInstant(event.occurredAt(), java.time.ZoneOffset.UTC)
        );
        for (EventMatchView match : matches) {
            try {
                consumeMatch(event, payload, match, tenantId);
            } catch (RuntimeException ex) {
                recordExecution(event, match, tenantId, null, "FAILED", ex.getMessage());
                throw ex;
            }
        }
    }

    private void consumeMatch(
            DomainEvent event,
            Map<String, Object> payload,
            EventMatchView match,
            UUID tenantId
    ) {
        String recipientId = resolveRecipientId(payload, match);
        if (match.quietNow()) {
            recordExecution(event, match, tenantId, recipientId, "SKIPPED_QUIET", "recipient is in quiet window");
            return;
        }
        if (match.allowedChannels().isEmpty()) {
            recordExecution(event, match, tenantId, recipientId, "SKIPPED_NO_CHANNEL", "no allowed channel");
            return;
        }
        if (recipientId == null) {
            recordExecution(event, match, tenantId, null, "SKIPPED_NO_RECIPIENT", "recipient cannot be resolved");
            return;
        }
        RenderedMessageView rendered = channelSenderService.renderTemplate(new RenderTemplateCommand(
                tenantId,
                match.templateCode(),
                ChannelType.INBOX,
                stringValue(payload.getOrDefault("locale", "zh-CN")),
                variables(event, payload)
        ));
        String businessId = firstText(payload, "businessId", "todoId", "taskId", "instanceId", "id");
        if (businessId == null) {
            businessId = event.eventId().toString();
        }
        MessageNotificationCreateResult result = messageService.createNotificationResult(
                new MessageNotificationCommands.CreateNotificationCommand(
                        event.eventId() + ":" + match.ruleCode() + ":" + recipientId,
                        event.tenantId(),
                        recipientId,
                        firstText(payload, "assignmentId", "assigneeId", "targetAssignmentId"),
                        firstText(payload, "positionId", "targetPositionId"),
                        rendered.title(),
                        rendered.body(),
                        firstText(payload, "deepLink", "url", "link", "href") == null
                                ? "/portal/events/" + businessId
                                : firstText(payload, "deepLink", "url", "link", "href"),
                        mapCategory(match.notificationCategory()),
                        mapPriority(match.priority()),
                        "event-subscription",
                        event.eventType(),
                        businessId,
                        event.occurredAt()
                )
        );
        recordExecution(
                event,
                match,
                tenantId,
                recipientId,
                result.created() ? "CREATED" : "DUPLICATE",
                result.notification().notificationId()
        );
        if (result.created()) {
            dispatchAllowedChannels(result.notification().notificationId(), tenantId, recipientId, rendered, match);
        }
    }

    private void recordExecution(
            DomainEvent event,
            EventMatchView match,
            UUID tenantId,
            String recipientId,
            String result,
            String message
    ) {
        try {
            executionLogRepository.saveIfAbsent(new SubscriptionExecutionLog(
                    UUID.randomUUID(),
                    event.eventId(),
                    event.eventType(),
                    match.ruleCode(),
                    recipientId,
                    result,
                    message == null || message.length() <= 512 ? message : message.substring(0, 512),
                    tenantId,
                    event.occurredAt()
            ));
        } catch (RuntimeException ignored) {
            // Event delivery must not be blocked by execution log persistence.
        }
    }

    private void dispatchAllowedChannels(
            String notificationId,
            UUID tenantId,
            String recipientId,
            RenderedMessageView rendered,
            EventMatchView match
    ) {
        List<ChannelType> channels = match.allowedChannels().stream()
                .map(this::mapChannel)
                .filter(channel -> channel != ChannelType.INBOX)
                .toList();
        if (channels.isEmpty()) {
            return;
        }
        channelSenderService.dispatchNotification(new DispatchNotificationCommand(
                toUuid(notificationId),
                tenantId,
                recipientId,
                rendered.title(),
                rendered.body(),
                null,
                mapChannelCategory(match.notificationCategory()),
                mapChannelPriority(match.priority()),
                channels
        ));
    }

    private Map<String, Object> payloadOf(DomainEvent event) {
        if (event.getClass().isRecord()) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (RecordComponent component : event.getClass().getRecordComponents()) {
                try {
                    values.put(component.getName(), component.getAccessor().invoke(event));
                } catch (IllegalAccessException | InvocationTargetException ignored) {
                    values.put(component.getName(), null);
                }
            }
            Object payload = values.get("payload");
            if (payload instanceof Map<?, ?> nested) {
                nested.forEach((key, value) -> values.put(String.valueOf(key), value));
            }
            return values;
        }
        return Map.of();
    }

    private Map<String, Object> variables(DomainEvent event, Map<String, Object> payload) {
        Map<String, Object> variables = new LinkedHashMap<>(payload);
        variables.put("eventId", event.eventId());
        variables.put("eventType", event.eventType());
        variables.put("tenantId", event.tenantId());
        variables.put("occurredAt", event.occurredAt());
        return variables;
    }

    private String resolveRecipientId(Map<String, Object> payload, EventMatchView match) {
        Map<String, Object> config = jsonMap(match.targetResolverConfig());
        String configuredField = firstText(config, "personField", "assignmentField", "field");
        if (configuredField != null) {
            String configuredValue = stringValue(payload.get(configuredField));
            if (configuredValue != null && !configuredValue.isBlank()) {
                return configuredValue;
            }
        }
        if (match.targetResolverType() == TargetResolverType.PAYLOAD_PERSON) {
            return firstText(payload, "recipientPersonId", "personId", "assigneeId", "operatorPersonId");
        }
        if (match.targetResolverType() == TargetResolverType.PAYLOAD_ASSIGNMENT) {
            return firstText(payload, "assigneeId", "assignmentId", "recipientId", "recipientPersonId");
        }
        return firstText(payload, "recipientId", "recipientPersonId", "assigneeId", "personId", "operatorPersonId");
    }

    private Map<String, Object> jsonMap(String value) {
        if (value == null || value.isBlank() || !value.trim().startsWith("{")) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private String firstText(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            String value = stringValue(payload.get(key));
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private com.hjo2oa.msg.message.center.domain.NotificationCategory mapCategory(
            com.hjo2oa.msg.event.subscription.domain.NotificationCategory category
    ) {
        return com.hjo2oa.msg.message.center.domain.NotificationCategory.valueOf(category.name());
    }

    private com.hjo2oa.msg.message.center.domain.NotificationPriority mapPriority(
            com.hjo2oa.msg.event.subscription.domain.NotificationPriority priority
    ) {
        return com.hjo2oa.msg.message.center.domain.NotificationPriority.valueOf(priority.name());
    }

    private MessageCategory mapChannelCategory(
            com.hjo2oa.msg.event.subscription.domain.NotificationCategory category
    ) {
        return MessageCategory.valueOf(category.name());
    }

    private MessagePriority mapChannelPriority(
            com.hjo2oa.msg.event.subscription.domain.NotificationPriority priority
    ) {
        return MessagePriority.valueOf(priority.name());
    }

    private ChannelType mapChannel(com.hjo2oa.msg.event.subscription.domain.ChannelType channelType) {
        return switch (channelType) {
            case INBOX -> ChannelType.INBOX;
            case EMAIL -> ChannelType.EMAIL;
            case SMS -> ChannelType.SMS;
            case WEBHOOK -> ChannelType.WEBHOOK;
            case WECHAT_WORK -> ChannelType.WEBHOOK;
            case DINGTALK -> ChannelType.WEBHOOK;
            case PUSH, APP_PUSH -> ChannelType.APP_PUSH;
        };
    }

    private UUID toUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private UUID toNullableUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return toUuid(value);
    }
}
