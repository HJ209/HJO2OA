package com.hjo2oa.data.common.infrastructure.event;

import com.hjo2oa.data.common.application.event.DataDomainEventPublisher;
import com.hjo2oa.data.common.domain.event.DataDomainEvent;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDataDomainEventPublisher implements DataDomainEventPublisher {

    private final DomainEventPublisher domainEventPublisher;

    public SpringDataDomainEventPublisher(DomainEventPublisher domainEventPublisher) {
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public void publish(DataDomainEvent event) {
        domainEventPublisher.publish(event);
    }
}
