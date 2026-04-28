package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@ConditionalOnProfile
public record GenericDomainEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String tenantId,
        Map<String, Object> payload
) implements DomainEvent {
}
