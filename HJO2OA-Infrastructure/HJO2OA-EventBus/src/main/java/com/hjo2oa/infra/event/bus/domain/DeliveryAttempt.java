package com.hjo2oa.infra.event.bus.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DeliveryAttempt(
        UUID id,
        UUID eventMessageId,
        String subscriberCode,
        int attemptNo,
        DeliveryStatus deliveryStatus,
        String errorCode,
        String errorMessage,
        Instant deliveredAt,
        Instant nextRetryAt,
        String requestSnapshot,
        String responseSnapshot
) {

    public DeliveryAttempt {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(eventMessageId, "eventMessageId must not be null");
        subscriberCode = requireText(subscriberCode, "subscriberCode");
        if (attemptNo < 1) {
            throw new IllegalArgumentException("attemptNo must be >= 1");
        }
        Objects.requireNonNull(deliveryStatus, "deliveryStatus must not be null");
    }

    public static DeliveryAttempt create(
            UUID id,
            UUID eventMessageId,
            String subscriberCode,
            int attemptNo,
            DeliveryStatus deliveryStatus,
            String errorCode,
            String errorMessage,
            Instant nextRetryAt
    ) {
        return new DeliveryAttempt(
                id, eventMessageId, subscriberCode, attemptNo,
                deliveryStatus, errorCode, errorMessage,
                null, nextRetryAt, null, null
        );
    }

    public DeliveryAttempt markSuccess(Instant deliveredAt) {
        return new DeliveryAttempt(
                id, eventMessageId, subscriberCode, attemptNo,
                DeliveryStatus.SUCCESS, null, null,
                deliveredAt, null, requestSnapshot, responseSnapshot
        );
    }

    public DeliveryAttempt markFailed(String errorCode, String errorMessage, Instant nextRetryAt) {
        return new DeliveryAttempt(
                id, eventMessageId, subscriberCode, attemptNo,
                DeliveryStatus.FAILED, errorCode, errorMessage,
                null, nextRetryAt, requestSnapshot, responseSnapshot
        );
    }

    public DeliveryAttempt markDeadLettered() {
        return new DeliveryAttempt(
                id, eventMessageId, subscriberCode, attemptNo,
                DeliveryStatus.DEAD_LETTERED, errorCode, errorMessage,
                null, null, requestSnapshot, responseSnapshot
        );
    }

    public DeliveryAttemptView toView() {
        return new DeliveryAttemptView(
                id, eventMessageId, subscriberCode, attemptNo,
                deliveryStatus, errorCode, errorMessage,
                deliveredAt, nextRetryAt
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
