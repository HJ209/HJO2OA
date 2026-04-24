package com.hjo2oa.data.common.infrastructure.event;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.hjo2oa.data.common.domain.event.AbstractDataDomainEvent;
import com.hjo2oa.data.common.domain.event.DataEventTypes;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SpringDataDomainEventPublisherTest {

    @Test
    void shouldDelegateDataDomainEventToSharedPublisher() {
        AtomicReference<Object> publishedEvent = new AtomicReference<>();
        DomainEventPublisher delegate = publishedEvent::set;
        SpringDataDomainEventPublisher publisher = new SpringDataDomainEventPublisher(delegate);
        TestDataEvent event = new TestDataEvent();

        publisher.publish(event);

        assertSame(event, publishedEvent.get());
    }

    private static class TestDataEvent extends AbstractDataDomainEvent {

        TestDataEvent() {
            super(
                    DataEventTypes.DATA_SERVICE_ACTIVATED,
                    "11111111-1111-1111-1111-111111111111",
                    "data-service",
                    "service-definition",
                    "tester",
                    Map.of("code", "TEST_SERVICE")
            );
        }
    }
}
