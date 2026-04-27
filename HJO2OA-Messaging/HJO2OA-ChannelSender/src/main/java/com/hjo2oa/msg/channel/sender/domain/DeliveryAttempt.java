package com.hjo2oa.msg.channel.sender.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DeliveryAttempt(
        UUID id,
        UUID deliveryTaskId,
        int attemptNo,
        String requestPayloadSnapshot,
        String providerResponse,
        DeliveryAttemptResultStatus resultStatus,
        String errorCode,
        String errorMessage,
        Instant requestedAt,
        Instant completedAt
) {

    public DeliveryAttempt {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(deliveryTaskId, "deliveryTaskId must not be null");
        if (attemptNo < 1) {
            throw new IllegalArgumentException("attemptNo must be positive");
        }
        Objects.requireNonNull(resultStatus, "resultStatus must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
    }

    public static DeliveryAttempt create(
            UUID id,
            UUID deliveryTaskId,
            int attemptNo,
            String requestPayloadSnapshot,
            String providerResponse,
            DeliveryAttemptResultStatus resultStatus,
            String errorCode,
            String errorMessage,
            Instant now
    ) {
        return new DeliveryAttempt(
                id,
                deliveryTaskId,
                attemptNo,
                requestPayloadSnapshot,
                providerResponse,
                resultStatus,
                errorCode,
                errorMessage,
                now,
                now
        );
    }

    public DeliveryAttemptView toView() {
        return new DeliveryAttemptView(
                id,
                deliveryTaskId,
                attemptNo,
                requestPayloadSnapshot,
                providerResponse,
                resultStatus,
                errorCode,
                errorMessage,
                requestedAt,
                completedAt
        );
    }
}
