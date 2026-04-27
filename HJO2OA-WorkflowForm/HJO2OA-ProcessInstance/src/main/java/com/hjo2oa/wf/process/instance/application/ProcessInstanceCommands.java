package com.hjo2oa.wf.process.instance.application;

import com.hjo2oa.wf.process.instance.domain.CandidateType;
import com.hjo2oa.wf.process.instance.domain.MultiInstanceType;
import com.hjo2oa.wf.process.instance.domain.TaskNodeType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProcessInstanceCommands {

    private ProcessInstanceCommands() {
    }

    public record StartProcessCommand(
            UUID definitionId,
            int definitionVersion,
            String definitionCode,
            String title,
            String category,
            UUID initiatorId,
            UUID initiatorOrgId,
            UUID initiatorDeptId,
            UUID initiatorPositionId,
            UUID formMetadataId,
            UUID formDataId,
            String firstNodeId,
            String firstNodeName,
            TaskNodeType firstNodeType,
            List<TaskParticipantCommand> participants,
            MultiInstanceType multiInstanceType,
            String completionCondition,
            Instant dueTime,
            UUID tenantId
    ) {
    }

    public record TaskParticipantCommand(
            UUID assigneeId,
            UUID assigneeOrgId,
            UUID assigneeDeptId,
            UUID assigneePositionId,
            CandidateType candidateType,
            List<UUID> candidateIds
    ) {
    }

    public record CompleteTaskCommand(
            UUID taskId,
            String actionCode,
            String actionName,
            UUID operatorId,
            UUID operatorOrgId,
            UUID operatorPositionId,
            String opinion,
            String targetNodeId,
            String targetNodeName,
            TaskNodeType targetNodeType,
            List<TaskParticipantCommand> nextParticipants,
            MultiInstanceType nextMultiInstanceType,
            String nextCompletionCondition,
            Instant nextDueTime,
            List<RouteConditionCommand> routeConditions,
            Map<String, Object> formDataPatch,
            boolean endProcess
    ) {
    }

    public record ClaimTaskCommand(
            UUID taskId,
            UUID assigneeId,
            UUID assigneeOrgId,
            UUID assigneeDeptId,
            UUID assigneePositionId
    ) {
    }

    public record TransferTaskCommand(
            UUID taskId,
            UUID toAssigneeId,
            UUID toAssigneeOrgId,
            UUID toAssigneeDeptId,
            UUID toAssigneePositionId
    ) {
    }

    public record TerminateProcessCommand(
            UUID instanceId,
            String reason
    ) {
    }

    public record RouteConditionCommand(
            String targetNodeId,
            String targetNodeName,
            TaskNodeType targetNodeType,
            String field,
            String operator,
            String expectedValue,
            boolean defaultRoute,
            List<TaskParticipantCommand> participants
    ) {
    }
}
