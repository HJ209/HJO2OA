package com.hjo2oa.wf.process.instance.infrastructure.flowable;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record FlowableProcessEngineDomainEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String tenantId,
        String flowableType,
        String processDefinitionId,
        String processInstanceId,
        String executionId,
        String entityId,
        String entityType
) implements DomainEvent {
}
