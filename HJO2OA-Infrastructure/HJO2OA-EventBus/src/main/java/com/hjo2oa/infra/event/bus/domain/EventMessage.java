package com.hjo2oa.infra.event.bus.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record EventMessage(
        UUID id,
        UUID eventDefinitionId,
        String eventType,
        String source,
        UUID tenantId,
        String correlationId,
        String traceId,
        UUID operatorAccountId,
        UUID operatorPersonId,
        String payload,
        PublishStatus publishStatus,
        Instant publishedAt,
        Instant retainedUntil,
        Instant createdAt,
        Instant updatedAt,
        List<DeliveryAttempt> deliveryAttempts
) {

    public EventMessage {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(eventDefinitionId, "eventDefinitionId must not be null");
        eventType = requireText(eventType, "eventType");
        source = requireText(source, "source");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(publishStatus, "publishStatus must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        deliveryAttempts = deliveryAttempts == null ? List.of() : List.copyOf(deliveryAttempts);
    }

    public static EventMessage create(
            UUID id,
            UUID eventDefinitionId,
            String eventType,
            String source,
            UUID tenantId,
            String correlationId,
            String traceId,
            UUID operatorAccountId,
            UUID operatorPersonId,
            String payload,
            Instant now
    ) {
        return new EventMessage(
                id, eventDefinitionId, eventType, source, tenantId,
                correlationId, traceId, operatorAccountId, operatorPersonId,
                payload, PublishStatus.PENDING, null, null,
                now, now, List.of()
        );
    }

    public EventMessage markPublished(Instant now) {
        return new EventMessage(
                id, eventDefinitionId, eventType, source, tenantId,
                correlationId, traceId, operatorAccountId, operatorPersonId,
                payload, PublishStatus.PUBLISHED, now, retainedUntil,
                createdAt, now, deliveryAttempts
        );
    }

    public EventMessage markDelivered(Instant now) {
        return new EventMessage(
                id, eventDefinitionId, eventType, source, tenantId,
                correlationId, traceId, operatorAccountId, operatorPersonId,
                payload, PublishStatus.DELIVERED, publishedAt, retainedUntil,
                createdAt, now, deliveryAttempts
        );
    }

    public EventMessage markDeadLettered(Instant now) {
        return new EventMessage(
                id, eventDefinitionId, eventType, source, tenantId,
                correlationId, traceId, operatorAccountId, operatorPersonId,
                payload, PublishStatus.DEAD_LETTERED, publishedAt, retainedUntil,
                createdAt, now, deliveryAttempts
        );
    }

    public EventMessage addDeliveryAttempt(DeliveryAttempt attempt, Instant now) {
        Objects.requireNonNull(attempt, "attempt must not be null");
        List<DeliveryAttempt> updated = new ArrayList<>(deliveryAttempts);
        updated.add(attempt);
        return new EventMessage(
                id, eventDefinitionId, eventType, source, tenantId,
                correlationId, traceId, operatorAccountId, operatorPersonId,
                payload, publishStatus, publishedAt, retainedUntil,
                createdAt, now, updated
        );
    }

    public EventMessageView toView() {
        return new EventMessageView(
                id, eventDefinitionId, eventType, source, tenantId,
                correlationId, traceId, operatorAccountId, operatorPersonId,
                payload, publishStatus, publishedAt, retainedUntil,
                createdAt, updatedAt,
                deliveryAttempts.stream().map(DeliveryAttempt::toView).toList()
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
