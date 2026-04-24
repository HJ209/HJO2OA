package com.hjo2oa.data.data.sync.domain;

import com.hjo2oa.shared.kernel.BizException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SyncExchangeTask(
        UUID taskId,
        UUID tenantId,
        String code,
        String name,
        String description,
        SyncTaskType taskType,
        SyncMode syncMode,
        UUID sourceConnectorId,
        UUID targetConnectorId,
        ConnectorDependencyStatus dependencyStatus,
        CheckpointMode checkpointMode,
        SyncCheckpointConfig checkpointConfig,
        SyncTriggerConfig triggerConfig,
        SyncRetryPolicy retryPolicy,
        SyncCompensationPolicy compensationPolicy,
        SyncReconciliationPolicy reconciliationPolicy,
        SyncScheduleConfig scheduleConfig,
        SyncTaskStatus status,
        List<SyncMappingRule> mappingRules,
        Instant createdAt,
        Instant updatedAt
) {

    public SyncExchangeTask {
        taskId = SyncDomainSupport.requireId(taskId, "taskId");
        tenantId = SyncDomainSupport.requireId(tenantId, "tenantId");
        code = SyncDomainSupport.requireText(code, "code");
        name = SyncDomainSupport.requireText(name, "name");
        description = SyncDomainSupport.normalize(description);
        Objects.requireNonNull(taskType, "taskType must not be null");
        Objects.requireNonNull(syncMode, "syncMode must not be null");
        sourceConnectorId = SyncDomainSupport.requireId(sourceConnectorId, "sourceConnectorId");
        targetConnectorId = SyncDomainSupport.requireId(targetConnectorId, "targetConnectorId");
        Objects.requireNonNull(dependencyStatus, "dependencyStatus must not be null");
        Objects.requireNonNull(checkpointMode, "checkpointMode must not be null");
        checkpointConfig = checkpointConfig == null ? SyncCheckpointConfig.empty() : checkpointConfig;
        triggerConfig = triggerConfig == null ? SyncTriggerConfig.manualOnly() : triggerConfig;
        retryPolicy = retryPolicy == null ? SyncRetryPolicy.manualOnly() : retryPolicy;
        compensationPolicy = compensationPolicy == null
                ? SyncCompensationPolicy.manualDefault()
                : compensationPolicy;
        reconciliationPolicy = reconciliationPolicy == null
                ? SyncReconciliationPolicy.enabledByDefault()
                : reconciliationPolicy;
        scheduleConfig = scheduleConfig == null ? SyncScheduleConfig.disabled() : scheduleConfig;
        Objects.requireNonNull(status, "status must not be null");
        mappingRules = mappingRules == null ? List.of() : List.copyOf(mappingRules);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        validateModel(syncMode, checkpointMode, triggerConfig, compensationPolicy, mappingRules);
    }

    public SyncExchangeTask activate(Instant now) {
        if (mappingRules.isEmpty()) {
            throw new BizException(DataSyncErrorDescriptors.TASK_MAPPING_INVALID, "active task must define mappings");
        }
        return new SyncExchangeTask(
                taskId,
                tenantId,
                code,
                name,
                description,
                taskType,
                syncMode,
                sourceConnectorId,
                targetConnectorId,
                dependencyStatus,
                checkpointMode,
                checkpointConfig,
                triggerConfig,
                retryPolicy,
                compensationPolicy,
                reconciliationPolicy,
                scheduleConfig,
                SyncTaskStatus.ACTIVE,
                mappingRules,
                createdAt,
                Objects.requireNonNull(now, "now must not be null")
        );
    }

    public SyncExchangeTask pause(Instant now) {
        return updateStatus(SyncTaskStatus.PAUSED, now);
    }

    public SyncExchangeTask markError(Instant now) {
        return updateStatus(SyncTaskStatus.ERROR, now);
    }

    public SyncExchangeTask update(
            String updatedName,
            String updatedDescription,
            SyncTaskType updatedTaskType,
            SyncMode updatedSyncMode,
            UUID updatedSourceConnectorId,
            UUID updatedTargetConnectorId,
            CheckpointMode updatedCheckpointMode,
            SyncCheckpointConfig updatedCheckpointConfig,
            SyncTriggerConfig updatedTriggerConfig,
            SyncRetryPolicy updatedRetryPolicy,
            SyncCompensationPolicy updatedCompensationPolicy,
            SyncReconciliationPolicy updatedReconciliationPolicy,
            SyncScheduleConfig updatedScheduleConfig,
            List<SyncMappingRule> updatedMappingRules,
            Instant now
    ) {
        return new SyncExchangeTask(
                taskId,
                tenantId,
                code,
                updatedName,
                updatedDescription,
                updatedTaskType,
                updatedSyncMode,
                updatedSourceConnectorId,
                updatedTargetConnectorId,
                dependencyStatus,
                updatedCheckpointMode,
                updatedCheckpointConfig,
                updatedTriggerConfig,
                updatedRetryPolicy,
                updatedCompensationPolicy,
                updatedReconciliationPolicy,
                updatedScheduleConfig,
                status,
                relinkMappings(updatedMappingRules, Objects.requireNonNull(now, "now must not be null")),
                createdAt,
                now
        );
    }

    public SyncExchangeTask refreshDependencyStatus(
            ConnectorDependencyStatus updatedDependencyStatus,
            Instant now
    ) {
        return new SyncExchangeTask(
                taskId,
                tenantId,
                code,
                name,
                description,
                taskType,
                syncMode,
                sourceConnectorId,
                targetConnectorId,
                Objects.requireNonNull(updatedDependencyStatus, "updatedDependencyStatus must not be null"),
                checkpointMode,
                checkpointConfig,
                triggerConfig,
                retryPolicy,
                compensationPolicy,
                reconciliationPolicy,
                scheduleConfig,
                status,
                mappingRules,
                createdAt,
                Objects.requireNonNull(now, "now must not be null")
        );
    }

    public SyncExchangeTask resetCheckpoint(String checkpointValue, String operatorId, String reason, Instant now) {
        if (!checkpointConfig.allowManualReset()) {
            throw new BizException(DataSyncErrorDescriptors.CHECKPOINT_RESET_FORBIDDEN);
        }
        return new SyncExchangeTask(
                taskId,
                tenantId,
                code,
                name,
                description,
                taskType,
                syncMode,
                sourceConnectorId,
                targetConnectorId,
                dependencyStatus,
                checkpointMode,
                checkpointConfig.override(checkpointValue, operatorId, reason, now),
                triggerConfig,
                retryPolicy,
                compensationPolicy,
                reconciliationPolicy,
                scheduleConfig,
                status,
                mappingRules,
                createdAt,
                now
        );
    }

    public SyncExchangeTask clearCheckpointOverride(Instant now) {
        return new SyncExchangeTask(
                taskId,
                tenantId,
                code,
                name,
                description,
                taskType,
                syncMode,
                sourceConnectorId,
                targetConnectorId,
                dependencyStatus,
                checkpointMode,
                checkpointConfig.clearOverride(),
                triggerConfig,
                retryPolicy,
                compensationPolicy,
                reconciliationPolicy,
                scheduleConfig,
                status,
                mappingRules,
                createdAt,
                now
        );
    }

    public boolean dependencyReady() {
        return dependencyStatus == ConnectorDependencyStatus.READY;
    }

    public boolean canExecute(ExecutionTriggerType triggerType) {
        if (!dependencyReady()) {
            return false;
        }
        return switch (triggerType) {
            case MANUAL, RETRY, RECONCILIATION, COMPENSATION ->
                    status == SyncTaskStatus.ACTIVE || status == SyncTaskStatus.PAUSED;
            case SCHEDULED, EVENT_DRIVEN -> status == SyncTaskStatus.ACTIVE;
        };
    }

    public boolean matchesEvent(String eventType) {
        return syncMode == SyncMode.EVENT_DRIVEN && triggerConfig.matchesEvent(eventType);
    }

    public boolean handlesSchedulerJob(String jobCode) {
        if (jobCode == null || jobCode.isBlank()) {
            return false;
        }
        return jobCode.equals(scheduleConfig.schedulerJobCode()) || jobCode.equals(triggerConfig.schedulerJobCode());
    }

    public String resolveStartCheckpoint(String lastSuccessfulCheckpoint) {
        String manualOverride = checkpointConfig.resolvedStartCheckpoint();
        if (manualOverride != null) {
            return manualOverride;
        }
        return SyncDomainSupport.normalize(lastSuccessfulCheckpoint);
    }

    public List<SyncMappingRule> sortedMappingRules() {
        return mappingRules.stream()
                .sorted(Comparator.comparingInt(SyncMappingRule::sortOrder)
                        .thenComparing(rule -> rule.targetField().toLowerCase()))
                .toList();
    }

    private SyncExchangeTask updateStatus(SyncTaskStatus updatedStatus, Instant now) {
        return new SyncExchangeTask(
                taskId,
                tenantId,
                code,
                name,
                description,
                taskType,
                syncMode,
                sourceConnectorId,
                targetConnectorId,
                dependencyStatus,
                checkpointMode,
                checkpointConfig,
                triggerConfig,
                retryPolicy,
                compensationPolicy,
                reconciliationPolicy,
                scheduleConfig,
                Objects.requireNonNull(updatedStatus, "updatedStatus must not be null"),
                mappingRules,
                createdAt,
                Objects.requireNonNull(now, "now must not be null")
        );
    }

    private List<SyncMappingRule> relinkMappings(List<SyncMappingRule> updatedMappingRules, Instant now) {
        if (updatedMappingRules == null || updatedMappingRules.isEmpty()) {
            return List.of();
        }
        return updatedMappingRules.stream()
                .map(rule -> new SyncMappingRule(
                        rule.ruleId(),
                        taskId,
                        rule.sourceField(),
                        rule.targetField(),
                        rule.transformRule(),
                        rule.conflictStrategy(),
                        rule.keyMapping(),
                        rule.sortOrder(),
                        rule.createdAt(),
                        now
                ))
                .toList();
    }

    private void validateModel(
            SyncMode validatedSyncMode,
            CheckpointMode validatedCheckpointMode,
            SyncTriggerConfig validatedTriggerConfig,
            SyncCompensationPolicy validatedCompensationPolicy,
            List<SyncMappingRule> validatedMappingRules
    ) {
        if (validatedSyncMode != SyncMode.FULL && validatedCheckpointMode == CheckpointMode.NONE) {
            throw new IllegalArgumentException("incremental and event-driven modes must declare checkpoint mode");
        }
        if (validatedSyncMode == SyncMode.EVENT_DRIVEN
                && validatedTriggerConfig.eventPatterns().isEmpty()
                && validatedTriggerConfig.schedulerJobCode() == null) {
            throw new IllegalArgumentException("event-driven mode must declare inbound event patterns");
        }
        if (!validatedCompensationPolicy.manualCompensationEnabled()) {
            boolean hasManualConflictRule = validatedMappingRules.stream()
                    .anyMatch(rule -> rule.conflictStrategy() == ConflictStrategy.MANUAL);
            if (hasManualConflictRule) {
                throw new IllegalArgumentException("manual conflict rules require manual compensation policy");
            }
        }
        long duplicateSourceTargets = validatedMappingRules.stream()
                .map(rule -> rule.sourceField() + "->" + rule.targetField())
                .distinct()
                .count();
        if (duplicateSourceTargets != validatedMappingRules.size()) {
            throw new BizException(DataSyncErrorDescriptors.TASK_MAPPING_INVALID, "duplicate source/target mapping rule");
        }
    }
}
