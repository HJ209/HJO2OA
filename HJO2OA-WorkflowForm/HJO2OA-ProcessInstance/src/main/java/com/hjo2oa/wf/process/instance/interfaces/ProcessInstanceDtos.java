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
            @Size(max = 64) String definitionCode,
            @Size(max = 128) String businessKey,
            @NotBlank @Size(max = 256) String title,
            @Size(max = 64) String category,
            @NotNull UUID initiatorId,
            @NotNull UUID initiatorOrgId,
            UUID initiatorDeptId,
            @NotNull UUID initiatorPositionId,
            UUID formMetadataId,
            @NotNull UUID formDataId,
            @Size(max = 64) String firstNodeId,
            @Size(max = 128) String firstNodeName,
            TaskNodeType firstNodeType,
            Map<String, Object> variables,
            @Valid List<TaskParticipantRequest> participants,
            MultiInstanceType multiInstanceType,
            @Size(max = 256) String completionCondition,
            Instant dueTime,
            UUID tenantId
    ) {

        ProcessInstanceCommands.StartProcessCommand toCommand(UUID resolvedTenantId, String idempotencyKey, String requestId) {
            return new ProcessInstanceCommands.StartProcessCommand(
                    definitionId,
                    definitionVersion,
                    definitionCode,
                    businessKey,
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
                    variables == null ? Map.of() : variables,
                    mapParticipants(participants),
                    multiInstanceType,
                    completionCondition,
                    dueTime,
                    resolvedTenantId,
                    idempotencyKey,
                    requestId
            );
        }
    }

    public record CompleteTaskRequest(
            @NotBlank @Size(max = 64) String actionCode,
            @Size(max = 128) String actionName,
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

        ProcessInstanceCommands.CompleteTaskCommand toCommand(UUID taskId, String idempotencyKey, String requestId) {
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
                    endProcess,
                    idempotencyKey,
                    requestId
            );
        }
    }

    public record ClaimTaskRequest(
            @NotNull UUID assigneeId,
            @NotNull UUID assigneeOrgId,
            UUID assigneeDeptId,
            @NotNull UUID assigneePositionId
    ) {

        ProcessInstanceCommands.ClaimTaskCommand toCommand(UUID taskId, String idempotencyKey, String requestId) {
            return new ProcessInstanceCommands.ClaimTaskCommand(
                    taskId,
                    assigneeId,
                    assigneeOrgId,
                    assigneeDeptId,
                    assigneePositionId,
                    idempotencyKey,
                    requestId
            );
        }
    }

    public record TransferTaskRequest(
            @NotNull UUID toAssigneeId,
            UUID toAssigneeOrgId,
            UUID toAssigneeDeptId,
            UUID toAssigneePositionId
    ) {

        ProcessInstanceCommands.TransferTaskCommand toCommand(UUID taskId, String idempotencyKey, String requestId) {
            return new ProcessInstanceCommands.TransferTaskCommand(
                    taskId,
                    toAssigneeId,
                    toAssigneeOrgId,
                    toAssigneeDeptId,
                    toAssigneePositionId,
                    idempotencyKey,
                    requestId
            );
        }
    }

    public record AddSignRequest(
            @NotNull UUID operatorId,
            @NotNull UUID operatorOrgId,
            @NotNull UUID operatorPositionId,
            @Valid List<TaskParticipantRequest> participants,
            @Size(max = 1024) String opinion
    ) {

        ProcessInstanceCommands.AddSignCommand toCommand(UUID taskId, String idempotencyKey, String requestId) {
            return new ProcessInstanceCommands.AddSignCommand(
                    taskId,
                    operatorId,
                    operatorOrgId,
                    operatorPositionId,
                    mapParticipants(participants),
                    opinion,
                    idempotencyKey,
                    requestId
            );
        }
    }

    public record ControlProcessRequest(
            @NotNull UUID operatorId,
            @Size(max = 512) String reason
    ) {

        ProcessInstanceCommands.TerminateProcessCommand toTerminateCommand(
                UUID instanceId,
                String idempotencyKey,
                String requestId
        ) {
            return new ProcessInstanceCommands.TerminateProcessCommand(
                    instanceId,
                    operatorId,
                    reason,
                    idempotencyKey,
                    requestId
            );
        }

        ProcessInstanceCommands.SuspendProcessCommand toSuspendCommand(
                UUID instanceId,
                String idempotencyKey,
                String requestId
        ) {
            return new ProcessInstanceCommands.SuspendProcessCommand(
                    instanceId,
                    operatorId,
                    reason,
                    idempotencyKey,
                    requestId
            );
        }

        ProcessInstanceCommands.ResumeProcessCommand toResumeCommand(
                UUID instanceId,
                String idempotencyKey,
                String requestId
        ) {
            return new ProcessInstanceCommands.ResumeProcessCommand(
                    instanceId,
                    operatorId,
                    idempotencyKey,
                    requestId
            );
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

    public record NodeHistoryResponse(
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

    public record VariableHistoryResponse(
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

    public record InstanceDetailResponse(
            ProcessInstanceResponse instance,
            List<TaskInstanceResponse> tasks,
            List<TaskActionResponse> actions,
            List<NodeHistoryResponse> nodeHistory,
            List<VariableHistoryResponse> variableHistory
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
