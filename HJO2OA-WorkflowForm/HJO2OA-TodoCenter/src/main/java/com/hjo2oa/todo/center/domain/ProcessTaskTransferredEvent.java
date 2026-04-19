package com.hjo2oa.todo.center.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProcessTaskTransferredEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String taskId,
        String instanceId,
        String fromAssigneeId,
        String toAssigneeId
) implements DomainEvent {

    public static final String EVENT_TYPE = "process.task.transferred";

    public ProcessTaskTransferredEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        taskId = requireText(taskId, "taskId");
        instanceId = requireText(instanceId, "instanceId");
        fromAssigneeId = requireText(fromAssigneeId, "fromAssigneeId");
        toAssigneeId = requireText(toAssigneeId, "toAssigneeId");
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
