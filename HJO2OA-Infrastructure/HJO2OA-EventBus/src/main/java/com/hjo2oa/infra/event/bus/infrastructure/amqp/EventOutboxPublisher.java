package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@ConditionalOnProfile
@ConditionalOnProperty(prefix = "hjo2oa.messaging.outbox.amqp", name = "enabled", havingValue = "true")
@Component
public class EventOutboxPublisher {

    private static final String ROUTING_KEY = "domain.event";

    private final EventOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final EventOutboxRetryPolicy retryPolicy;
    private final Clock clock;
    private final String exchangeName;
    private final int batchSize;

    @Autowired
    public EventOutboxPublisher(
            EventOutboxRepository outboxRepository,
            RabbitTemplate rabbitTemplate,
            EventOutboxRetryPolicy retryPolicy,
            @Value("${hjo2oa.messaging.outbox.amqp.exchange-name:hjo2oa.domain.events}") String exchangeName
    ) {
        this(outboxRepository, rabbitTemplate, retryPolicy, Clock.systemUTC(), exchangeName, 50);
    }

    EventOutboxPublisher(
            EventOutboxRepository outboxRepository,
            RabbitTemplate rabbitTemplate,
            EventOutboxRetryPolicy retryPolicy,
            Clock clock,
            String exchangeName,
            int batchSize
    ) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "rabbitTemplate must not be null");
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.exchangeName = Objects.requireNonNull(exchangeName, "exchangeName must not be null");
        this.batchSize = Math.max(batchSize, 1);
    }

    @Scheduled(fixedDelayString = "${hjo2oa.messaging.outbox.scan-interval:5000}")
    public void publishPendingEvents() {
        Instant now = clock.instant();
        for (EventOutboxEntity event : outboxRepository.findDueForPublish(now, batchSize)) {
            try {
                rabbitTemplate.convertAndSend(exchangeName, ROUTING_KEY, event.getPayloadJson(), message -> {
                    applyMessageProperties(message, event);
                    return message;
                });
                outboxRepository.markPublished(event.getId(), clock.instant());
            } catch (RuntimeException ex) {
                markFailure(event, ex);
            }
        }
    }

    private void applyMessageProperties(Message message, EventOutboxEntity event) {
        MessageProperties properties = message.getMessageProperties();
        properties.setMessageId(event.getEventId() == null ? event.getId().toString() : event.getEventId().toString());
        properties.setType(event.getEventType());
        properties.setCorrelationId(event.getTraceId());
        properties.setHeader("eventType", event.getEventType());
        properties.setHeader("tenantId", event.getTenantId());
        properties.setHeader("schemaVersion", event.getSchemaVersion());
        properties.setHeader("traceId", event.getTraceId());
    }

    private void markFailure(EventOutboxEntity event, RuntimeException ex) {
        int retryCount = event.getRetryCount() == null ? 1 : event.getRetryCount() + 1;
        String error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        Instant now = clock.instant();
        if (retryPolicy.exhausted(retryCount)) {
            outboxRepository.markDead(event.getId(), retryCount, error, now);
            return;
        }
        outboxRepository.markFailed(event.getId(), retryCount, retryPolicy.nextRetryAt(now, retryCount), error);
    }
}
