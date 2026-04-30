package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import java.time.Instant;
import java.util.UUID;

public interface ConsumedEventRepository {

    boolean tryStart(DomainEventEnvelope envelope, String consumerCode, Instant now);

    void markSuccess(UUID eventId, String consumerCode, Instant now);

    void markFailed(UUID eventId, String consumerCode, String lastError, Instant now);
}
