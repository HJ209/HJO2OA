package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.util.Map;
import java.util.Objects;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@ConditionalOnProfile
@ConditionalOnProperty(prefix = "hjo2oa.messaging.outbox.amqp", name = "enabled", havingValue = "true")
@Component
public class AmqpDomainEventConsumer {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    public AmqpDomainEventConsumer(ApplicationEventPublisher applicationEventPublisher, ObjectMapper objectMapper) {
        this.applicationEventPublisher = Objects.requireNonNull(
                applicationEventPublisher,
                "applicationEventPublisher must not be null"
        );
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @RabbitListener(queues = "${hjo2oa.messaging.outbox.amqp.queue-name:hjo2oa.domain.events.local}")
    public void onMessage(String body) {
        AmqpDomainEventMessage message = readMessage(body);
        applicationEventPublisher.publishEvent(toDomainEvent(message));
    }

    private AmqpDomainEventMessage readMessage(String body) {
        try {
            return objectMapper.readValue(body, AmqpDomainEventMessage.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid AMQP domain event message", ex);
        }
    }

    private DomainEvent toDomainEvent(AmqpDomainEventMessage message) {
        try {
            Class<?> eventClass = Class.forName(message.eventClass());
            if (DomainEvent.class.isAssignableFrom(eventClass)) {
                return objectMapper.readValue(message.payload(), eventClass.asSubclass(DomainEvent.class));
            }
        } catch (ClassNotFoundException | JsonProcessingException ex) {
            return fallbackEvent(message);
        }
        return fallbackEvent(message);
    }

    private GenericDomainEvent fallbackEvent(AmqpDomainEventMessage message) {
        try {
            return new GenericDomainEvent(
                    message.eventId(),
                    message.eventType(),
                    message.occurredAt(),
                    message.tenantId(),
                    objectMapper.readValue(message.payload(), MAP_TYPE)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid AMQP domain event payload", ex);
        }
    }
}
