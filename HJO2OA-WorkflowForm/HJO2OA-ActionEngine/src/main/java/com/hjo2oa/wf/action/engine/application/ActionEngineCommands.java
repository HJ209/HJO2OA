package com.hjo2oa.wf.action.engine.application;

import com.hjo2oa.wf.action.engine.domain.ActionExecutionRequest;
import com.hjo2oa.wf.action.engine.domain.ActionOperator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ActionEngineCommands {

    private ActionEngineCommands() {
    }

    public record ExecuteActionCommand(
            UUID taskId,
            String actionCode,
            String opinion,
            String targetNodeId,
            List<String> targetAssigneeIds,
            Map<String, Object> formDataPatch,
            String operatorAccountId,
            String operatorPersonId,
            String operatorPositionId,
            String operatorOrgId,
            String idempotencyKey
    ) {

        public ActionExecutionRequest toExecutionRequest() {
            return new ActionExecutionRequest(
                    actionCode,
                    opinion,
                    targetNodeId,
                    targetAssigneeIds,
                    formDataPatch,
                    new ActionOperator(operatorAccountId, operatorPersonId, operatorPositionId, operatorOrgId),
                    idempotencyKey
            );
        }
    }
}
