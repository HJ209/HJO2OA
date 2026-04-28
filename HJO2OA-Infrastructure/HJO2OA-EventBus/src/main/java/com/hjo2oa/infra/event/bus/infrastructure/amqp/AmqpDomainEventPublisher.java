package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.shared.messaging.DomainEvent;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@ConditionalOnProfile
@ConditionalOnProperty(prefix = "hjo2oa.messaging.outbox.amqp", name = "enabled", havingValue = "true")
@Primary
@Component
public class AmqpDomainEventPublisher implements DomainEventPublisher {

    private static final String STATUS_PENDING = "PENDING";

    private final EventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public AmqpDomainEventPublisher(EventOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        AmqpDomainEventMessage message = new AmqpDomainEventMessage(
                event.eventId(),
                event.eventType(),
                event.getClass().getName(),
                event.occurredAt(),
                event.tenantId(),
                writeJson(event)
        );
        EventOutboxEntity entity = new EventOutboxEntity()
                .setId(event.eventId() == null ? UUID.randomUUID() : event.eventId())
                .setAggregateType(event.eventType())
                .setAggregateId(event.tenantId())
                .setEventType(event.eventType())
                .setPayloadJson(writeJson(message))
                .setStatus(STATUS_PENDING)
                .setCreatedAt(Instant.now())
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
