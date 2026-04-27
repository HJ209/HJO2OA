package com.hjo2oa.infra.event.bus.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventMessageView(
        UUID id,
        UUID eventDefinitionId,
        String eventType,
        String source,
        UUID tenantId,
        String correlationId,
        String traceId,
        UUID operatorAccountId,
        UUID operatorPersonId,
        String payload,
        PublishStatus publishStatus,
        Instant publishedAt,
        Instant retainedUntil,
        Instant createdAt,
        Instant updatedAt,
        List<DeliveryAttemptView> deliveryAttempts
) {
}
