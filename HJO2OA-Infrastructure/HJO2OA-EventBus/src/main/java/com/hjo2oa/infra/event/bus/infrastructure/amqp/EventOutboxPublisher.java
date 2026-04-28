package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import java.util.Objects;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private final String exchangeName;

    public EventOutboxPublisher(
            EventOutboxRepository outboxRepository,
            RabbitTemplate rabbitTemplate,
            @Value("${hjo2oa.messaging.outbox.amqp.exchange-name:hjo2oa.domain.events}") String exchangeName
    ) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "rabbitTemplate must not be null");
        this.exchangeName = Objects.requireNonNull(exchangeName, "exchangeName must not be null");
    }

    @Scheduled(fixedDelayString = "${hjo2oa.messaging.outbox.scan-interval:5000}")
    public void publishPendingEvents() {
        for (EventOutboxEntity event : outboxRepository.findPending(50)) {
            try {
                rabbitTemplate.convertAndSend(exchangeName, ROUTING_KEY, event.getPayloadJson());
                outboxRepository.markPublished(event.getId());
            } catch (RuntimeException ex) {
                outboxRepository.markFailed(event.getId());
            }
        }
    }
}
