package com.hjo2oa.wf.process.instance.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProcessInstanceViews {

    private ProcessInstanceViews() {
    }

    public record ProcessInstanceView(
            UUID id,
            UUID definitionId,
            int definitionVersion,
            String definitionCode,
            String businessKey,
            String title,
            String category,
            UUID initiatorId,
            UUID initiatorOrgId,
            UUID initiatorDeptId,
            UUID initiatorPositionId,
            UUID formMetadataId,
            UUID formDataId,
            List<String> currentNodes,
            ProcessInstanceStatus status,
            Instant startTime,
            Instant endTime,
            UUID tenantId,
            String idempotencyKey,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record TaskInstanceView(
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
    }

    public record TaskActionView(
            UUID id,
            UUID taskId,
            UUID instanceId,
            String actionCode,
            String actionName,
            UUID operatorId,
            UUID operatorOrgId,
            UUID operatorPositionId,
            String opinion,
            String targetNodeId,
            Map<String, Object> formDataPatch,
            Instant createdAt
    ) {
    }

    public record InstanceDetailView(
            ProcessInstanceView instance,
            List<TaskInstanceView> tasks,
            List<TaskActionView> actions,
            List<NodeHistoryView> nodeHistory,
            List<VariableHistoryView> variableHistory
    ) {
    }

    public record NodeHistoryView(
            UUID id,
            UUID instanceId,
            UUID taskId,
            String nodeId,
            String nodeName,
            TaskNodeType nodeType,
            String status,
            String actionCode,
            UUID operatorId,
            Instant occurredAt,
            UUID tenantId
    ) {
    }

    public record VariableHistoryView(
            UUID id,
            UUID instanceId,
            UUID taskId,
            String variableName,
            String oldValue,
            String newValue,
            UUID operatorId,
            Instant occurredAt,
            UUID tenantId
    ) {
    }
}
