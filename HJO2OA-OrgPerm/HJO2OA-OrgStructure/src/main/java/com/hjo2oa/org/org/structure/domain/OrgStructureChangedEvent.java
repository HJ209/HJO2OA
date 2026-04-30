package com.hjo2oa.org.org.structure.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OrgStructureChangedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String tenantId,
        UUID aggregateId,
        Map<String, Object> payload
) implements DomainEvent {

    public OrgStructureChangedEvent {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public static OrgStructureChangedEvent of(
            String eventType,
            UUID tenantId,
            UUID aggregateId,
            Instant occurredAt,
            Map<String, Object> payload
    ) {
        return new OrgStructureChangedEvent(
                UUID.randomUUID(),
                eventType,
                occurredAt,
                tenantId.toString(),
                aggregateId,
                payload
        );
    }
}
