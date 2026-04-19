package com.hjo2oa.msg.message.center.domain;

import java.time.Instant;
import java.util.Objects;

public record NotificationDeliveryRecord(
        String deliveryId,
        String notificationId,
        NotificationDeliveryChannel channel,
        NotificationDeliveryStatus status,
        int attemptCount,
        Instant createdAt,
        Instant updatedAt,
        Instant deliveredAt,
        String lastErrorCode
) {

    public NotificationDeliveryRecord {
        deliveryId = requireText(deliveryId, "deliveryId");
        notificationId = requireText(notificationId, "notificationId");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (attemptCount < 0) {
            throw new IllegalArgumentException("attemptCount must not be negative");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        lastErrorCode = normalize(lastErrorCode);
    }

    public static NotificationDeliveryRecord pending(
            String deliveryId,
            String notificationId,
            NotificationDeliveryChannel channel,
            Instant occurredAt
    ) {
        return new NotificationDeliveryRecord(
                deliveryId,
                notificationId,
                channel,
                NotificationDeliveryStatus.PENDING,
                0,
                occurredAt,
                occurredAt,
                null,
                null
        );
    }

    public NotificationDeliveryRecord markDelivered(Instant deliveredAt) {
        Instant resolvedDeliveredAt = Objects.requireNonNull(deliveredAt, "deliveredAt must not be null");
        return new NotificationDeliveryRecord(
                deliveryId,
                notificationId,
                channel,
                NotificationDeliveryStatus.DELIVERED,
                attemptCount + 1,
                createdAt,
                resolvedDeliveredAt,
                resolvedDeliveredAt,
                null
        );
    }

    public NotificationDeliveryRecord markFailed(String errorCode, Instant failedAt) {
        Instant resolvedFailedAt = Objects.requireNonNull(failedAt, "failedAt must not be null");
        return new NotificationDeliveryRecord(
                deliveryId,
                notificationId,
                channel,
                NotificationDeliveryStatus.FAILED,
                attemptCount + 1,
                createdAt,
                resolvedFailedAt,
                deliveredAt,
                requireText(errorCode, "errorCode")
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
