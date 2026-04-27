package com.hjo2oa.infra.event.bus.domain;

import java.util.Optional;
import java.util.UUID;

public interface EventMessageRepository {

    EventMessage save(EventMessage message);

    Optional<EventMessage> findById(UUID id);

    java.util.List<EventMessage> findByPublishStatus(PublishStatus status);

    java.util.List<EventMessage> findByTenantId(UUID tenantId);

    java.util.List<EventMessage> findByTraceId(String traceId);
}
