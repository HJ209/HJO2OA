package com.hjo2oa.infra.event.bus.infrastructure;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertSame;

class SpringDomainEventPublisherTest {

    @Test
    void shouldForwardDomainEventToSpringPublisher() {
        AtomicReference<Object> publishedEvent = new AtomicReference<>();
        ApplicationEventPublisher applicationEventPublisher = publishedEvent::set;
        SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(applicationEventPublisher);
        DomainEvent event = new TestDomainEvent();

        publisher.publish(event);

        assertSame(event, publishedEvent.get());
    }

    private record TestDomainEvent() implements DomainEvent {

        @Override
        public UUID eventId() {
            return UUID.randomUUID();
        }

        @Override
        public String eventType() {
            return "test.event";
        }

        @Override
        public Instant occurredAt() {
            return Instant.parse("2026-04-19T00:00:00Z");
        }

        @Override
        public String tenantId() {
            return "tenant-1";
        }
    }
}
