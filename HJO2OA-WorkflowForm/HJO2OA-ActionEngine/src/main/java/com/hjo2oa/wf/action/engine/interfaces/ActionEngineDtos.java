package com.hjo2oa.wf.action.engine.interfaces;

import com.hjo2oa.wf.action.engine.application.ActionEngineCommands;
import com.hjo2oa.wf.action.engine.domain.ActionCategory;
import com.hjo2oa.wf.action.engine.domain.ActionResultStatus;
import com.hjo2oa.wf.action.engine.domain.RouteTarget;
import com.hjo2oa.wf.action.engine.domain.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ActionEngineDtos {

    private ActionEngineDtos() {
    }

    public record ExecuteActionRequest(
            @NotBlank String actionCode,
            String opinion,
            String targetNodeId,
            List<String> targetAssigneeIds,
            Map<String, Object> formDataPatch,
            @NotBlank String operatorAccountId,
            String operatorPersonId,
            String operatorPositionId,
            String operatorOrgId
    ) {

        ActionEngineCommands.ExecuteActionCommand toCommand(UUID taskId, String idempotencyKey) {
            return new ActionEngineCommands.ExecuteActionCommand(
                    taskId,
                    actionCode,
                    opinion,
                    targetNodeId,
                    targetAssigneeIds,
                    formDataPatch,
                    operatorAccountId,
                    operatorPersonId,
                    operatorPositionId,
                    operatorOrgId,
                    idempotencyKey
            );
        }
    }

    public record ActionDefinitionResponse(
            UUID id,
            String code,
            String name,
            ActionCategory category,
            RouteTarget routeTarget,
            boolean requireOpinion,
            boolean requireTarget,
            Map<String, Object> uiConfig,
            String tenantId
    ) {
    }

    public record TaskActionResponse(
            UUID id,
            UUID taskId,
            UUID instanceId,
            String actionCode,
            ActionCategory category,
            String opinion,
            String targetNodeId,
            List<String> targetAssigneeIds,
            Map<String, Object> formDataPatch,
            String operatorAccountId,
            String operatorPersonId,
            String operatorPositionId,
            String operatorOrgId,
            String idempotencyKey,
            ActionResultStatus resultStatus,
            Instant createdAt,
            String tenantId
    ) {
    }

    public record ExecuteActionResponse(
            TaskActionResponse action,
            TaskStatus taskStatus
    ) {
    }
}
