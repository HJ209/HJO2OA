package com.hjo2oa.data.data.sync.application;

import com.hjo2oa.data.common.application.audit.DataAuditLog;
import com.hjo2oa.data.common.application.event.DataDomainEventPublisher;
import com.hjo2oa.data.data.sync.domain.CompensationAction;
import com.hjo2oa.data.data.sync.domain.DataSyncCompletedEvent;
import com.hjo2oa.data.data.sync.domain.DataSyncErrorDescriptors;
import com.hjo2oa.data.data.sync.domain.DataSyncFailedEvent;
import com.hjo2oa.data.data.sync.domain.DifferenceStatus;
import com.hjo2oa.data.data.sync.domain.ExecutionTriggerType;
import com.hjo2oa.data.data.sync.domain.ReconciliationStatus;
import com.hjo2oa.data.data.sync.domain.SyncCompensationDecision;
import com.hjo2oa.data.data.sync.domain.SyncCompensationResult;
import com.hjo2oa.data.data.sync.domain.SyncConnectorGateway;
import com.hjo2oa.data.data.sync.domain.SyncDiffSummary;
import com.hjo2oa.data.data.sync.domain.SyncDifferenceItem;
import com.hjo2oa.data.data.sync.domain.SyncExchangeTask;
import com.hjo2oa.data.data.sync.domain.SyncExchangeTaskRepository;
import com.hjo2oa.data.data.sync.domain.SyncExecutionRecord;
import com.hjo2oa.data.data.sync.domain.SyncExecutionRecordRepository;
import com.hjo2oa.data.data.sync.domain.SyncMappedRecord;
import com.hjo2oa.data.data.sync.domain.SyncMappingRule;
import com.hjo2oa.data.data.sync.domain.SyncPayloadRecord;
import com.hjo2oa.data.data.sync.domain.SyncPullBatch;
import com.hjo2oa.data.data.sync.domain.SyncPushResult;
import com.hjo2oa.data.data.sync.domain.SyncReconciliationResult;
import com.hjo2oa.data.data.sync.domain.SyncResultSummary;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncExecutionApplicationService {

    private final SyncExchangeTaskRepository taskRepository;
    private final SyncExecutionRecordRepository executionRecordRepository;
    private final SyncConnectorGateway connectorGateway;
    private final DataDomainEventPublisher eventPublisher;
    private final Clock clock;

    public SyncExecutionApplicationService(
            SyncExchangeTaskRepository taskRepository,
            SyncExecutionRecordRepository executionRecordRepository,
            SyncConnectorGateway connectorGateway,
            DataDomainEventPublisher eventPublisher,
            Clock clock
    ) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.executionRecordRepository = Objects.requireNonNull(
                executionRecordRepository,
                "executionRecordRepository must not be null"
        );
        this.connectorGateway = Objects.requireNonNull(connectorGateway, "connectorGateway must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    @DataAuditLog(module = "data-sync", action = "trigger-task", targetType = "SyncExecutionRecord", captureArguments = true)
    public SyncExecutionDetailView triggerTask(UUID taskId, TriggerSyncTaskCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        SyncExchangeTask task = loadTask(taskId);
        SyncExecutionRecord record = execute(
                task,
                ExecutionTriggerType.MANUAL,
                null,
                command.idempotencyKey(),
                0,
                command.operatorAccountId(),
                command.operatorPersonId(),
                command.triggerContext()
        );
        return toDetailView(record);
    }

    @Transactional
    @DataAuditLog(module = "data-sync", action = "retry-execution", targetType = "SyncExecutionRecord", captureArguments = true)
    public SyncExecutionDetailView retryExecution(UUID executionId, RetrySyncExecutionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        SyncExecutionRecord original = loadExecution(executionId);
        if (!original.canRetry()) {
            throw new BizException(DataSyncErrorDescriptors.EXECUTION_NOT_RETRYABLE);
        }
        SyncExchangeTask task = loadTask(original.syncTaskId());
        Map<String, Object> triggerContext = new LinkedHashMap<>(original.triggerContext());
        triggerContext.put("retryOfExecutionId", original.executionId().toString());
        triggerContext.put("retryReason", command.reason());
        SyncExecutionRecord record = execute(
                task,
                ExecutionTriggerType.RETRY,
                original.executionId(),
                command.idempotencyKey(),
                original.retryCount() + 1,
                command.operatorAccountId(),
                command.operatorPersonId(),
                triggerContext
        );
        return toDetailView(record);
    }

    @Transactional
    @DataAuditLog(module = "data-sync", action = "reconcile-execution", targetType = "SyncExecutionRecord", captureArguments = true)
    public SyncExecutionDetailView reconcileExecution(UUID executionId, ReconcileSyncExecutionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        SyncExecutionRecord reference = loadExecution(executionId);
        SyncExchangeTask task = loadTask(reference.syncTaskId());
        if (!task.reconciliationPolicy().enabled()) {
            throw new BizException(DataSyncErrorDescriptors.RECONCILIATION_NOT_ALLOWED);
        }
        Map<String, Object> triggerContext = new LinkedHashMap<>(reference.triggerContext());
        triggerContext.put("reconcileOfExecutionId", reference.executionId().toString());
        triggerContext.put("reconcileReason", command.reason());
        SyncExecutionRecord record = executeReconciliation(
                task,
                reference.executionId(),
                command.idempotencyKey(),
                command.operatorAccountId(),
                command.operatorPersonId(),
                triggerContext
        );
        return toDetailView(record);
    }

    @Transactional
    @DataAuditLog(module = "data-sync", action = "compensate-execution", targetType = "SyncExecutionRecord", captureArguments = true)
    public SyncExecutionDetailView compensateExecution(UUID executionId, SubmitManualCompensationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        SyncExecutionRecord reference = loadExecution(executionId);
        SyncExchangeTask task = loadTask(reference.syncTaskId());
        if (!task.compensationPolicy().manualCompensationEnabled()) {
            throw new BizException(DataSyncErrorDescriptors.COMPENSATION_NOT_ALLOWED);
        }
        validateCompensationDecisions(reference.differenceItems(), command.decisions());
        SyncExecutionRecord record = executeCompensation(
                task,
                reference,
                command.idempotencyKey(),
                command.operatorAccountId(),
                command.operatorPersonId(),
                command.reason(),
                command.decisions()
        );
        return toDetailView(record);
    }

    @Transactional
    public void onBusinessEvent(DomainEvent event, Map<String, Object> payload) {
        Objects.requireNonNull(event, "event must not be null");
        for (SyncExchangeTask task : taskRepository.findActiveEventDrivenTasks()) {
            if (!task.matchesEvent(event.eventType())) {
                continue;
            }
            Map<String, Object> triggerContext = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
            triggerContext.putIfAbsent("eventId", event.eventId().toString());
            triggerContext.putIfAbsent("eventType", event.eventType());
            triggerContext.putIfAbsent("tenantId", event.tenantId());
            triggerContext.putIfAbsent("occurredAt", event.occurredAt().toString());
            execute(
                    task,
                    ExecutionTriggerType.EVENT_DRIVEN,
                    null,
                    "event:" + event.eventId(),
                    0,
                    null,
                    null,
                    triggerContext
            );
        }
    }

    @Transactional
    public void onSchedulerTrigger(String jobCode, Map<String, Object> payload) {
        if (jobCode == null || jobCode.isBlank()) {
            return;
        }
        for (SyncExchangeTask task : taskRepository.findActiveScheduledTasks()) {
            if (!task.handlesSchedulerJob(jobCode)) {
                continue;
            }
            Map<String, Object> triggerContext = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
            triggerContext.putIfAbsent("jobCode", jobCode);
            execute(
                    task,
                    ExecutionTriggerType.SCHEDULED,
                    null,
                    resolveSchedulerIdempotencyKey(jobCode, triggerContext),
                    0,
                    null,
                    null,
                    triggerContext
            );
        }
    }

    @Transactional
    public void triggerScheduledTask(SyncExchangeTask task, String idempotencyKey, Map<String, Object> triggerContext) {
        execute(task, ExecutionTriggerType.SCHEDULED, null, idempotencyKey, 0, null, null, triggerContext);
    }

    private SyncExecutionRecord execute(
            SyncExchangeTask task,
            ExecutionTriggerType triggerType,
            UUID parentExecutionId,
            String idempotencyKey,
            int retryCount,
            String operatorAccountId,
            String operatorPersonId,
            Map<String, Object> triggerContext
    ) {
        Objects.requireNonNull(task, "task must not be null");
        Optional<SyncExecutionRecord> existingRecord = existingIdempotentRecord(task.taskId(), idempotencyKey);
        if (existingRecord.isPresent()) {
            return existingRecord.orElseThrow();
        }

        Instant now = now();
        SyncExecutionRecord running = SyncExecutionRecord.start(
                task,
                triggerType,
                parentExecutionId,
                idempotencyKey,
                retryCount,
                operatorAccountId,
                operatorPersonId,
                triggerContext,
                now
        );
        executionRecordRepository.save(running);
        if (!task.canExecute(triggerType)) {
            return completeAsFailure(task, running, null, SyncResultSummary.empty(), List.of(), "TASK_NOT_EXECUTABLE",
                    "task cannot be executed in current state");
        }

        String startCheckpoint = resolveStartCheckpoint(task);
        try {
            SyncPullBatch pullBatch = connectorGateway.pull(task, startCheckpoint, triggerType, triggerContext);
            List<SyncMappedRecord> mappedRecords = mapRecords(task, pullBatch.records());
            SyncPushResult pushResult = connectorGateway.push(task, mappedRecords);
            SyncReconciliationResult reconciliationResult = task.reconciliationPolicy().enabled()
                    ? connectorGateway.reconcile(task, mappedRecords)
                    : new SyncReconciliationResult(ReconciliationStatus.NOT_CHECKED, List.of());
            List<SyncDifferenceItem> differences = mergeDifferences(
                    pushResult.differences(),
                    reconciliationResult.differences()
            );
            SyncResultSummary resultSummary = ensureSourceCount(pushResult.resultSummary(), pullBatch.records().size());
            if (shouldFail(task, resultSummary, differences, reconciliationResult.reconciliationStatus())) {
                return completeAsFailure(
                        task,
                        running,
                        pullBatch.nextCheckpoint() != null ? pullBatch.nextCheckpoint() : startCheckpoint,
                        resultSummary,
                        differences,
                        resolveFailureCode(resultSummary, reconciliationResult.reconciliationStatus()),
                        resolveFailureMessage(resultSummary, differences, reconciliationResult.reconciliationStatus())
                );
            }
            SyncExecutionRecord succeeded = running.succeed(
                    pullBatch.nextCheckpoint() != null ? pullBatch.nextCheckpoint() : startCheckpoint,
                    resultSummary,
                    differences,
                    reconciliationResult.reconciliationStatus(),
                    now()
            );
            executionRecordRepository.save(succeeded);
            clearCheckpointOverrideIfNeeded(task);
            publishCompleted(task, succeeded);
            return succeeded;
        } catch (RuntimeException ex) {
            return completeAsFailure(
                    task,
                    running,
                    startCheckpoint,
                    SyncResultSummary.empty(),
                    List.of(),
                    "UNEXPECTED_SYNC_ERROR",
                    ex.getMessage()
            );
        }
    }

    private SyncExecutionRecord executeReconciliation(
            SyncExchangeTask task,
            UUID parentExecutionId,
            String idempotencyKey,
            String operatorAccountId,
            String operatorPersonId,
            Map<String, Object> triggerContext
    ) {
        Optional<SyncExecutionRecord> existingRecord = existingIdempotentRecord(task.taskId(), idempotencyKey);
        if (existingRecord.isPresent()) {
            return existingRecord.orElseThrow();
        }
        String startCheckpoint = resolveStartCheckpoint(task);
        SyncExecutionRecord running = SyncExecutionRecord.start(
                task,
                ExecutionTriggerType.RECONCILIATION,
                parentExecutionId,
                idempotencyKey,
                0,
                operatorAccountId,
                operatorPersonId,
                triggerContext,
                now()
        );
        executionRecordRepository.save(running);
        if (!task.canExecute(ExecutionTriggerType.RECONCILIATION)) {
            return completeAsFailure(task, running, startCheckpoint, SyncResultSummary.empty(), List.of(),
                    "TASK_NOT_EXECUTABLE", "task cannot be reconciled in current state");
        }

        try {
            SyncPullBatch pullBatch = connectorGateway.pull(task, startCheckpoint, ExecutionTriggerType.RECONCILIATION, triggerContext);
            List<SyncMappedRecord> mappedRecords = mapRecords(task, pullBatch.records());
            SyncReconciliationResult reconciliationResult = connectorGateway.reconcile(task, mappedRecords);
            List<SyncDifferenceItem> differences = reconciliationResult.differences();
            SyncResultSummary resultSummary = new SyncResultSummary(pullBatch.records().size(), 0, 0, pullBatch.records().size(), 0);
            if (shouldFail(task, resultSummary, differences, reconciliationResult.reconciliationStatus())) {
                return completeAsFailure(
                        task,
                        running,
                        pullBatch.nextCheckpoint() != null ? pullBatch.nextCheckpoint() : startCheckpoint,
                        resultSummary,
                        differences,
                        "RECONCILIATION_DIFFERENCE_DETECTED",
                        "differences detected during reconciliation"
                );
            }
            SyncExecutionRecord succeeded = running.succeed(
                    pullBatch.nextCheckpoint() != null ? pullBatch.nextCheckpoint() : startCheckpoint,
                    resultSummary,
                    differences,
                    reconciliationResult.reconciliationStatus(),
                    now()
            );
            executionRecordRepository.save(succeeded);
            publishCompleted(task, succeeded);
            return succeeded;
        } catch (RuntimeException ex) {
            return completeAsFailure(task, running, startCheckpoint, SyncResultSummary.empty(), List.of(),
                    "RECONCILIATION_ERROR", ex.getMessage());
        }
    }

    private SyncExecutionRecord executeCompensation(
            SyncExchangeTask task,
            SyncExecutionRecord reference,
            String idempotencyKey,
            String operatorAccountId,
            String operatorPersonId,
            String reason,
            List<SyncCompensationDecision> decisions
    ) {
        Optional<SyncExecutionRecord> existingRecord = existingIdempotentRecord(task.taskId(), idempotencyKey);
        if (existingRecord.isPresent()) {
            return existingRecord.orElseThrow();
        }
        Map<String, Object> triggerContext = new LinkedHashMap<>();
        triggerContext.put("compensateOfExecutionId", reference.executionId().toString());
        triggerContext.put("reason", reason);
        SyncExecutionRecord running = SyncExecutionRecord.start(
                task,
                ExecutionTriggerType.COMPENSATION,
                reference.executionId(),
                idempotencyKey,
                0,
                operatorAccountId,
                operatorPersonId,
                triggerContext,
                now()
        );
        executionRecordRepository.save(running);
        if (!task.canExecute(ExecutionTriggerType.COMPENSATION)) {
            return completeAsFailure(task, running, reference.checkpointValue(), SyncResultSummary.empty(), List.of(),
                    "TASK_NOT_EXECUTABLE", "task cannot be compensated in current state");
        }

        try {
            SyncCompensationResult compensationResult = connectorGateway.compensate(
                    task,
                    reference.differenceItems(),
                    decisions,
                    operatorAccountId
            );
            List<SyncDifferenceItem> differences = compensationResult.differences();
            if (shouldFail(task, compensationResult.resultSummary(), differences, compensationResult.reconciliationStatus())) {
                return completeAsFailure(
                        task,
                        running,
                        reference.checkpointValue(),
                        compensationResult.resultSummary(),
                        differences,
                        "COMPENSATION_INCOMPLETE",
                        "manual compensation did not resolve all required differences"
                );
            }
            SyncExecutionRecord succeeded = running.succeed(
                    reference.checkpointValue(),
                    compensationResult.resultSummary(),
                    differences,
                    compensationResult.reconciliationStatus(),
                    now()
            );
            executionRecordRepository.save(succeeded);
            publishCompleted(task, succeeded);
            return succeeded;
        } catch (RuntimeException ex) {
            return completeAsFailure(task, running, reference.checkpointValue(), SyncResultSummary.empty(), List.of(),
                    "COMPENSATION_ERROR", ex.getMessage());
        }
    }

    private void validateCompensationDecisions(
            List<SyncDifferenceItem> originalDifferences,
            List<SyncCompensationDecision> decisions
    ) {
        if (decisions == null || decisions.isEmpty()) {
            throw new BizException(DataSyncErrorDescriptors.COMPENSATION_NOT_ALLOWED, "compensation decisions are required");
        }
        Map<String, SyncDifferenceItem> differenceIndex = new LinkedHashMap<>();
        for (SyncDifferenceItem difference : originalDifferences) {
            differenceIndex.put(difference.differenceCode(), difference);
        }
        for (SyncCompensationDecision decision : decisions) {
            SyncDifferenceItem difference = differenceIndex.get(decision.differenceCode());
            if (difference == null) {
                throw new BizException(DataSyncErrorDescriptors.COMPENSATION_NOT_ALLOWED, "difference not found");
            }
            if (difference.resolved()) {
                throw new BizException(DataSyncErrorDescriptors.DIFFERENCE_ALREADY_RESOLVED);
            }
            if (decision.action() == CompensationAction.IGNORE_DIFFERENCE && difference.status() != DifferenceStatus.DETECTED) {
                throw new BizException(DataSyncErrorDescriptors.DIFFERENCE_ALREADY_RESOLVED);
            }
        }
    }

    private Optional<SyncExecutionRecord> existingIdempotentRecord(UUID taskId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return executionRecordRepository.findByTaskIdAndIdempotencyKey(taskId, idempotencyKey);
    }

    private SyncExchangeTask loadTask(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(DataSyncErrorDescriptors.TASK_NOT_FOUND));
    }

    private SyncExecutionRecord loadExecution(UUID executionId) {
        return executionRecordRepository.findById(executionId)
                .orElseThrow(() -> new BizException(DataSyncErrorDescriptors.EXECUTION_NOT_FOUND));
    }

    private String resolveStartCheckpoint(SyncExchangeTask task) {
        String lastSuccessfulCheckpoint = executionRecordRepository.findLatestSuccessfulByTaskId(task.taskId())
                .map(SyncExecutionRecord::checkpointValue)
                .orElse(null);
        return task.resolveStartCheckpoint(lastSuccessfulCheckpoint);
    }

    private String resolveSchedulerIdempotencyKey(String jobCode, Map<String, Object> triggerContext) {
        String explicitKey = stringValue(triggerContext, "idempotencyKey");
        if (explicitKey != null) {
            return explicitKey;
        }
        String eventId = stringValue(triggerContext, "eventId");
        if (eventId != null) {
            return "scheduler-event:" + eventId;
        }
        String triggerAt = stringValue(triggerContext, "triggerAt");
        if (triggerAt != null) {
            return "scheduler:" + jobCode + ":" + triggerAt;
        }
        return "scheduler:" + jobCode + ":" + now().toEpochMilli();
    }

    private List<SyncMappedRecord> mapRecords(SyncExchangeTask task, List<SyncPayloadRecord> payloadRecords) {
        List<SyncMappedRecord> mappedRecords = new ArrayList<>();
        for (SyncPayloadRecord payloadRecord : payloadRecords) {
            LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
            List<String> businessKeys = new ArrayList<>();
            for (SyncMappingRule rule : task.sortedMappingRules()) {
                Object mappedValue = rule.mapValue(payloadRecord.payload());
                payload.put(rule.targetField(), mappedValue);
                if (rule.keyMapping() && mappedValue != null) {
                    businessKeys.add(String.valueOf(mappedValue));
                }
            }
            String recordKey = businessKeys.isEmpty()
                    ? payloadRecord.recordKey()
                    : String.join("|", businessKeys);
            mappedRecords.add(new SyncMappedRecord(
                    recordKey,
                    payloadRecord.checkpointToken(),
                    payloadRecord.eventId(),
                    payloadRecord.occurredAt(),
                    payload
            ));
        }
        return mappedRecords;
    }

    private List<SyncDifferenceItem> mergeDifferences(
            List<SyncDifferenceItem> first,
            List<SyncDifferenceItem> second
    ) {
        LinkedHashMap<String, SyncDifferenceItem> merged = new LinkedHashMap<>();
        if (first != null) {
            first.forEach(item -> merged.put(item.differenceCode(), item));
        }
        if (second != null) {
            second.forEach(item -> merged.put(item.differenceCode(), item));
        }
        return List.copyOf(merged.values());
    }

    private SyncResultSummary ensureSourceCount(SyncResultSummary resultSummary, int sourceCount) {
        if (resultSummary == null) {
            return new SyncResultSummary(sourceCount, 0, 0, 0, 0);
        }
        if (resultSummary.sourceCount() == sourceCount) {
            return resultSummary;
        }
        return new SyncResultSummary(
                sourceCount,
                resultSummary.insertedCount(),
                resultSummary.updatedCount(),
                resultSummary.skippedCount(),
                resultSummary.failedCount()
        );
    }

    private boolean shouldFail(
            SyncExchangeTask task,
            SyncResultSummary resultSummary,
            List<SyncDifferenceItem> differences,
            ReconciliationStatus reconciliationStatus
    ) {
        if (resultSummary.failedCount() > 0) {
            return true;
        }
        if (reconciliationStatus == ReconciliationStatus.MANUAL_REVIEW_REQUIRED
                && task.reconciliationPolicy().failWhenDifferenceDetected()) {
            return true;
        }
        return differences.stream().anyMatch(item -> item.status() == DifferenceStatus.DETECTED)
                && task.reconciliationPolicy().failWhenDifferenceDetected();
    }

    private String resolveFailureCode(SyncResultSummary resultSummary, ReconciliationStatus reconciliationStatus) {
        if (resultSummary.failedCount() > 0) {
            return "TARGET_WRITE_FAILED";
        }
        if (reconciliationStatus == ReconciliationStatus.MANUAL_REVIEW_REQUIRED) {
            return "RECONCILIATION_DIFFERENCE_DETECTED";
        }
        return "SYNC_EXECUTION_FAILED";
    }

    private String resolveFailureMessage(
            SyncResultSummary resultSummary,
            List<SyncDifferenceItem> differences,
            ReconciliationStatus reconciliationStatus
    ) {
        if (resultSummary.failedCount() > 0) {
            return "target write failed";
        }
        if (reconciliationStatus == ReconciliationStatus.MANUAL_REVIEW_REQUIRED) {
            return "differences detected and manual compensation is required";
        }
        if (!differences.isEmpty()) {
            return "differences detected";
        }
        return "execution failed";
    }

    private SyncExecutionRecord completeAsFailure(
            SyncExchangeTask task,
            SyncExecutionRecord running,
            String checkpoint,
            SyncResultSummary resultSummary,
            List<SyncDifferenceItem> differences,
            String failureCode,
            String failureMessage
    ) {
        SyncExecutionRecord failed = running.fail(
                checkpoint,
                resultSummary,
                differences,
                failureCode,
                failureMessage,
                task.retryPolicy().canRetry(running.retryCount(), failureCode),
                SyncDiffSummary.from(differences).reconciliationStatus(),
                now()
        );
        executionRecordRepository.save(failed);
        publishFailed(task, failed);
        return failed;
    }

    private void clearCheckpointOverrideIfNeeded(SyncExchangeTask task) {
        if (task.checkpointMode() == null || task.checkpointConfig().manualOverrideValue() == null) {
            return;
        }
        taskRepository.save(task.clearCheckpointOverride(now()));
    }

    private void publishCompleted(SyncExchangeTask task, SyncExecutionRecord record) {
        eventPublisher.publish(new DataSyncCompletedEvent(
                task.tenantId().toString(),
                record.operatorAccountId(),
                task.taskId(),
                record.executionId(),
                task.code(),
                task.syncMode(),
                record.checkpointValue(),
                record.resultSummary().insertedCount(),
                record.resultSummary().updatedCount(),
                record.resultSummary().failedCount(),
                record.resultSummary().successCount(),
                record.finishedAt()
        ));
    }

    private void publishFailed(SyncExchangeTask task, SyncExecutionRecord record) {
        eventPublisher.publish(new DataSyncFailedEvent(
                task.tenantId().toString(),
                record.operatorAccountId(),
                task.taskId(),
                record.executionId(),
                task.code(),
                task.syncMode(),
                record.checkpointValue(),
                record.failureCode(),
                record.retryable(),
                record.finishedAt()
        ));
    }

    private SyncExecutionDetailView toDetailView(SyncExecutionRecord record) {
        return new SyncExecutionDetailView(
                new SyncExecutionSummaryView(
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
                ),
                record.differenceItems(),
                record.triggerContext(),
                record.operatorAccountId(),
                record.operatorPersonId()
        );
    }

    private Instant now() {
        return clock.instant();
    }

    private String stringValue(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
