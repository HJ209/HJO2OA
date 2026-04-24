package com.hjo2oa.data.data.sync.application;

import com.hjo2oa.data.data.sync.domain.CheckpointMode;
import com.hjo2oa.data.data.sync.domain.SyncCheckpointConfig;
import com.hjo2oa.data.data.sync.domain.SyncCompensationPolicy;
import com.hjo2oa.data.data.sync.domain.SyncMode;
import com.hjo2oa.data.data.sync.domain.SyncReconciliationPolicy;
import com.hjo2oa.data.data.sync.domain.SyncRetryPolicy;
import com.hjo2oa.data.data.sync.domain.SyncScheduleConfig;
import com.hjo2oa.data.data.sync.domain.SyncTaskType;
import com.hjo2oa.data.data.sync.domain.SyncTriggerConfig;
import java.util.List;
import java.util.UUID;

public record CreateSyncExchangeTaskCommand(
        UUID tenantId,
        String code,
        String name,
        String description,
        SyncTaskType taskType,
        SyncMode syncMode,
        UUID sourceConnectorId,
        UUID targetConnectorId,
        CheckpointMode checkpointMode,
        SyncCheckpointConfig checkpointConfig,
        SyncTriggerConfig triggerConfig,
        SyncRetryPolicy retryPolicy,
        SyncCompensationPolicy compensationPolicy,
        SyncReconciliationPolicy reconciliationPolicy,
        SyncScheduleConfig scheduleConfig,
        List<SyncMappingRuleDraft> mappingRules
) {
}
