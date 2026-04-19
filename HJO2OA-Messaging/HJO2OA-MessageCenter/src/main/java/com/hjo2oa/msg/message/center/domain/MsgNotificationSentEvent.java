package com.hjo2oa.msg.message.center.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MsgNotificationSentEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String notificationId,
        String recipientId,
        String channel,
        String category,
        String sourceModule
) implements DomainEvent {

    public static final String EVENT_TYPE = "msg.notification.sent";

    public MsgNotificationSentEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        notificationId = requireText(notificationId, "notificationId");
        recipientId = requireText(recipientId, "recipientId");
        channel = requireText(channel, "channel");
        category = requireText(category, "category");
        sourceModule = requireText(sourceModule, "sourceModule");
    }

    public static MsgNotificationSentEvent from(Notification notification, NotificationDeliveryRecord deliveryRecord) {
        return new MsgNotificationSentEvent(
                UUID.randomUUID(),
                deliveryRecord.updatedAt(),
                notification.tenantId(),
                notification.notificationId(),
                notification.recipientId(),
                deliveryRecord.channel().name(),
                notification.category().name(),
                notification.sourceModule()
        );
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
