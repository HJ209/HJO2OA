package com.hjo2oa.infra.event.bus.domain;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAttemptView(
        UUID id,
        UUID eventMessageId,
        String subscriberCode,
        int attemptNo,
        DeliveryStatus deliveryStatus,
        String errorCode,
        String errorMessage,
        Instant deliveredAt,
        Instant nextRetryAt
) {
}
