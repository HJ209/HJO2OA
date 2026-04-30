package com.hjo2oa.org.position.assignment.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PositionAssignmentChangedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String tenantId,
        UUID aggregateId,
        Map<String, Object> payload
) implements DomainEvent {

    public PositionAssignmentChangedEvent {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public static PositionAssignmentChangedEvent of(
            String eventType,
            UUID tenantId,
            UUID aggregateId,
            Instant occurredAt,
            Map<String, Object> payload
    ) {
        return new PositionAssignmentChangedEvent(
                UUID.randomUUID(),
                eventType,
                occurredAt,
                tenantId.toString(),
                aggregateId,
                payload
        );
    }
}
