package com.hjo2oa.infra.event.bus.application;

import com.hjo2oa.infra.event.bus.infrastructure.amqp.EventOutboxQuery;
import java.util.Objects;
import java.util.UUID;

public final class EventBusManagementCommands {

    private EventBusManagementCommands() {
    }

    public record OperatorContext(
            UUID operatorAccountId,
            UUID operatorPersonId,
            String requestId,
            String idempotencyKey
    ) {
    }

    public record EventOperationCommand(
            UUID eventId,
            String reason,
            OperatorContext operatorContext
    ) {

        public EventOperationCommand {
            Objects.requireNonNull(eventId, "eventId must not be null");
            reason = requireReason(reason);
            Objects.requireNonNull(operatorContext, "operatorContext must not be null");
        }
    }

    public record ReplayEventsCommand(
            EventOutboxQuery query,
            String reason,
            OperatorContext operatorContext
    ) {

        public ReplayEventsCommand {
            Objects.requireNonNull(query, "query must not be null");
            reason = requireReason(reason);
            Objects.requireNonNull(operatorContext, "operatorContext must not be null");
        }
    }

    private static String requireReason(String reason) {
        Objects.requireNonNull(reason, "reason must not be null");
        String normalized = reason.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        return normalized;
    }
}
