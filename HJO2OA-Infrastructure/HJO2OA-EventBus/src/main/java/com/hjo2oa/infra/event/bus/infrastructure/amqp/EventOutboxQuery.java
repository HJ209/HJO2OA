package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import java.time.Instant;
import java.util.UUID;

public record EventOutboxQuery(
        UUID eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        String tenantId,
        String traceId,
        EventOutboxStatus status,
        Instant occurredFrom,
        Instant occurredTo,
        int page,
        int size
) {

    public EventOutboxQuery {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 500);
    }

    public static EventOutboxQuery forEventId(UUID eventId) {
        return new EventOutboxQuery(eventId, null, null, null, null, null, null, null, null, 1, 1);
    }

    public EventOutboxQuery withPage(int nextPage, int nextSize) {
        return new EventOutboxQuery(
                eventId,
                eventType,
                aggregateType,
                aggregateId,
                tenantId,
                traceId,
                status,
                occurredFrom,
                occurredTo,
                nextPage,
                nextSize
        );
    }
}
