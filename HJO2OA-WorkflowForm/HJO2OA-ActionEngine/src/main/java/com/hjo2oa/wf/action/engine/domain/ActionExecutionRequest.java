package com.hjo2oa.wf.action.engine.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ActionExecutionRequest(
        String actionCode,
        String opinion,
        String targetNodeId,
        List<String> targetAssigneeIds,
        Map<String, Object> formDataPatch,
        ActionOperator operator,
        String idempotencyKey
) {

    public ActionExecutionRequest {
        actionCode = requireText(actionCode, "actionCode");
        targetAssigneeIds = targetAssigneeIds == null ? List.of() : List.copyOf(targetAssigneeIds);
        formDataPatch = formDataPatch == null ? Map.of() : Map.copyOf(formDataPatch);
        Objects.requireNonNull(operator, "operator must not be null");
        idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
    }

    public boolean hasOpinion() {
        return opinion != null && !opinion.isBlank();
    }

    public boolean hasTarget() {
        return (targetNodeId != null && !targetNodeId.isBlank()) || !targetAssigneeIds.isEmpty();
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
