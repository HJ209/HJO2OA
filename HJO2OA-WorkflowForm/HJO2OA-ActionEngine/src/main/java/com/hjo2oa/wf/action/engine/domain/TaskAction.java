package com.hjo2oa.wf.action.engine.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record TaskAction(
        UUID id,
        UUID taskId,
        UUID instanceId,
        String actionCode,
        ActionCategory category,
        String opinion,
        String targetNodeId,
        List<String> targetAssigneeIds,
        Map<String, Object> formDataPatch,
        ActionOperator operator,
        String idempotencyKey,
        ActionResultStatus resultStatus,
        Instant createdAt,
        String tenantId
) {

    public TaskAction {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(instanceId, "instanceId must not be null");
        actionCode = requireText(actionCode, "actionCode");
        Objects.requireNonNull(category, "category must not be null");
        targetAssigneeIds = targetAssigneeIds == null ? List.of() : List.copyOf(targetAssigneeIds);
        formDataPatch = formDataPatch == null ? Map.of() : Map.copyOf(formDataPatch);
        Objects.requireNonNull(operator, "operator must not be null");
        idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(resultStatus, "resultStatus must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
    }

    public static TaskAction create(
            TaskInstanceSnapshot task,
            ActionDefinition definition,
            ActionExecutionRequest request,
            ActionResultStatus resultStatus,
            Instant createdAt
    ) {
        return new TaskAction(
                UUID.randomUUID(),
                task.taskId(),
                task.instanceId(),
                definition.code(),
                definition.category(),
                normalize(request.opinion()),
                normalize(request.targetNodeId()),
                request.targetAssigneeIds(),
                request.formDataPatch(),
                request.operator(),
                request.idempotencyKey(),
                resultStatus,
                createdAt,
                task.tenantId()
        );
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
