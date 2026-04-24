package com.hjo2oa.data.data.sync.infrastructure;

import com.hjo2oa.data.data.sync.domain.CompensationAction;
import com.hjo2oa.data.data.sync.domain.ConnectorDependencyStatus;
import com.hjo2oa.data.data.sync.domain.ConflictStrategy;
import com.hjo2oa.data.data.sync.domain.DifferenceStatus;
import com.hjo2oa.data.data.sync.domain.DifferenceType;
import com.hjo2oa.data.data.sync.domain.ExecutionTriggerType;
import com.hjo2oa.data.data.sync.domain.ReconciliationStatus;
import com.hjo2oa.data.data.sync.domain.SyncCompensationDecision;
import com.hjo2oa.data.data.sync.domain.SyncCompensationResult;
import com.hjo2oa.data.data.sync.domain.SyncConnectorGateway;
import com.hjo2oa.data.data.sync.domain.SyncDifferenceItem;
import com.hjo2oa.data.data.sync.domain.SyncExchangeTask;
import com.hjo2oa.data.data.sync.domain.SyncMappedRecord;
import com.hjo2oa.data.data.sync.domain.SyncMappingRule;
import com.hjo2oa.data.data.sync.domain.SyncPayloadRecord;
import com.hjo2oa.data.data.sync.domain.SyncPullBatch;
import com.hjo2oa.data.data.sync.domain.SyncPushResult;
import com.hjo2oa.data.data.sync.domain.SyncReconciliationResult;
import com.hjo2oa.data.data.sync.domain.SyncResultSummary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class InMemorySyncConnectorGateway implements SyncConnectorGateway {

    private final ConcurrentMap<UUID, String> connectorStatuses = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ConcurrentMap<String, ConnectorRecord>> connectorRecords = new ConcurrentHashMap<>();

    @Override
    public ConnectorDependencyStatus connectorStatus(UUID connectorId) {
        String rawStatus = connectorStatuses.get(connectorId);
        if (rawStatus == null || rawStatus.isBlank() || "ACTIVE".equalsIgnoreCase(rawStatus)) {
            return ConnectorDependencyStatus.READY;
        }
        return ConnectorDependencyStatus.SOURCE_UNAVAILABLE;
    }

    @Override
    public void updateConnectorStatus(UUID connectorId, String connectorStatus) {
        connectorStatuses.put(connectorId, connectorStatus == null ? "ACTIVE" : connectorStatus.trim().toUpperCase());
    }

    @Override
    public SyncPullBatch pull(
            SyncExchangeTask task,
            String startCheckpoint,
            ExecutionTriggerType triggerType,
            Map<String, Object> triggerContext
    ) {
        List<ConnectorRecord> records = listRecords(task.sourceConnectorId());
        String expectedEventId = stringValue(triggerContext, "eventId");
        List<SyncPayloadRecord> selected = records.stream()
                .filter(record -> matchesTrigger(task, triggerType, expectedEventId, startCheckpoint, record))
                .sorted(Comparator.comparing(ConnectorRecord::occurredAt).thenComparing(ConnectorRecord::recordKey))
                .map(record -> new SyncPayloadRecord(
                        record.recordKey(),
                        record.checkpointToken(),
                        record.eventId(),
                        record.occurredAt(),
                        record.payload()
                ))
                .toList();
        String nextCheckpoint = selected.isEmpty()
                ? startCheckpoint
                : selected.get(selected.size() - 1).checkpointToken();
        Map<String, Object> sourceContext = Map.of("sourceCount", selected.size());
        return new SyncPullBatch(selected, nextCheckpoint, sourceContext);
    }

    @Override
    public SyncPushResult push(SyncExchangeTask task, List<SyncMappedRecord> mappedRecords) {
        ConcurrentMap<String, ConnectorRecord> targetStore = connectorRecords.computeIfAbsent(
                task.targetConnectorId(),
                ignored -> new ConcurrentHashMap<>()
        );
        long inserted = 0;
        long updated = 0;
        long skipped = 0;
        List<SyncDifferenceItem> differences = new ArrayList<>();
        Instant now = Instant.now();

        for (SyncMappedRecord mappedRecord : mappedRecords) {
            ConnectorRecord existing = targetStore.get(mappedRecord.recordKey());
            if (existing == null) {
                targetStore.put(mappedRecord.recordKey(), ConnectorRecord.from(mappedRecord));
                inserted++;
                continue;
            }

            Map<String, Object> mergedPayload = new LinkedHashMap<>(existing.payload());
            boolean changed = false;
            boolean producedDifference = false;
            for (SyncMappingRule rule : task.sortedMappingRules()) {
                Object expectedValue = mappedRecord.payload().get(rule.targetField());
                Object actualValue = existing.payload().get(rule.targetField());
                if (Objects.equals(expectedValue, actualValue)) {
                    continue;
                }
                switch (rule.conflictStrategy()) {
                    case OVERWRITE -> {
                        mergedPayload.put(rule.targetField(), expectedValue);
                        changed = true;
                    }
                    case MERGE -> {
                        if (actualValue == null) {
                            mergedPayload.put(rule.targetField(), expectedValue);
                            changed = true;
                        }
                    }
                    case SKIP -> {
                        producedDifference = true;
                        differences.add(new SyncDifferenceItem(
                                differenceCode(mappedRecord.recordKey(), rule.targetField(), DifferenceType.VALUE_MISMATCH),
                                DifferenceType.VALUE_MISMATCH,
                                DifferenceStatus.DETECTED,
                                mappedRecord.recordKey(),
                                rule.targetField(),
                                mappedRecord.payload(),
                                existing.payload(),
                                "conflict skipped by strategy",
                                now,
                                null,
                                null,
                                null
                        ));
                    }
                    case MANUAL -> {
                        producedDifference = true;
                        differences.add(new SyncDifferenceItem(
                                differenceCode(mappedRecord.recordKey(), rule.targetField(), DifferenceType.CONFLICT),
                                DifferenceType.CONFLICT,
                                DifferenceStatus.DETECTED,
                                mappedRecord.recordKey(),
                                rule.targetField(),
                                mappedRecord.payload(),
                                existing.payload(),
                                "manual compensation required",
                                now,
                                null,
                                null,
                                null
                        ));
                    }
                }
            }
            if (changed) {
                targetStore.put(mappedRecord.recordKey(), new ConnectorRecord(
                        mappedRecord.recordKey(),
                        mappedRecord.checkpointToken(),
                        mappedRecord.eventId(),
                        mappedRecord.occurredAt() == null ? now : mappedRecord.occurredAt(),
                        mergedPayload
                ));
                updated++;
            } else if (producedDifference || mergedPayload.equals(existing.payload())) {
                skipped++;
            }
        }
        return new SyncPushResult(new SyncResultSummary(mappedRecords.size(), inserted, updated, skipped, 0), differences);
    }

    @Override
    public SyncReconciliationResult reconcile(SyncExchangeTask task, List<SyncMappedRecord> mappedRecords) {
        Map<String, SyncMappedRecord> sourceIndex = new LinkedHashMap<>();
        for (SyncMappedRecord mappedRecord : mappedRecords) {
            sourceIndex.put(mappedRecord.recordKey(), mappedRecord);
        }
        List<SyncDifferenceItem> differences = new ArrayList<>();
        Instant now = Instant.now();
        ConcurrentMap<String, ConnectorRecord> targetStore = connectorRecords.computeIfAbsent(
                task.targetConnectorId(),
                ignored -> new ConcurrentHashMap<>()
        );

        for (SyncMappedRecord mappedRecord : mappedRecords) {
            ConnectorRecord targetRecord = targetStore.get(mappedRecord.recordKey());
            if (targetRecord == null) {
                differences.add(new SyncDifferenceItem(
                        differenceCode(mappedRecord.recordKey(), null, DifferenceType.MISSING_TARGET),
                        DifferenceType.MISSING_TARGET,
                        DifferenceStatus.DETECTED,
                        mappedRecord.recordKey(),
                        null,
                        mappedRecord.payload(),
                        Map.of(),
                        "target record is missing",
                        now,
                        null,
                        null,
                        null
                ));
                continue;
            }
            for (Map.Entry<String, Object> entry : mappedRecord.payload().entrySet()) {
                Object actualValue = targetRecord.payload().get(entry.getKey());
                if (Objects.equals(entry.getValue(), actualValue)) {
                    continue;
                }
                differences.add(new SyncDifferenceItem(
                        differenceCode(mappedRecord.recordKey(), entry.getKey(), DifferenceType.VALUE_MISMATCH),
                        DifferenceType.VALUE_MISMATCH,
                        DifferenceStatus.DETECTED,
                        mappedRecord.recordKey(),
                        entry.getKey(),
                        mappedRecord.payload(),
                        targetRecord.payload(),
                        "target value does not match source projection",
                        now,
                        null,
                        null,
                        null
                ));
            }
        }

        if (task.reconciliationPolicy().checkExtraTargetRecords()) {
            for (ConnectorRecord targetRecord : targetStore.values()) {
                if (sourceIndex.containsKey(targetRecord.recordKey())) {
                    continue;
                }
                differences.add(new SyncDifferenceItem(
                        differenceCode(targetRecord.recordKey(), null, DifferenceType.EXTRA_TARGET),
                        DifferenceType.EXTRA_TARGET,
                        DifferenceStatus.DETECTED,
                        targetRecord.recordKey(),
                        null,
                        Map.of(),
                        targetRecord.payload(),
                        "extra target record detected",
                        now,
                        null,
                        null,
                        null
                ));
            }
        }

        return new SyncReconciliationResult(
                differences.isEmpty() ? ReconciliationStatus.CONSISTENT : ReconciliationStatus.MANUAL_REVIEW_REQUIRED,
                differences
        );
    }

    @Override
    public SyncCompensationResult compensate(
            SyncExchangeTask task,
            List<SyncDifferenceItem> originalDifferences,
            List<SyncCompensationDecision> decisions,
            String operatorId
    ) {
        Instant now = Instant.now();
        ConcurrentMap<String, ConnectorRecord> targetStore = connectorRecords.computeIfAbsent(
                task.targetConnectorId(),
                ignored -> new ConcurrentHashMap<>()
        );
        Map<String, SyncCompensationDecision> decisionIndex = new LinkedHashMap<>();
        for (SyncCompensationDecision decision : decisions) {
            decisionIndex.put(decision.differenceCode(), decision);
        }

        List<SyncDifferenceItem> updatedDifferences = new ArrayList<>();
        long updated = 0;
        long skipped = 0;
        for (SyncDifferenceItem difference : originalDifferences) {
            SyncCompensationDecision decision = decisionIndex.get(difference.differenceCode());
            if (decision == null) {
                updatedDifferences.add(difference);
                continue;
            }
            switch (decision.action()) {
                case IGNORE_DIFFERENCE -> {
                    updatedDifferences.add(difference.ignore(operatorId, decision.reason(), now));
                    skipped++;
                }
                case MANUAL_CONFIRMED -> {
                    updatedDifferences.add(difference.confirmManually(operatorId, decision.reason(), now));
                    skipped++;
                }
                case RETRY_WRITE -> {
                    applyRetryWrite(targetStore, difference, now);
                    updatedDifferences.add(difference.compensate(operatorId, decision.reason(), now));
                    updated++;
                }
            }
        }

        long pending = updatedDifferences.stream().filter(item -> item.status() == DifferenceStatus.DETECTED).count();
        ReconciliationStatus reconciliationStatus = pending == 0
                ? ReconciliationStatus.CONSISTENT
                : ReconciliationStatus.MANUAL_REVIEW_REQUIRED;
        return new SyncCompensationResult(
                new SyncResultSummary(originalDifferences.size(), 0, updated, skipped, 0),
                reconciliationStatus,
                updatedDifferences
        );
    }

    public void registerConnector(UUID connectorId, String status) {
        updateConnectorStatus(connectorId, status);
    }

    public void upsertRecord(UUID connectorId, SyncPayloadRecord record) {
        connectorRecords.computeIfAbsent(connectorId, ignored -> new ConcurrentHashMap<>())
                .put(record.recordKey(), new ConnectorRecord(
                        record.recordKey(),
                        record.checkpointToken(),
                        record.eventId(),
                        record.occurredAt() == null ? Instant.now() : record.occurredAt(),
                        record.payload()
                ));
    }

    public List<SyncPayloadRecord> records(UUID connectorId) {
        return listRecords(connectorId).stream()
                .map(record -> new SyncPayloadRecord(
                        record.recordKey(),
                        record.checkpointToken(),
                        record.eventId(),
                        record.occurredAt(),
                        record.payload()
                ))
                .toList();
    }

    private boolean matchesTrigger(
            SyncExchangeTask task,
            ExecutionTriggerType triggerType,
            String expectedEventId,
            String startCheckpoint,
            ConnectorRecord record
    ) {
        if (triggerType == ExecutionTriggerType.RECONCILIATION) {
            return true;
        }
        if (triggerType == ExecutionTriggerType.EVENT_DRIVEN && expectedEventId != null) {
            return expectedEventId.equals(record.eventId());
        }
        if (task.syncMode() == com.hjo2oa.data.data.sync.domain.SyncMode.FULL) {
            return true;
        }
        if (startCheckpoint == null) {
            return true;
        }
        if (record.checkpointToken() == null) {
            return true;
        }
        return compareCheckpoint(task.checkpointMode().name(), record.checkpointToken(), startCheckpoint) > 0;
    }

    private int compareCheckpoint(String mode, String left, String right) {
        if (right == null) {
            return 1;
        }
        if ("TIMESTAMP".equals(mode)) {
            return Instant.parse(left).compareTo(Instant.parse(right));
        }
        if ("OFFSET".equals(mode) || "VERSION".equals(mode)) {
            try {
                return Long.compare(Long.parseLong(left), Long.parseLong(right));
            } catch (NumberFormatException ignored) {
                return left.compareTo(right);
            }
        }
        return left.compareTo(right);
    }

    private List<ConnectorRecord> listRecords(UUID connectorId) {
        return connectorRecords.computeIfAbsent(connectorId, ignored -> new ConcurrentHashMap<>())
                .values()
                .stream()
                .sorted(Comparator.comparing(ConnectorRecord::occurredAt).thenComparing(ConnectorRecord::recordKey))
                .toList();
    }

    private void applyRetryWrite(
            ConcurrentMap<String, ConnectorRecord> targetStore,
            SyncDifferenceItem difference,
            Instant now
    ) {
        if (difference.differenceType() == DifferenceType.EXTRA_TARGET) {
            targetStore.remove(difference.recordKey());
            return;
        }
        targetStore.put(difference.recordKey(), new ConnectorRecord(
                difference.recordKey(),
                null,
                null,
                now,
                difference.expectedPayload()
        ));
    }

    private String differenceCode(String recordKey, String fieldName, DifferenceType differenceType) {
        return differenceType.name() + ":" + recordKey + ":" + (fieldName == null ? "*" : fieldName);
    }

    private String stringValue(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private record ConnectorRecord(
            String recordKey,
            String checkpointToken,
            String eventId,
            Instant occurredAt,
            Map<String, Object> payload
    ) {
        private ConnectorRecord {
            payload = payload == null ? Map.of() : Map.copyOf(payload);
        }

        private static ConnectorRecord from(SyncMappedRecord mappedRecord) {
            return new ConnectorRecord(
                    mappedRecord.recordKey(),
                    mappedRecord.checkpointToken(),
                    mappedRecord.eventId(),
                    mappedRecord.occurredAt() == null ? Instant.now() : mappedRecord.occurredAt(),
                    mappedRecord.payload()
            );
        }
    }
}
