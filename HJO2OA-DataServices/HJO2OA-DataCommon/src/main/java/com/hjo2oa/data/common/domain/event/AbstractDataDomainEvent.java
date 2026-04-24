package com.hjo2oa.data.common.domain.event;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public abstract class AbstractDataDomainEvent implements DataDomainEvent {

    private final UUID eventId;
    private final String eventType;
    private final Instant occurredAt;
    private final String tenantId;
    private final String moduleCode;
    private final String aggregateCode;
    private final String operatorId;
    private final Map<String, Object> payload;

    protected AbstractDataDomainEvent(
            String eventType,
            String tenantId,
            String moduleCode,
            String aggregateCode,
            String operatorId,
            Map<String, Object> payload
    ) {
        this(
                UUID.randomUUID(),
                eventType,
                Instant.now(),
                tenantId,
                moduleCode,
                aggregateCode,
                operatorId,
                payload
        );
    }

    protected AbstractDataDomainEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            String moduleCode,
            String aggregateCode,
            String operatorId,
            Map<String, Object> payload
    ) {
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.moduleCode = Objects.requireNonNull(moduleCode, "moduleCode must not be null");
        this.aggregateCode = Objects.requireNonNull(aggregateCode, "aggregateCode must not be null");
        this.operatorId = operatorId;
        this.payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    @Override
    public UUID eventId() {
        return eventId;
    }

    @Override
    public String eventType() {
        return eventType;
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }

    @Override
    public String tenantId() {
        return tenantId;
    }

    @Override
    public String moduleCode() {
        return moduleCode;
    }

    @Override
    public String aggregateCode() {
        return aggregateCode;
    }

    @Override
    public String operatorId() {
        return operatorId;
    }

    @Override
    public Map<String, Object> payload() {
        return payload;
    }
}
