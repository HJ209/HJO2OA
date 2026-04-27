package com.hjo2oa.msg.channel.sender.domain;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAttemptView(
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
}
