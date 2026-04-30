package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventOutboxRepository {

    void save(EventOutboxEntity entity);

    List<EventOutboxEntity> findDueForPublish(Instant now, int limit);

    Optional<EventOutboxEntity> findByEventId(UUID eventId);

    EventOutboxPage query(EventOutboxQuery query);

    EventOutboxStatistics statistics();

    void markPublished(UUID id, Instant publishedAt);

    void markFailed(UUID id, int retryCount, Instant nextRetryAt, String lastError);

    void markDead(UUID id, int retryCount, String lastError, Instant deadAt);

    void resetForReplay(UUID id, Instant now);
}
