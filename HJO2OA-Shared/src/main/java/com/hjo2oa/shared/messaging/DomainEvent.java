package com.hjo2oa.shared.messaging;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

    UUID eventId();

    String eventType();

    Instant occurredAt();

    String tenantId();
}
