package com.hjo2oa.wf.process.instance.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record TaskAction(
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

    public TaskAction {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(instanceId, "instanceId must not be null");
        actionCode = requireText(actionCode, "actionCode");
        actionName = requireText(actionName, "actionName");
        Objects.requireNonNull(operatorId, "operatorId must not be null");
        Objects.requireNonNull(operatorOrgId, "operatorOrgId must not be null");
        Objects.requireNonNull(operatorPositionId, "operatorPositionId must not be null");
        formDataPatch = formDataPatch == null || formDataPatch.isEmpty() ? Map.of() : Map.copyOf(formDataPatch);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static TaskAction record(
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
            Instant now
    ) {
        return new TaskAction(
                UUID.randomUUID(),
                taskId,
                instanceId,
                actionCode,
                actionName,
                operatorId,
                operatorOrgId,
                operatorPositionId,
                opinion,
                targetNodeId,
                formDataPatch,
                now
        );
    }

    public ProcessInstanceViews.TaskActionView toView() {
        return new ProcessInstanceViews.TaskActionView(
                id,
                taskId,
                instanceId,
                actionCode,
                actionName,
                operatorId,
                operatorOrgId,
                operatorPositionId,
                opinion,
                targetNodeId,
                formDataPatch,
                createdAt
        );
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
