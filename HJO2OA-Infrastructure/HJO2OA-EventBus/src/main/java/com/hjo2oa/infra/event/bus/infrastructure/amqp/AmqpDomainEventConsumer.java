package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final ConsumedEventRepository consumedEventRepository;
    private final Clock clock;
    private final String consumerCode;

    @Autowired
    public AmqpDomainEventConsumer(
            ApplicationEventPublisher applicationEventPublisher,
            ObjectMapper objectMapper,
            ConsumedEventRepository consumedEventRepository,
            @Value("${hjo2oa.messaging.outbox.amqp.consumer-code:spring-application-event}") String consumerCode
    ) {
        this(applicationEventPublisher, objectMapper, consumedEventRepository, Clock.systemUTC(), consumerCode);
    }

    AmqpDomainEventConsumer(
            ApplicationEventPublisher applicationEventPublisher,
            ObjectMapper objectMapper,
            ConsumedEventRepository consumedEventRepository,
            Clock clock,
            String consumerCode
    ) {
        this.applicationEventPublisher = Objects.requireNonNull(
                applicationEventPublisher,
                "applicationEventPublisher must not be null"
        );
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.consumedEventRepository = Objects.requireNonNull(
                consumedEventRepository,
                "consumedEventRepository must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.consumerCode = Objects.requireNonNull(consumerCode, "consumerCode must not be null");
    }

    @RabbitListener(queues = "${hjo2oa.messaging.outbox.amqp.queue-name:hjo2oa.domain.events.local}")
    public void onMessage(String body) {
        DomainEventEnvelope envelope = readEnvelope(body);
        if (!consumedEventRepository.tryStart(envelope, consumerCode, clock.instant())) {
            return;
        }
        try {
            applicationEventPublisher.publishEvent(toDomainEvent(envelope));
            consumedEventRepository.markSuccess(envelope.eventId(), consumerCode, clock.instant());
        } catch (RuntimeException ex) {
            consumedEventRepository.markFailed(envelope.eventId(), consumerCode, ex.getMessage(), clock.instant());
            throw ex;
        }
    }

    private DomainEventEnvelope readEnvelope(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.hasNonNull("aggregateType")) {
                return objectMapper.treeToValue(root, DomainEventEnvelope.class);
            }
            return legacyEnvelope(objectMapper.treeToValue(root, AmqpDomainEventMessage.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid AMQP domain event message", ex);
        }
    }

    private DomainEventEnvelope legacyEnvelope(AmqpDomainEventMessage message) throws JsonProcessingException {
        UUID eventId = message.eventId() == null ? UUID.randomUUID() : message.eventId();
        JsonNode eventBody = objectMapper.readTree(message.payload());
        return new DomainEventEnvelope(
                eventId,
                message.eventType(),
                message.eventType(),
                message.tenantId() == null ? eventId.toString() : message.tenantId(),
                message.tenantId() == null ? "GLOBAL" : message.tenantId(),
                message.occurredAt() == null ? clock.instant() : message.occurredAt(),
                eventId.toString(),
                "1",
                eventBody,
                message.eventClass(),
                eventBody
        );
    }

    private DomainEvent toDomainEvent(DomainEventEnvelope envelope) {
        try {
            Class<?> eventClass = envelope.eventClass() == null ? null : Class.forName(envelope.eventClass());
            if (eventClass != null && DomainEvent.class.isAssignableFrom(eventClass)) {
                return objectMapper.treeToValue(envelope.eventBody(), eventClass.asSubclass(DomainEvent.class));
            }
        } catch (ClassNotFoundException | JsonProcessingException ex) {
            return fallbackEvent(envelope);
        }
        return fallbackEvent(envelope);
    }

    private GenericDomainEvent fallbackEvent(DomainEventEnvelope envelope) {
        return new GenericDomainEvent(
                envelope.eventId(),
                envelope.eventType(),
                envelope.occurredAt(),
                envelope.tenantId(),
                objectMapper.convertValue(envelope.payload(), MAP_TYPE)
        );
    }
}
