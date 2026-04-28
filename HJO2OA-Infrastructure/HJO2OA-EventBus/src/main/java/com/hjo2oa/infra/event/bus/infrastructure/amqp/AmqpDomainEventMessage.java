package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import java.time.Instant;
import java.util.UUID;

@ConditionalOnProfile
public record AmqpDomainEventMessage(
        UUID eventId,
        String eventType,
        String eventClass,
        Instant occurredAt,
        String tenantId,
        String payload
) {
}
