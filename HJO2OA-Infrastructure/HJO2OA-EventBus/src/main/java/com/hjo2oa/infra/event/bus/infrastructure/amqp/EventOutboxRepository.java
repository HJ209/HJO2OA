package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import java.util.List;
import java.util.UUID;

@ConditionalOnProfile
public interface EventOutboxRepository {

    void save(EventOutboxEntity entity);

    List<EventOutboxEntity> findPending(int limit);

    void markPublished(UUID id);

    void markFailed(UUID id);
}
