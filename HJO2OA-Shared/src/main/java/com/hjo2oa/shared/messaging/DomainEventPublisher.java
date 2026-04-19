package com.hjo2oa.shared.messaging;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
