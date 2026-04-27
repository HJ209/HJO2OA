package com.hjo2oa.wf.process.instance.interfaces;

import com.hjo2oa.wf.process.instance.application.ProcessInstanceCommands;
import com.hjo2oa.wf.process.instance.domain.CandidateType;
import com.hjo2oa.wf.process.instance.domain.MultiInstanceType;
import com.hjo2oa.wf.process.instance.domain.ProcessInstanceStatus;
import com.hjo2oa.wf.process.instance.domain.TaskInstanceStatus;
import com.hjo2oa.wf.process.instance.domain.TaskNodeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProcessInstanceDtos {

    private ProcessInstanceDtos() {
    }

    public record StartProcessRequest(
            @NotNull UUID definitionId,
            int definitionVersion,
            @NotBlank @Size(max = 64) String definitionCode,
            @NotBlank @Size(max = 256) String title,
            @Size(max = 64) String category,
            @NotNull UUID initiatorId,
            @NotNull UUID initiatorOrgId,
            UUID initiatorDeptId,
            @NotNull UUID initiatorPositionId,
            @NotNull UUID formMetadataId,
            @NotNull UUID formDataId,
            @NotBlank @Size(max = 64) String firstNodeId,
            @Size(max = 128) String firstNodeName,
            TaskNodeType firstNodeType,
            @Valid List<TaskParticipantRequest> participants,
            MultiInstanceType multiInstanceType,
            @Size(max = 256) String completionCondition,
            Instant dueTime,
            @NotNull UUID tenantId
    ) {

        ProcessInstanceCommands.StartProcessCommand toCommand() {
            return new ProcessInstanceCommands.StartProcessCommand(
                    definitionId,
                    definitionVersion,
                    definitionCode,
                    title,
                    category,
                    initiatorId,
                    initiatorOrgId,
                    initiatorDeptId,
                    initiatorPositionId,
                    formMetadataId,
                    formDataId,
                    firstNodeId,
                    firstNodeName,
                    firstNodeType,
                    mapParticipants(participants),
                    multiInstanceType,
                    completionCondition,
                    dueTime,
                    tenantId
            );
        }
    }

    public record CompleteTaskRequest(
            @NotBlank @Size(max = 64) String actionCode,
            @NotBlank @Size(max = 128) String actionName,
            @NotNull UUID operatorId,
            @NotNull UUID operatorOrgId,
            @NotNull UUID operatorPositionId,
            @Size(max = 1024) String opinion,
            @Size(max = 64) String targetNodeId,
            @Size(max = 128) String targetNodeName,
            TaskNodeType targetNodeType,
            @Valid List<TaskParticipantRequest> nextParticipants,
            MultiInstanceType nextMultiInstanceType,
            @Size(max = 256) String nextCompletionCondition,
            Instant nextDueTime,
            @Valid List<RouteConditionRequest> routeConditions,
            Map<String, Object> formDataPatch,
            boolean endProcess
    ) {

        ProcessInstanceCommands.CompleteTaskCommand toCommand(UUID taskId) {
            return new ProcessInstanceCommands.CompleteTaskCommand(
                    taskId,
                    actionCode,
                    actionName,
                    operatorId,
                    operatorOrgId,
                    operatorPositionId,
                    opinion,
                    targetNodeId,
                    targetNodeName,
                    targetNodeType,
                    mapParticipants(nextParticipants),
                    nextMultiInstanceType,
                    nextCompletionCondition,
                    nextDueTime,
                    mapRoutes(routeConditions),
                    formDataPatch,
                    endProcess
            );
        }
    }

    public record ClaimTaskRequest(
            @NotNull UUID assigneeId,
            @NotNull UUID assigneeOrgId,
            UUID assigneeDeptId,
            @NotNull UUID assigneePositionId
    ) {

        ProcessInstanceCommands.ClaimTaskCommand toCommand(UUID taskId) {
            return new ProcessInstanceCommands.ClaimTaskCommand(
                    taskId,
                    assigneeId,
                    assigneeOrgId,
                    assigneeDeptId,
                    assigneePositionId
            );
        }
    }

    public record TransferTaskRequest(
            @NotNull UUID toAssigneeId,
            @NotNull UUID toAssigneeOrgId,
            UUID toAssigneeDeptId,
            @NotNull UUID toAssigneePositionId
    ) {

        ProcessInstanceCommands.TransferTaskCommand toCommand(UUID taskId) {
            return new ProcessInstanceCommands.TransferTaskCommand(
                    taskId,
                    toAssigneeId,
                    toAssigneeOrgId,
                    toAssigneeDeptId,
                    toAssigneePositionId
            );
        }
    }

    public record TerminateProcessRequest(
            @Size(max = 512) String reason
    ) {

        ProcessInstanceCommands.TerminateProcessCommand toCommand(UUID instanceId) {
            return new ProcessInstanceCommands.TerminateProcessCommand(instanceId, reason);
        }
    }

    public record TaskParticipantRequest(
            UUID assigneeId,
            UUID assigneeOrgId,
            UUID assigneeDeptId,
            UUID assigneePositionId,
            CandidateType candidateType,
            List<UUID> candidateIds
    ) {

        ProcessInstanceCommands.TaskParticipantCommand toCommand() {
            return new ProcessInstanceCommands.TaskParticipantCommand(
                    assigneeId,
                    assigneeOrgId,
                    assigneeDeptId,
                    assigneePositionId,
                    candidateType,
                    candidateIds
            );
        }
    }

    public record RouteConditionRequest(
            @Size(max = 64) String targetNodeId,
            @Size(max = 128) String targetNodeName,
            TaskNodeType targetNodeType,
            @Size(max = 128) String field,
            @Size(max = 16) String operator,
            @Size(max = 256) String expectedValue,
            boolean defaultRoute,
            @Valid List<TaskParticipantRequest> participants
    ) {

        ProcessInstanceCommands.RouteConditionCommand toCommand() {
            return new ProcessInstanceCommands.RouteConditionCommand(
                    targetNodeId,
                    targetNodeName,
                    targetNodeType,
                    field,
                    operator,
                    expectedValue,
                    defaultRoute,
                    mapParticipants(participants)
            );
        }
    }

    public record ProcessInstanceResponse(
            UUID id,
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
            List<String> currentNodes,
            ProcessInstanceStatus status,
            Instant startTime,
            Instant endTime,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record TaskInstanceResponse(
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

    public record TaskActionResponse(
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

    public record InstanceDetailResponse(
            ProcessInstanceResponse instance,
            List<TaskInstanceResponse> tasks,
            List<TaskActionResponse> actions
    ) {
    }

    private static List<ProcessInstanceCommands.TaskParticipantCommand> mapParticipants(
            List<TaskParticipantRequest> participants
    ) {
        if (participants == null) {
            return List.of();
        }
        return participants.stream().map(TaskParticipantRequest::toCommand).toList();
    }

    private static List<ProcessInstanceCommands.RouteConditionCommand> mapRoutes(
            List<RouteConditionRequest> routes
    ) {
        if (routes == null) {
            return List.of();
        }
        return routes.stream().map(RouteConditionRequest::toCommand).toList();
    }
}
