package com.hjo2oa.infra.event.bus.application;

import java.util.Optional;

public interface EventBusOperationAuditRepository {

    EventBusOperationAudit save(EventBusOperationAudit audit);

    Optional<EventBusOperationAudit> findByIdempotencyKey(String idempotencyKey);
}
