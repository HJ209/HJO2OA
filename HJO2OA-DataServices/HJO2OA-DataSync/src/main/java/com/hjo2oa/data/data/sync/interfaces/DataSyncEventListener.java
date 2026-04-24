package com.hjo2oa.data.data.sync.interfaces;

import com.hjo2oa.data.common.domain.event.DataEventTypes;
import com.hjo2oa.data.data.sync.application.SyncExecutionApplicationService;
import com.hjo2oa.data.data.sync.application.SyncTaskApplicationService;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DataSyncEventListener {

    private final SyncTaskApplicationService taskApplicationService;
    private final SyncExecutionApplicationService executionApplicationService;
    private final DomainEventPayloadExtractor payloadExtractor;

    public DataSyncEventListener(
            SyncTaskApplicationService taskApplicationService,
            SyncExecutionApplicationService executionApplicationService,
            DomainEventPayloadExtractor payloadExtractor
    ) {
        this.taskApplicationService = taskApplicationService;
        this.executionApplicationService = executionApplicationService;
        this.payloadExtractor = payloadExtractor;
    }

    @EventListener
    public void onDomainEvent(DomainEvent event) {
        if (event == null || event.eventType() == null) {
            return;
        }
        String eventType = event.eventType();
        if (DataEventTypes.DATA_CONNECTOR_UPDATED.equals(eventType)) {
            UUID connectorId = payloadExtractor.uuid(event, "connectorId");
            if (connectorId != null) {
                taskApplicationService.refreshConnectorDependency(connectorId, payloadExtractor.text(event, "status"));
            }
            return;
        }
        if (eventType.startsWith("infra.scheduler.")) {
            Map<String, Object> payload = payload(event);
            payload.putIfAbsent("eventId", event.eventId().toString());
            payload.putIfAbsent("occurredAt", event.occurredAt().toString());
            executionApplicationService.onSchedulerTrigger(payloadExtractor.text(event, "jobCode"), payload);
            return;
        }
        if (eventType.startsWith("org.")
                || eventType.startsWith("process.")
                || eventType.startsWith("content.")
                || eventType.startsWith("msg.")) {
            executionApplicationService.onBusinessEvent(event, payload(event));
        }
    }

    private Map<String, Object> payload(DomainEvent event) {
        return new java.util.LinkedHashMap<>(payloadExtractor.payload(event));
    }
}
