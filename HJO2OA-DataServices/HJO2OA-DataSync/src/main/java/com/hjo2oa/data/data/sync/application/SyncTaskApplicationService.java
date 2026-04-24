package com.hjo2oa.data.data.sync.application;

import com.hjo2oa.data.common.application.audit.DataAuditLog;
import com.hjo2oa.data.data.sync.domain.CheckpointMode;
import com.hjo2oa.data.data.sync.domain.ConnectorDependencyStatus;
import com.hjo2oa.data.data.sync.domain.DataSyncErrorDescriptors;
import com.hjo2oa.data.data.sync.domain.ExecutionStatus;
import com.hjo2oa.data.data.sync.domain.PagedResult;
import com.hjo2oa.data.data.sync.domain.SyncConnectorGateway;
import com.hjo2oa.data.data.sync.domain.SyncExchangeTask;
import com.hjo2oa.data.data.sync.domain.SyncExchangeTaskRepository;
import com.hjo2oa.data.data.sync.domain.SyncExecutionFilter;
import com.hjo2oa.data.data.sync.domain.SyncExecutionRecord;
import com.hjo2oa.data.data.sync.domain.SyncExecutionRecordRepository;
import com.hjo2oa.data.data.sync.domain.SyncMappingRule;
import com.hjo2oa.data.data.sync.domain.SyncTaskFilter;
import com.hjo2oa.shared.kernel.BizException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncTaskApplicationService {

    private final SyncExchangeTaskRepository taskRepository;
    private final SyncExecutionRecordRepository executionRecordRepository;
    private final SyncConnectorGateway connectorGateway;
    private final Clock clock;

    public SyncTaskApplicationService(
            SyncExchangeTaskRepository taskRepository,
            SyncExecutionRecordRepository executionRecordRepository,
            SyncConnectorGateway connectorGateway,
            Clock clock
    ) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.executionRecordRepository = Objects.requireNonNull(
                executionRecordRepository,
                "executionRecordRepository must not be null"
        );
        this.connectorGateway = Objects.requireNonNull(connectorGateway, "connectorGateway must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public PagedResult<SyncTaskSummaryView> pageTasks(SyncTaskFilter filter) {
        PagedResult<SyncExchangeTask> page = taskRepository.page(filter);
        List<SyncTaskSummaryView> items = page.items().stream().map(this::toSummaryView).toList();
        return new PagedResult<>(items, page.page(), page.size(), page.total());
    }

    public SyncTaskDetailView getTask(UUID taskId) {
        return toDetailView(loadTask(taskId));
    }

    public PagedResult<SyncExecutionSummaryView> pageExecutions(SyncExecutionFilter filter) {
        PagedResult<SyncExecutionRecord> page = executionRecordRepository.page(filter);
        List<SyncExecutionSummaryView> items = page.items().stream().map(this::toExecutionSummary).toList();
        return new PagedResult<>(items, page.page(), page.size(), page.total());
    }

    public SyncExecutionDetailView getExecution(UUID executionId) {
        SyncExecutionRecord record = executionRecordRepository.findById(executionId)
                .orElseThrow(() -> new BizException(DataSyncErrorDescriptors.EXECUTION_NOT_FOUND));
        return new SyncExecutionDetailView(
                toExecutionSummary(record),
                record.differenceItems(),
                record.triggerContext(),
                record.operatorAccountId(),
                record.operatorPersonId()
        );
    }

    @Transactional
    @DataAuditLog(module = "data-sync", action = "create-task", targetType = "SyncExchangeTask", captureArguments = true)
    public SyncTaskDetailView create(CreateSyncExchangeTaskCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (taskRepository.findByCode(command.tenantId(), command.code()).isPresent()) {
            throw new BizException(DataSyncErrorDescriptors.TASK_CODE_DUPLICATE);
        }
        Instant now = now();
        UUID taskId = UUID.randomUUID();
        SyncExchangeTask task = new SyncExchangeTask(
                taskId,
                command.tenantId(),
                command.code(),
                command.name(),
                command.description(),
                command.taskType(),
                command.syncMode(),
                command.sourceConnectorId(),
                command.targetConnectorId(),
                resolveDependencyStatus(command.sourceConnectorId(), command.targetConnectorId()),
                command.checkpointMode(),
                command.checkpointConfig(),
                command.triggerConfig(),
                command.retryPolicy(),
                command.compensationPolicy(),
                command.reconciliationPolicy(),
                command.scheduleConfig(),
                com.hjo2oa.data.data.sync.domain.SyncTaskStatus.DRAFT,
                toRules(taskId, command.mappingRules(), now),
                now,
                now
        );
        taskRepository.save(task);
        return toDetailView(task);
    }

    @Transactional
    @DataAuditLog(module = "data-sync", action = "update-task", targetType = "SyncExchangeTask", captureArguments = true)
    public SyncTaskDetailView update(UUID taskId, UpdateSyncExchangeTaskCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        SyncExchangeTask existing = loadTask(taskId);
        Instant now = now();
        SyncExchangeTask updated = existing.update(
                command.name(),
                command.description(),
                command.taskType(),
                command.syncMode(),
                command.sourceConnectorId(),
                command.targetConnectorId(),
                command.checkpointMode(),
                command.checkpointConfig(),
                command.triggerConfig(),
                command.retryPolicy(),
                command.compensationPolicy(),
                command.reconciliationPolicy(),
                command.scheduleConfig(),
                toRules(existing.taskId(), command.mappingRules(), now),
                now
        ).refreshDependencyStatus(
                resolveDependencyStatus(command.sourceConnectorId(), command.targetConnectorId()),
                now
        );
        taskRepository.save(updated);
        return toDetailView(updated);
    }

    @Transactional
    @DataAuditLog(module = "data-sync", action = "activate-task", targetType = "SyncExchangeTask", captureArguments = true)
    public SyncTaskDetailView activate(UUID taskId) {
        SyncExchangeTask task = loadTask(taskId);
        SyncExchangeTask updated = task.refreshDependencyStatus(
                resolveDependencyStatus(task.sourceConnectorId(), task.targetConnectorId()),
                now()
        ).activate(now());
        taskRepository.save(updated);
        return toDetailView(updated);
    }

    @Transactional
    @DataAuditLog(module = "data-sync", action = "pause-task", targetType = "SyncExchangeTask", captureArguments = true)
    public SyncTaskDetailView pause(UUID taskId) {
        SyncExchangeTask updated = loadTask(taskId).pause(now());
        taskRepository.save(updated);
        return toDetailView(updated);
    }

    @Transactional
    @DataAuditLog(module = "data-sync", action = "delete-task", targetType = "SyncExchangeTask", captureArguments = true)
    public void delete(UUID taskId) {
        loadTask(taskId);
        taskRepository.delete(taskId);
    }

    @Transactional
    @DataAuditLog(module = "data-sync", action = "reset-checkpoint", targetType = "SyncExchangeTask", captureArguments = true)
    public SyncTaskDetailView resetCheckpoint(UUID taskId, ResetSyncCheckpointCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        SyncExchangeTask updated = loadTask(taskId).resetCheckpoint(
                command.checkpointValue(),
                command.operatorAccountId(),
                command.reason(),
                now()
        );
        taskRepository.save(updated);
        return toDetailView(updated);
    }

    @Transactional
    public void refreshConnectorDependency(UUID connectorId, String connectorStatus) {
        connectorGateway.updateConnectorStatus(connectorId, connectorStatus);
        Instant now = now();
        for (SyncExchangeTask task : taskRepository.findByConnectorId(connectorId)) {
            ConnectorDependencyStatus dependencyStatus = resolveDependencyStatus(
                    task.sourceConnectorId(),
                    task.targetConnectorId()
            );
            if (dependencyStatus != task.dependencyStatus()) {
                taskRepository.save(task.refreshDependencyStatus(dependencyStatus, now));
            }
        }
    }

    private SyncExchangeTask loadTask(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(DataSyncErrorDescriptors.TASK_NOT_FOUND));
    }

    private List<SyncMappingRule> toRules(UUID taskId, List<SyncMappingRuleDraft> drafts, Instant now) {
        if (drafts == null || drafts.isEmpty()) {
            return List.of();
        }
        return drafts.stream()
                .map(draft -> new SyncMappingRule(
                        UUID.randomUUID(),
                        taskId,
                        draft.sourceField(),
                        draft.targetField(),
                        draft.transformRule(),
                        draft.conflictStrategy(),
                        draft.keyMapping(),
                        draft.sortOrder(),
                        now,
                        now
                ))
                .toList();
    }

    private SyncTaskSummaryView toSummaryView(SyncExchangeTask task) {
        Optional<SyncExecutionRecord> latestExecution = executionRecordRepository.findLatestByTaskId(task.taskId());
        SyncExecutionSummaryView latestExecutionView = latestExecution.map(this::toExecutionSummary).orElse(null);
        String latestCheckpoint = latestExecution.map(SyncExecutionRecord::checkpointValue).orElse(null);
        return new SyncTaskSummaryView(
                task.taskId(),
                task.tenantId(),
                task.code(),
                task.name(),
                task.description(),
                task.taskType(),
                task.syncMode(),
                task.sourceConnectorId(),
                task.targetConnectorId(),
                task.dependencyStatus(),
                task.checkpointMode(),
                task.status(),
                latestCheckpoint,
                latestExecutionView,
                task.createdAt(),
                task.updatedAt()
        );
    }

    private SyncTaskDetailView toDetailView(SyncExchangeTask task) {
        return new SyncTaskDetailView(
                toSummaryView(task),
                task.checkpointConfig(),
                task.triggerConfig(),
                task.retryPolicy(),
                task.compensationPolicy(),
                task.reconciliationPolicy(),
                task.scheduleConfig(),
                task.sortedMappingRules()
        );
    }

    private SyncExecutionSummaryView toExecutionSummary(SyncExecutionRecord record) {
        return new SyncExecutionSummaryView(
                record.executionId(),
                record.syncTaskId(),
                record.parentExecutionId(),
                record.taskCode(),
                record.executionBatchNo(),
                record.triggerType(),
                record.executionStatus(),
                record.checkpointValue(),
                record.retryable(),
                record.retryCount(),
                record.failureCode(),
                record.failureMessage(),
                record.resultSummary(),
                record.diffSummary(),
                record.reconciliationStatus(),
                record.startedAt(),
                record.finishedAt()
        );
    }

    private ConnectorDependencyStatus resolveDependencyStatus(UUID sourceConnectorId, UUID targetConnectorId) {
        ConnectorDependencyStatus sourceStatus = connectorGateway.connectorStatus(sourceConnectorId);
        ConnectorDependencyStatus targetStatus = connectorGateway.connectorStatus(targetConnectorId);
        boolean sourceReady = sourceStatus == ConnectorDependencyStatus.READY;
        boolean targetReady = targetStatus == ConnectorDependencyStatus.READY;
        if (sourceReady && targetReady) {
            return ConnectorDependencyStatus.READY;
        }
        if (!sourceReady && !targetReady) {
            return ConnectorDependencyStatus.BOTH_UNAVAILABLE;
        }
        return sourceReady
                ? ConnectorDependencyStatus.TARGET_UNAVAILABLE
                : ConnectorDependencyStatus.SOURCE_UNAVAILABLE;
    }

    private Instant now() {
        return clock.instant();
    }
}
