package com.hjo2oa.msg.message.center.domain;

import java.time.Instant;
import java.util.Objects;

public record NotificationAction(
        String actionId,
        String notificationId,
        NotificationActionType actionType,
        String operatorId,
        Instant occurredAt
) {

    public NotificationAction {
        actionId = requireText(actionId, "actionId");
        notificationId = requireText(notificationId, "notificationId");
        Objects.requireNonNull(actionType, "actionType must not be null");
        operatorId = requireText(operatorId, "operatorId");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static NotificationAction read(
            String actionId,
            String notificationId,
            String operatorId,
            Instant occurredAt
    ) {
        return new NotificationAction(
                actionId,
                notificationId,
                NotificationActionType.READ,
                operatorId,
                occurredAt
        );
    }

    public static NotificationAction archive(
            String actionId,
            String notificationId,
            String operatorId,
            Instant occurredAt
    ) {
        return new NotificationAction(
                actionId,
                notificationId,
                NotificationActionType.ARCHIVE,
                operatorId,
                occurredAt
        );
    }

    public static NotificationAction delete(
            String actionId,
            String notificationId,
            String operatorId,
            Instant occurredAt
    ) {
        return new NotificationAction(
                actionId,
                notificationId,
                NotificationActionType.DELETE,
                operatorId,
                occurredAt
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
