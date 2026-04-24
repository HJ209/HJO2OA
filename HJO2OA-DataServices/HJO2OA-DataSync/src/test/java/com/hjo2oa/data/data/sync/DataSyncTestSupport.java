package com.hjo2oa.data.data.sync;

import com.hjo2oa.data.common.application.event.DataDomainEventPublisher;
import com.hjo2oa.data.common.domain.event.AbstractDataDomainEvent;
import com.hjo2oa.data.common.domain.event.DataDomainEvent;
import com.hjo2oa.data.data.sync.application.CreateSyncExchangeTaskCommand;
import com.hjo2oa.data.data.sync.application.SyncMappingRuleDraft;
import com.hjo2oa.data.data.sync.domain.CheckpointMode;
import com.hjo2oa.data.data.sync.domain.ConflictStrategy;
import com.hjo2oa.data.data.sync.domain.SyncCheckpointConfig;
import com.hjo2oa.data.data.sync.domain.SyncCompensationPolicy;
import com.hjo2oa.data.data.sync.domain.SyncMode;
import com.hjo2oa.data.data.sync.domain.SyncPayloadRecord;
import com.hjo2oa.data.data.sync.domain.SyncReconciliationPolicy;
import com.hjo2oa.data.data.sync.domain.SyncRetryPolicy;
import com.hjo2oa.data.data.sync.domain.SyncScheduleConfig;
import com.hjo2oa.data.data.sync.domain.SyncTaskType;
import com.hjo2oa.data.data.sync.domain.SyncTriggerConfig;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DataSyncTestSupport {

    public static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID SOURCE_CONNECTOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID TARGET_CONNECTOR_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private DataSyncTestSupport() {
    }

    public static CreateSyncExchangeTaskCommand createTaskCommand(
            String code,
            SyncMode syncMode,
            CheckpointMode checkpointMode,
            SyncCheckpointConfig checkpointConfig,
            SyncTriggerConfig triggerConfig,
            SyncRetryPolicy retryPolicy,
            SyncCompensationPolicy compensationPolicy,
            SyncReconciliationPolicy reconciliationPolicy,
            SyncScheduleConfig scheduleConfig,
            ConflictStrategy conflictStrategy
    ) {
        return new CreateSyncExchangeTaskCommand(
                TENANT_ID,
                code,
                code + "-name",
                code + "-description",
                SyncTaskType.EXPORT,
                syncMode,
                SOURCE_CONNECTOR_ID,
                TARGET_CONNECTOR_ID,
                checkpointMode,
                checkpointConfig,
                triggerConfig,
                retryPolicy,
                compensationPolicy,
                reconciliationPolicy,
                scheduleConfig,
                defaultMappingRules(conflictStrategy)
        );
    }

    public static List<SyncMappingRuleDraft> defaultMappingRules(ConflictStrategy conflictStrategy) {
        return List.of(
                new SyncMappingRuleDraft("id", "id", Map.of(), conflictStrategy, true, 0),
                new SyncMappingRuleDraft("name", "name", Map.of("operation", "TRIM"), conflictStrategy, false, 1)
        );
    }

    public static SyncPayloadRecord payloadRecord(
            String recordKey,
            String checkpointToken,
            String eventId,
            Instant occurredAt,
            Map<String, Object> payload
    ) {
        return new SyncPayloadRecord(recordKey, checkpointToken, eventId, occurredAt, payload);
    }

    public static final class CollectingDataDomainEventPublisher implements DataDomainEventPublisher {

        private final List<DataDomainEvent> events = new ArrayList<>();

        @Override
        public void publish(DataDomainEvent event) {
            events.add(event);
        }

        public List<DataDomainEvent> events() {
            return List.copyOf(events);
        }

        public <T extends DataDomainEvent> List<T> eventsOfType(Class<T> eventType) {
            return events.stream()
                    .filter(eventType::isInstance)
                    .map(eventType::cast)
                    .toList();
        }
    }

    public static final class TestDataEvent extends AbstractDataDomainEvent {

        public TestDataEvent(UUID eventId, Instant occurredAt, String eventType, Map<String, Object> payload) {
            super(
                    eventId,
                    eventType,
                    occurredAt,
                    TENANT_ID.toString(),
                    "test-module",
                    "test-aggregate",
                    "system",
                    payload
            );
        }
    }
}
