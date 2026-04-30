package com.hjo2oa.infra.event.bus.infrastructure.amqp;

public enum EventOutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
    DEAD
}
