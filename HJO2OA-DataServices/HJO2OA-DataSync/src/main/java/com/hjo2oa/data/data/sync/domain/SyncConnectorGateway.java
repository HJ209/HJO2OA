package com.hjo2oa.data.data.sync.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SyncConnectorGateway {

    ConnectorDependencyStatus connectorStatus(UUID connectorId);

    void updateConnectorStatus(UUID connectorId, String connectorStatus);

    SyncPullBatch pull(
            SyncExchangeTask task,
            String startCheckpoint,
            ExecutionTriggerType triggerType,
            Map<String, Object> triggerContext
    );

    SyncPushResult push(SyncExchangeTask task, List<SyncMappedRecord> mappedRecords);

    SyncReconciliationResult reconcile(SyncExchangeTask task, List<SyncMappedRecord> mappedRecords);

    SyncCompensationResult compensate(
            SyncExchangeTask task,
            List<SyncDifferenceItem> originalDifferences,
            List<SyncCompensationDecision> decisions,
            String operatorId
    );
}
