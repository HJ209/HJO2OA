package com.hjo2oa.wf.action.engine.domain;

import java.util.Objects;
import java.util.UUID;

public record TaskInstanceSnapshot(
        UUID taskId,
        UUID instanceId,
        String assigneeId,
        TaskStatus status,
        String tenantId
) {

    public TaskInstanceSnapshot {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(instanceId, "instanceId must not be null");
        assigneeId = normalize(assigneeId);
        Objects.requireNonNull(status, "status must not be null");
        tenantId = requireText(tenantId, "tenantId");
    }

    public boolean pending() {
        return status == TaskStatus.PENDING;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
