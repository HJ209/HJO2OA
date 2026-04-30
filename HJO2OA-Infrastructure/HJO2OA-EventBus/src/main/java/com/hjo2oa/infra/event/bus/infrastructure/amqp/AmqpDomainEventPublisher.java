package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.shared.messaging.DomainEvent;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@ConditionalOnProfile
@ConditionalOnProperty(prefix = "hjo2oa.messaging.outbox.amqp", name = "enabled", havingValue = "true")
@Primary
@Component
public class AmqpDomainEventPublisher implements DomainEventPublisher {

    private final EventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final DomainEventEnvelopeFactory envelopeFactory;
    private final Clock clock;

    @Autowired
    public AmqpDomainEventPublisher(EventOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this(outboxRepository, objectMapper, Clock.systemUTC());
    }

    AmqpDomainEventPublisher(EventOutboxRepository outboxRepository, ObjectMapper objectMapper, Clock clock) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.envelopeFactory = new DomainEventEnvelopeFactory(objectMapper, clock);
    }

    @Override
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        DomainEventEnvelope envelope = envelopeFactory.from(event);
        Instant now = clock.instant();
        EventOutboxEntity entity = new EventOutboxEntity()
                .setId(UUID.randomUUID())
                .setEventId(envelope.eventId())
                .setAggregateType(envelope.aggregateType())
                .setAggregateId(envelope.aggregateId())
                .setEventType(envelope.eventType())
                .setTenantId(envelope.tenantId())
                .setOccurredAt(envelope.occurredAt())
                .setTraceId(envelope.traceId())
                .setSchemaVersion(envelope.schemaVersion())
                .setPayloadJson(writeJson(envelope))
                .setStatus(EventOutboxStatus.PENDING.name())
                .setCreatedAt(now)
                .setRetryCount(0);
        outboxRepository.save(entity);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize domain event", ex);
        }
    }
}
