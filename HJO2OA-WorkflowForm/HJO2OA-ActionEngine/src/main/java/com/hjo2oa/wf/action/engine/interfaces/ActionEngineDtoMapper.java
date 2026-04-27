package com.hjo2oa.wf.action.engine.interfaces;

import com.hjo2oa.wf.action.engine.domain.ActionDefinition;
import com.hjo2oa.wf.action.engine.domain.ActionExecutionResult;
import com.hjo2oa.wf.action.engine.domain.TaskAction;
import org.springframework.stereotype.Component;

@Component
public class ActionEngineDtoMapper {

    public ActionEngineDtos.ActionDefinitionResponse toActionDefinitionResponse(ActionDefinition definition) {
        return new ActionEngineDtos.ActionDefinitionResponse(
                definition.id(),
                definition.code(),
                definition.name(),
                definition.category(),
                definition.routeTarget(),
                definition.requireOpinion(),
                definition.requireTarget(),
                definition.uiConfig(),
                definition.tenantId()
        );
    }

    public ActionEngineDtos.TaskActionResponse toTaskActionResponse(TaskAction action) {
        return new ActionEngineDtos.TaskActionResponse(
                action.id(),
                action.taskId(),
                action.instanceId(),
                action.actionCode(),
                action.category(),
                action.opinion(),
                action.targetNodeId(),
                action.targetAssigneeIds(),
                action.formDataPatch(),
                action.operator().accountId(),
                action.operator().personId(),
                action.operator().positionId(),
                action.operator().orgId(),
                action.idempotencyKey(),
                action.resultStatus(),
                action.createdAt(),
                action.tenantId()
        );
    }

    public ActionEngineDtos.ExecuteActionResponse toExecuteActionResponse(ActionExecutionResult result) {
        return new ActionEngineDtos.ExecuteActionResponse(
                toTaskActionResponse(result.taskAction()),
                result.taskStatus()
        );
    }
}
