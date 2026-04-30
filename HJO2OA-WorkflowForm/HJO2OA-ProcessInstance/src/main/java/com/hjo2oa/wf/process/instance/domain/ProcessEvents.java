package com.hjo2oa.wf.process.instance.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProcessEvents {

    public static final String INSTANCE_STARTED = "process.instance.started";
    public static final String INSTANCE_COMPLETED = "process.instance.completed";
    public static final String INSTANCE_TERMINATED = "process.instance.terminated";
    public static final String INSTANCE_SUSPENDED = "process.instance.suspended";
    public static final String INSTANCE_RESUMED = "process.instance.resumed";
    public static final String TASK_CREATED = "process.task.created";
    public static final String TASK_CLAIMED = "process.task.claimed";
    public static final String TASK_COMPLETED_EVENT_TYPE = "process.task.completed";
    public static final String TASK_TERMINATED = "process.task.terminated";
    public static final String TASK_TRANSFERRED = "process.task.transferred";

    private ProcessEvents() {
    }

    public record ProcessInstanceStartedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID instanceId,
            UUID definitionId,
            String definitionCode,
            int definitionVersion,
            UUID initiatorId,
            UUID initiatorOrgId,
            UUID formDataId,
            List<String> currentNodes
    ) implements DomainEvent {

        public static ProcessInstanceStartedEvent from(ProcessInstance instance, Instant occurredAt) {
            return new ProcessInstanceStartedEvent(
                    UUID.randomUUID(),
                    INSTANCE_STARTED,
                    occurredAt,
                    instance.tenantId().toString(),
                    instance.id(),
                    instance.definitionId(),
                    instance.definitionCode(),
                    instance.definitionVersion(),
                    instance.initiatorId(),
                    instance.initiatorOrgId(),
                    instance.formDataId(),
                    instance.currentNodes()
            );
        }
    }

    public record ProcessInstanceCompletedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID instanceId,
            UUID definitionId,
            Instant endTime
    ) implements DomainEvent {

        public static ProcessInstanceCompletedEvent from(ProcessInstance instance, Instant occurredAt) {
            return new ProcessInstanceCompletedEvent(
                    UUID.randomUUID(),
                    INSTANCE_COMPLETED,
                    occurredAt,
                    instance.tenantId().toString(),
                    instance.id(),
                    instance.definitionId(),
                    occurredAt
            );
        }
    }

    public record ProcessInstanceTerminatedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID instanceId,
            UUID definitionId,
            String reason,
            Instant endTime
    ) implements DomainEvent {

        public static ProcessInstanceTerminatedEvent from(ProcessInstance instance, String reason, Instant occurredAt) {
            return new ProcessInstanceTerminatedEvent(
                    UUID.randomUUID(),
                    INSTANCE_TERMINATED,
                    occurredAt,
                    instance.tenantId().toString(),
                    instance.id(),
                    instance.definitionId(),
                    reason,
                    occurredAt
            );
        }
    }

    public record ProcessInstanceSuspendedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID instanceId,
            String reason
    ) implements DomainEvent {

        public static ProcessInstanceSuspendedEvent from(ProcessInstance instance, String reason, Instant occurredAt) {
            return new ProcessInstanceSuspendedEvent(
                    UUID.randomUUID(),
                    INSTANCE_SUSPENDED,
                    occurredAt,
                    instance.tenantId().toString(),
                    instance.id(),
                    reason
            );
        }
    }

    public record ProcessInstanceResumedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID instanceId
    ) implements DomainEvent {

        public static ProcessInstanceResumedEvent from(ProcessInstance instance, Instant occurredAt) {
            return new ProcessInstanceResumedEvent(
                    UUID.randomUUID(),
                    INSTANCE_RESUMED,
                    occurredAt,
                    instance.tenantId().toString(),
                    instance.id()
            );
        }
    }

    public record ProcessTaskCreatedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID taskId,
            UUID instanceId,
            String nodeId,
            String nodeName,
            UUID assigneeId,
            CandidateType candidateType,
            List<UUID> candidateIds,
            Instant dueTime
    ) implements DomainEvent {

        public static ProcessTaskCreatedEvent from(TaskInstance task, Instant occurredAt) {
            return new ProcessTaskCreatedEvent(
                    UUID.randomUUID(),
                    TASK_CREATED,
                    occurredAt,
                    task.tenantId().toString(),
                    task.id(),
                    task.instanceId(),
                    task.nodeId(),
                    task.nodeName(),
                    task.assigneeId(),
                    task.candidateType(),
                    task.candidateIds(),
                    task.dueTime()
            );
        }
    }

    public record ProcessTaskClaimedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID taskId,
            UUID instanceId,
            UUID assigneeId,
            Instant claimTime
    ) implements DomainEvent {

        public static ProcessTaskClaimedEvent from(TaskInstance task, Instant occurredAt) {
            return new ProcessTaskClaimedEvent(
                    UUID.randomUUID(),
                    TASK_CLAIMED,
                    occurredAt,
                    task.tenantId().toString(),
                    task.id(),
                    task.instanceId(),
                    task.assigneeId(),
                    task.claimTime()
            );
        }
    }

    public record ProcessTaskCompletedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID taskId,
            UUID instanceId,
            String actionCode,
            Instant completedTime
    ) implements DomainEvent {

        public static ProcessTaskCompletedEvent from(TaskInstance task, String actionCode, Instant occurredAt) {
            return new ProcessTaskCompletedEvent(
                    UUID.randomUUID(),
                    TASK_COMPLETED_EVENT_TYPE,
                    occurredAt,
                    task.tenantId().toString(),
                    task.id(),
                    task.instanceId(),
                    actionCode,
                    task.completedTime() == null ? occurredAt : task.completedTime()
            );
        }
    }

    public record ProcessTaskTerminatedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID taskId,
            UUID instanceId,
            String reason
    ) implements DomainEvent {

        public static ProcessTaskTerminatedEvent from(TaskInstance task, String reason, Instant occurredAt) {
            return new ProcessTaskTerminatedEvent(
                    UUID.randomUUID(),
                    TASK_TERMINATED,
                    occurredAt,
                    task.tenantId().toString(),
                    task.id(),
                    task.instanceId(),
                    reason
            );
        }
    }

    public record ProcessTaskTransferredEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID taskId,
            UUID instanceId,
            UUID fromPersonId,
            UUID toPersonId
    ) implements DomainEvent {

        public static ProcessTaskTransferredEvent from(TaskInstance task, UUID fromPersonId, Instant occurredAt) {
            return new ProcessTaskTransferredEvent(
                    UUID.randomUUID(),
                    TASK_TRANSFERRED,
                    occurredAt,
                    task.tenantId().toString(),
                    task.id(),
                    task.instanceId(),
                    fromPersonId,
                    task.assigneeId()
            );
        }
    }
}
