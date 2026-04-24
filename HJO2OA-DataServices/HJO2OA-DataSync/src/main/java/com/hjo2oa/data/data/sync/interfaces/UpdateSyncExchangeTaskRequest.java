package com.hjo2oa.data.data.sync.interfaces;

import com.hjo2oa.data.data.sync.application.UpdateSyncExchangeTaskCommand;
import com.hjo2oa.data.data.sync.domain.CheckpointMode;
import com.hjo2oa.data.data.sync.domain.SyncMode;
import com.hjo2oa.data.data.sync.domain.SyncTaskType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record UpdateSyncExchangeTaskRequest(
        @NotBlank String name,
        String description,
        @NotNull SyncTaskType taskType,
        @NotNull SyncMode syncMode,
        @NotNull UUID sourceConnectorId,
        @NotNull UUID targetConnectorId,
        @NotNull CheckpointMode checkpointMode,
        @Valid SyncCheckpointConfigRequest checkpointConfig,
        @Valid SyncTriggerConfigRequest triggerConfig,
        @Valid SyncRetryPolicyRequest retryPolicy,
        @Valid SyncCompensationPolicyRequest compensationPolicy,
        @Valid SyncReconciliationPolicyRequest reconciliationPolicy,
        @Valid SyncScheduleConfigRequest scheduleConfig,
        @NotEmpty List<@Valid SyncMappingRuleRequest> mappingRules
) {

    public UpdateSyncExchangeTaskCommand toCommand() {
        return new UpdateSyncExchangeTaskCommand(
                name,
                description,
                taskType,
                syncMode,
                sourceConnectorId,
                targetConnectorId,
                checkpointMode,
                checkpointConfig == null ? null : checkpointConfig.toConfig(),
                triggerConfig == null ? null : triggerConfig.toConfig(),
                retryPolicy == null ? null : retryPolicy.toPolicy(),
                compensationPolicy == null ? null : compensationPolicy.toPolicy(),
                reconciliationPolicy == null ? null : reconciliationPolicy.toPolicy(),
                scheduleConfig == null ? null : scheduleConfig.toConfig(),
                mappingRules.stream().map(SyncMappingRuleRequest::toDraft).toList()
        );
    }
}
