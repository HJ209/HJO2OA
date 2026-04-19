package com.hjo2oa.msg.message.center.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MsgNotificationReadEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String notificationId,
        String recipientId,
        Instant readAt
) implements DomainEvent {

    public static final String EVENT_TYPE = "msg.notification.read";

    public MsgNotificationReadEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        notificationId = requireText(notificationId, "notificationId");
        recipientId = requireText(recipientId, "recipientId");
        Objects.requireNonNull(readAt, "readAt must not be null");
    }

    public static MsgNotificationReadEvent from(Notification notification) {
        Objects.requireNonNull(notification, "notification must not be null");
        return new MsgNotificationReadEvent(
                UUID.randomUUID(),
                notification.updatedAt(),
                notification.tenantId(),
                notification.notificationId(),
                notification.recipientId(),
                notification.readAt()
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
