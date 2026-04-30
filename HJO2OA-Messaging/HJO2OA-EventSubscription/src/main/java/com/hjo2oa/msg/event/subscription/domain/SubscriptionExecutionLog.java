package com.hjo2oa.msg.event.subscription.domain;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionExecutionLog(
        UUID id,
        UUID eventId,
        String eventType,
        String ruleCode,
        String recipientId,
        String result,
        String message,
        UUID tenantId,
        Instant occurredAt
) {
}
