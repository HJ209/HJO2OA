package com.hjo2oa.msg.message.center.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MsgNotificationDeliveredEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String notificationId,
        String channel,
        Instant deliveredAt
) implements DomainEvent {

    public static final String EVENT_TYPE = "msg.notification.delivered";

    public MsgNotificationDeliveredEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        notificationId = requireText(notificationId, "notificationId");
        channel = requireText(channel, "channel");
        Objects.requireNonNull(deliveredAt, "deliveredAt must not be null");
    }

    public static MsgNotificationDeliveredEvent from(Notification notification, NotificationDeliveryRecord deliveryRecord) {
        Instant resolvedDeliveredAt = Objects.requireNonNull(deliveryRecord.deliveredAt(), "deliveredAt must not be null");
        return new MsgNotificationDeliveredEvent(
                UUID.randomUUID(),
                resolvedDeliveredAt,
                notification.tenantId(),
                notification.notificationId(),
                deliveryRecord.channel().name(),
                resolvedDeliveredAt
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
