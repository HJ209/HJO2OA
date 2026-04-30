package com.hjo2oa.infra.event.bus.interfaces;

import com.hjo2oa.infra.event.bus.application.EventBusManagementCommands.EventOperationCommand;
import com.hjo2oa.infra.event.bus.application.EventBusManagementCommands.OperatorContext;
import com.hjo2oa.infra.event.bus.application.EventBusManagementCommands.ReplayEventsCommand;
import com.hjo2oa.infra.event.bus.infrastructure.amqp.EventOutboxQuery;
import com.hjo2oa.infra.event.bus.infrastructure.amqp.EventOutboxStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

public final class EventBusDtos {

    private EventBusDtos() {
    }

    public record EventOperationRequest(
            @NotBlank String reason,
            UUID operatorAccountId,
            UUID operatorPersonId
    ) {

        EventOperationCommand toCommand(UUID eventId, OperatorContext operatorContext) {
            return new EventOperationCommand(
                    eventId,
                    reason,
                    new OperatorContext(
                            operatorAccountId == null ? operatorContext.operatorAccountId() : operatorAccountId,
                            operatorPersonId == null ? operatorContext.operatorPersonId() : operatorPersonId,
                            operatorContext.requestId(),
                            operatorContext.idempotencyKey()
                    )
            );
        }
    }

    public record ReplayRequest(
            UUID eventId,
            String eventType,
            String aggregateType,
            String aggregateId,
            String tenantId,
            String traceId,
            String status,
            Instant occurredFrom,
            Instant occurredTo,
            @NotBlank String reason,
            UUID operatorAccountId,
            UUID operatorPersonId
    ) {

        ReplayEventsCommand toCommand(OperatorContext operatorContext) {
            return new ReplayEventsCommand(
                    new EventOutboxQuery(
                            eventId,
                            eventType,
                            aggregateType,
                            aggregateId,
                            tenantId,
                            traceId,
                            parseStatus(status),
                            occurredFrom,
                            occurredTo,
                            1,
                            500
                    ),
                    reason,
                    new OperatorContext(
                            operatorAccountId == null ? operatorContext.operatorAccountId() : operatorAccountId,
                            operatorPersonId == null ? operatorContext.operatorPersonId() : operatorPersonId,
                            operatorContext.requestId(),
                            operatorContext.idempotencyKey()
                    )
            );
        }
    }

    static EventOutboxStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return EventOutboxStatus.valueOf(status.trim().toUpperCase());
    }
}
