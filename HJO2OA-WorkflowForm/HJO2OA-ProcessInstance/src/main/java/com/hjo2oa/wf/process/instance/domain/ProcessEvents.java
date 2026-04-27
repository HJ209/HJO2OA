package com.hjo2oa.wf.process.instance.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProcessEvents {

    public static final String TASK_COMPLETED_EVENT_TYPE = "infra.event.task-completed";

    private ProcessEvents() {
    }

    public record ProcessInstanceStartedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID instanceId,
            UUID definitionId,
            UUID initiatorId
    ) implements DomainEvent {
    }

    public record ProcessInstanceCompletedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID instanceId,
            Instant endTime
    ) implements DomainEvent {
    }

    public record ProcessTaskCreatedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID taskId,
            UUID instanceId,
            String nodeId,
            CandidateType candidateType,
            List<UUID> candidateIds
    ) implements DomainEvent {
    }

    public record ProcessTaskCompletedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID taskId,
            UUID instanceId,
            String actionCode
    ) implements DomainEvent {
    }
}
