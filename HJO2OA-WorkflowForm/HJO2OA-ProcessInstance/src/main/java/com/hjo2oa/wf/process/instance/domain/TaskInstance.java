package com.hjo2oa.wf.process.instance.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record TaskInstance(
        UUID id,
        UUID instanceId,
        String nodeId,
        String nodeName,
        TaskNodeType nodeType,
        UUID assigneeId,
        UUID assigneeOrgId,
        UUID assigneeDeptId,
        UUID assigneePositionId,
        CandidateType candidateType,
        List<UUID> candidateIds,
        MultiInstanceType multiInstanceType,
        String completionCondition,
        TaskInstanceStatus status,
        Instant claimTime,
        Instant completedTime,
        Instant dueTime,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {

    public TaskInstance {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(instanceId, "instanceId must not be null");
        nodeId = requireText(nodeId, "nodeId");
        nodeName = requireText(nodeName, "nodeName");
        Objects.requireNonNull(nodeType, "nodeType must not be null");
        candidateIds = normalizeIds(candidateIds);
        multiInstanceType = multiInstanceType == null ? MultiInstanceType.NONE : multiInstanceType;
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static TaskInstance create(
            UUID instanceId,
            String nodeId,
            String nodeName,
            TaskNodeType nodeType,
            UUID assigneeId,
            UUID assigneeOrgId,
            UUID assigneeDeptId,
            UUID assigneePositionId,
            CandidateType candidateType,
            List<UUID> candidateIds,
            MultiInstanceType multiInstanceType,
            String completionCondition,
            Instant dueTime,
            UUID tenantId,
            Instant now
    ) {
        TaskInstanceStatus initialStatus = assigneeId == null
                ? TaskInstanceStatus.CREATED
                : TaskInstanceStatus.CLAIMED;
        return new TaskInstance(
                UUID.randomUUID(),
                instanceId,
                nodeId,
                nodeName,
                nodeType,
                assigneeId,
                assigneeOrgId,
                assigneeDeptId,
                assigneePositionId,
                candidateType,
                candidateIds,
                multiInstanceType,
                completionCondition,
                initialStatus,
                assigneeId == null ? null : now,
                null,
                dueTime,
                tenantId,
                now,
                now
        );
    }

    public TaskInstance claim(UUID personId, UUID orgId, UUID deptId, UUID positionId, Instant now) {
        if (status != TaskInstanceStatus.CREATED) {
            throw new IllegalStateException("Only created task can be claimed");
        }
        return copy(personId, orgId, deptId, positionId, TaskInstanceStatus.CLAIMED, now, completedTime, now);
    }

    public TaskInstance complete(Instant now) {
        if (status != TaskInstanceStatus.CREATED && status != TaskInstanceStatus.CLAIMED) {
            throw new IllegalStateException("Task is not completable: " + status);
        }
        return copy(
                assigneeId,
                assigneeOrgId,
                assigneeDeptId,
                assigneePositionId,
                TaskInstanceStatus.COMPLETED,
                claimTime,
                now,
                now
        );
    }

    public TaskInstance transfer(UUID toPersonId, UUID toOrgId, UUID toDeptId, UUID toPositionId, Instant now) {
        if (status != TaskInstanceStatus.CREATED && status != TaskInstanceStatus.CLAIMED) {
            throw new IllegalStateException("Task is not transferable: " + status);
        }
        return copy(toPersonId, toOrgId, toDeptId, toPositionId, TaskInstanceStatus.CLAIMED, now, null, now);
    }

    public TaskInstance terminate(Instant now) {
        if (status == TaskInstanceStatus.COMPLETED || status == TaskInstanceStatus.TERMINATED) {
            return this;
        }
        return copy(
                assigneeId,
                assigneeOrgId,
                assigneeDeptId,
                assigneePositionId,
                TaskInstanceStatus.TERMINATED,
                claimTime,
                completedTime,
                now
        );
    }

    public boolean isOpen() {
        return status == TaskInstanceStatus.CREATED || status == TaskInstanceStatus.CLAIMED;
    }

    public ProcessInstanceViews.TaskInstanceView toView() {
        return new ProcessInstanceViews.TaskInstanceView(
                id,
                instanceId,
                nodeId,
                nodeName,
                nodeType,
                assigneeId,
                assigneeOrgId,
                assigneeDeptId,
                assigneePositionId,
                candidateType,
                candidateIds,
                multiInstanceType,
                completionCondition,
                status,
                claimTime,
                completedTime,
                dueTime,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    private TaskInstance copy(
            UUID nextAssigneeId,
            UUID nextOrgId,
            UUID nextDeptId,
            UUID nextPositionId,
            TaskInstanceStatus nextStatus,
            Instant nextClaimTime,
            Instant nextCompletedTime,
            Instant now
    ) {
        return new TaskInstance(
                id,
                instanceId,
                nodeId,
                nodeName,
                nodeType,
                nextAssigneeId,
                nextOrgId,
                nextDeptId,
                nextPositionId,
                candidateType,
                candidateIds,
                multiInstanceType,
                completionCondition,
                nextStatus,
                nextClaimTime,
                nextCompletedTime,
                dueTime,
                tenantId,
                createdAt,
                now
        );
    }

    private static List<UUID> normalizeIds(List<UUID> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<UUID> normalized = new LinkedHashSet<>();
        for (UUID value : values) {
            if (value != null) {
                normalized.add(value);
            }
        }
        return List.copyOf(normalized);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
