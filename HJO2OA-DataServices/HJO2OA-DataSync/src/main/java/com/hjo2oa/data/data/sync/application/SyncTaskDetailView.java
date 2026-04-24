package com.hjo2oa.data.data.sync.application;

import com.hjo2oa.data.data.sync.domain.SyncCheckpointConfig;
import com.hjo2oa.data.data.sync.domain.SyncCompensationPolicy;
import com.hjo2oa.data.data.sync.domain.SyncMappingRule;
import com.hjo2oa.data.data.sync.domain.SyncReconciliationPolicy;
import com.hjo2oa.data.data.sync.domain.SyncRetryPolicy;
import com.hjo2oa.data.data.sync.domain.SyncScheduleConfig;
import com.hjo2oa.data.data.sync.domain.SyncTriggerConfig;
import java.util.List;

public record SyncTaskDetailView(
        SyncTaskSummaryView summary,
        SyncCheckpointConfig checkpointConfig,
        SyncTriggerConfig triggerConfig,
        SyncRetryPolicy retryPolicy,
        SyncCompensationPolicy compensationPolicy,
        SyncReconciliationPolicy reconciliationPolicy,
        SyncScheduleConfig scheduleConfig,
        List<SyncMappingRule> mappingRules
) {
}
