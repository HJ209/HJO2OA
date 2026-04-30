package com.hjo2oa.infra.event.bus.application;

import java.time.Instant;
import java.util.UUID;

public record EventBusOperationAudit(
        UUID id,
        UUID eventId,
        String operationType,
        UUID operatorAccountId,
        UUID operatorPersonId,
        String tenantId,
        String traceId,
        String requestId,
        String idempotencyKey,
        String reason,
        String detailJson,
        Instant createdAt
) {
}
