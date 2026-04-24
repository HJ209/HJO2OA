package com.hjo2oa.data.data.sync.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.data.data.sync.DataSyncTestSupport;
import com.hjo2oa.data.data.sync.domain.CheckpointMode;
import com.hjo2oa.data.data.sync.domain.CompensationAction;
import com.hjo2oa.data.data.sync.domain.ConflictStrategy;
import com.hjo2oa.data.data.sync.domain.DataSyncCompletedEvent;
import com.hjo2oa.data.data.sync.domain.DataSyncFailedEvent;
import com.hjo2oa.data.data.sync.domain.ExecutionStatus;
import com.hjo2oa.data.data.sync.domain.ExecutionTriggerType;
import com.hjo2oa.data.data.sync.domain.ReconciliationStatus;
import com.hjo2oa.data.data.sync.domain.SyncCheckpointConfig;
import com.hjo2oa.data.data.sync.domain.SyncCompensationDecision;
import com.hjo2oa.data.data.sync.domain.SyncCompensationPolicy;
import com.hjo2oa.data.data.sync.domain.SyncExecutionFilter;
import com.hjo2oa.data.data.sync.domain.SyncMode;
import com.hjo2oa.data.data.sync.domain.SyncPullBatch;
import com.hjo2oa.data.data.sync.domain.SyncPushResult;
import com.hjo2oa.data.data.sync.domain.SyncReconciliationPolicy;
import com.hjo2oa.data.data.sync.domain.SyncResultSummary;
import com.hjo2oa.data.data.sync.domain.SyncRetryPolicy;
import com.hjo2oa.data.data.sync.domain.SyncScheduleConfig;
import com.hjo2oa.data.data.sync.domain.SyncTriggerConfig;
import com.hjo2oa.data.data.sync.infrastructure.InMemorySyncConnectorGateway;
import com.hjo2oa.data.data.sync.infrastructure.InMemorySyncExchangeTaskRepository;
import com.hjo2oa.data.data.sync.infrastructure.InMemorySyncExecutionRecordRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SyncExecutionApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T03:00:00Z");

    private InMemorySyncExchangeTaskRepository taskRepository;
    private InMemorySyncExecutionRecordRepository executionRecordRepository;
    private InMemorySyncConnectorGateway connectorGateway;
    private DataSyncTestSupport.CollectingDataDomainEventPublisher eventPublisher;
    private SyncTaskApplicationService taskApplicationService;
    private SyncExecutionApplicationService executionApplicationService;

    @BeforeEach
    void setUp() {
        taskRepository = new InMemorySyncExchangeTaskRepository();
        executionRecordRepository = new InMemorySyncExecutionRecordRepository();
        connectorGateway = new InMemorySyncConnectorGateway();
        connectorGateway.registerConnector(DataSyncTestSupport.SOURCE_CONNECTOR_ID, "ACTIVE");
        connectorGateway.registerConnector(DataSyncTestSupport.TARGET_CONNECTOR_ID, "ACTIVE");
        eventPublisher = new DataSyncTestSupport.CollectingDataDomainEventPublisher();
        Clock clock = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);
        taskApplicationService = new SyncTaskApplicationService(
                taskRepository,
                executionRecordRepository,
                connectorGateway,
                clock
        );
        executionApplicationService = new SyncExecutionApplicationService(
                taskRepository,
                executionRecordRepository,
                connectorGateway,
                eventPublisher,
                clock
        );
    }

    @Test
    void shouldExecuteFullSyncAndPublishCompletedEvent() {
        SyncTaskDetailView task = createAndActivateTask(
                "sync-full-success",
                SyncMode.FULL,
                CheckpointMode.NONE,
                SyncCheckpointConfig.empty(),
                SyncTriggerConfig.manualOnly(),
                SyncRetryPolicy.manualOnly(),
                SyncCompensationPolicy.manualDefault(),
                new SyncReconciliationPolicy(false, false, false, 0),
                SyncScheduleConfig.disabled(),
                ConflictStrategy.OVERWRITE
        );
        connectorGateway.upsertRecord(
                DataSyncTestSupport.SOURCE_CONNECTOR_ID,
                DataSyncTestSupport.payloadRecord(
                        "user-1",
                        "1",
                        null,
                        FIXED_TIME,
                        Map.of("id", "user-1", "name", " Alice ")
                )
        );

        SyncExecutionDetailView triggered = executionApplicationService.triggerTask(
                task.summary().taskId(),
                new TriggerSyncTaskCommand("manual-batch-1", "ops-admin", "person-1", Map.of("source", "manual"))
        );
        SyncExecutionDetailView idempotentReplay = executionApplicationService.triggerTask(
                task.summary().taskId(),
                new TriggerSyncTaskCommand("manual-batch-1", "ops-admin", "person-1", Map.of("source", "manual"))
        );

        assertThat(triggered.summary().executionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(triggered.summary().resultSummary().sourceCount()).isEqualTo(1);
        assertThat(triggered.summary().resultSummary().insertedCount()).isEqualTo(1);
        assertThat(triggered.summary().checkpointValue()).isEqualTo("1");
        assertThat(idempotentReplay.summary().executionId()).isEqualTo(triggered.summary().executionId());
        assertThat(connectorGateway.records(DataSyncTestSupport.TARGET_CONNECTOR_ID))
                .singleElement()
                .satisfies(record -> assertThat(record.payload()).containsEntry("name", "Alice"));
        assertThat(eventPublisher.eventsOfType(DataSyncCompletedEvent.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.eventType()).isEqualTo("data.sync.completed");
                    assertThat(event.code()).isEqualTo("sync-full-success");
                    assertThat(event.checkpoint()).isEqualTo("1");
                });
    }

    @Test
    void shouldRespectIncrementalCheckpointAndClearManualOverrideAfterSuccess() {
        SyncTaskDetailView task = createAndActivateTask(
                "sync-incremental-checkpoint",
                SyncMode.INCREMENTAL,
                CheckpointMode.OFFSET,
                new SyncCheckpointConfig("offset", "eventId", true, "10", null, null, null, null),
                SyncTriggerConfig.manualOnly(),
                SyncRetryPolicy.manualOnly(),
                SyncCompensationPolicy.manualDefault(),
                new SyncReconciliationPolicy(false, false, false, 0),
                SyncScheduleConfig.disabled(),
                ConflictStrategy.OVERWRITE
        );
        connectorGateway.upsertRecord(
                DataSyncTestSupport.SOURCE_CONNECTOR_ID,
                DataSyncTestSupport.payloadRecord("user-10", "10", null, FIXED_TIME, Map.of("id", "user-10", "name", "Ignored"))
        );
        connectorGateway.upsertRecord(
                DataSyncTestSupport.SOURCE_CONNECTOR_ID,
                DataSyncTestSupport.payloadRecord("user-11", "11", null, FIXED_TIME, Map.of("id", "user-11", "name", "First"))
        );
        connectorGateway.upsertRecord(
                DataSyncTestSupport.SOURCE_CONNECTOR_ID,
                DataSyncTestSupport.payloadRecord("user-12", "12", null, FIXED_TIME, Map.of("id", "user-12", "name", "Second"))
        );

        SyncExecutionDetailView firstExecution = executionApplicationService.triggerTask(
                task.summary().taskId(),
                new TriggerSyncTaskCommand("incremental-1", "ops-admin", "person-1", Map.of())
        );

        assertThat(firstExecution.summary().executionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(firstExecution.summary().resultSummary().sourceCount()).isEqualTo(2);
        assertThat(firstExecution.summary().checkpointValue()).isEqualTo("12");

        taskApplicationService.resetCheckpoint(
                task.summary().taskId(),
                new ResetSyncCheckpointCommand("11", "ops-admin", "rerun from checkpoint")
        );
        connectorGateway.upsertRecord(
                DataSyncTestSupport.SOURCE_CONNECTOR_ID,
                DataSyncTestSupport.payloadRecord("user-13", "13", null, FIXED_TIME, Map.of("id", "user-13", "name", "Third"))
        );

        SyncExecutionDetailView secondExecution = executionApplicationService.triggerTask(
                task.summary().taskId(),
                new TriggerSyncTaskCommand("incremental-2", "ops-admin", "person-1", Map.of())
        );

        assertThat(secondExecution.summary().executionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(secondExecution.summary().resultSummary().sourceCount()).isEqualTo(2);
        assertThat(secondExecution.summary().resultSummary().updatedCount()).isZero();
        assertThat(secondExecution.summary().resultSummary().insertedCount()).isEqualTo(1);
        assertThat(secondExecution.summary().resultSummary().skippedCount()).isEqualTo(1);
        assertThat(secondExecution.summary().checkpointValue()).isEqualTo("13");
        assertThat(taskApplicationService.getTask(task.summary().taskId()).checkpointConfig().manualOverrideValue()).isNull();
    }

    @Test
    void shouldConsumeBusinessEventInEventDrivenModeIdempotently() {
        SyncTaskDetailView task = createAndActivateTask(
                "sync-event-driven",
                SyncMode.EVENT_DRIVEN,
                CheckpointMode.VERSION,
                new SyncCheckpointConfig("version", "eventId", false, "0", null, null, null, null),
                new SyncTriggerConfig(true, List.of("org.person.*"), null),
                SyncRetryPolicy.manualOnly(),
                SyncCompensationPolicy.manualDefault(),
                new SyncReconciliationPolicy(false, false, false, 0),
                SyncScheduleConfig.disabled(),
                ConflictStrategy.OVERWRITE
        );
        UUID eventId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        connectorGateway.upsertRecord(
                DataSyncTestSupport.SOURCE_CONNECTOR_ID,
                DataSyncTestSupport.payloadRecord(
                        "person-1",
                        "1",
                        eventId.toString(),
                        FIXED_TIME,
                        Map.of("id", "person-1", "name", "Event User")
                )
        );

        DataSyncTestSupport.TestDataEvent event = new DataSyncTestSupport.TestDataEvent(
                eventId,
                FIXED_TIME,
                "org.person.updated",
                Map.of("personId", "person-1")
        );

        executionApplicationService.onBusinessEvent(event, Map.of("personId", "person-1"));
        executionApplicationService.onBusinessEvent(event, Map.of("personId", "person-1"));

        assertThat(executionRecordRepository.page(new SyncExecutionFilter(
                task.summary().taskId(),
                null,
                null,
                null,
                null,
                null,
                1,
                20
        )).total()).isEqualTo(1);
        assertThat(connectorGateway.records(DataSyncTestSupport.TARGET_CONNECTOR_ID))
                .singleElement()
                .satisfies(record -> assertThat(record.recordKey()).isEqualTo("person-1"));
    }

    @Test
    void shouldUseStableIdempotencyKeyForSchedulerTriggeredEvents() {
        SyncTaskDetailView task = createAndActivateTask(
                "sync-scheduler-event",
                SyncMode.INCREMENTAL,
                CheckpointMode.OFFSET,
                new SyncCheckpointConfig("offset", "eventId", false, "0", null, null, null, null),
                SyncTriggerConfig.manualOnly(),
                SyncRetryPolicy.manualOnly(),
                SyncCompensationPolicy.manualDefault(),
                new SyncReconciliationPolicy(false, false, false, 0),
                new SyncScheduleConfig(true, "0 * * * * *", "UTC", "sync.scheduler.job"),
                ConflictStrategy.OVERWRITE
        );
        connectorGateway.upsertRecord(
                DataSyncTestSupport.SOURCE_CONNECTOR_ID,
                DataSyncTestSupport.payloadRecord("schedule-1", "1", null, FIXED_TIME, Map.of("id", "schedule-1", "name", "Scheduled"))
        );

        Map<String, Object> triggerContext = Map.of(
                "eventId", "scheduler-event-1",
                "triggerAt", "2026-04-24T03:00:00Z"
        );
        executionApplicationService.onSchedulerTrigger("sync.scheduler.job", triggerContext);
        executionApplicationService.onSchedulerTrigger("sync.scheduler.job", triggerContext);

        assertThat(executionRecordRepository.page(new SyncExecutionFilter(
                task.summary().taskId(),
                null,
                null,
                null,
                null,
                null,
                1,
                20
        )).total()).isEqualTo(1);
    }

    @Test
    void shouldMarkExecutionFailedRetryableAndAllowRetry() {
        FlakyPushConnectorGateway flakyGateway = new FlakyPushConnectorGateway();
        flakyGateway.registerConnector(DataSyncTestSupport.SOURCE_CONNECTOR_ID, "ACTIVE");
        flakyGateway.registerConnector(DataSyncTestSupport.TARGET_CONNECTOR_ID, "ACTIVE");
        SyncTaskApplicationService taskService = new SyncTaskApplicationService(
                taskRepository,
                executionRecordRepository,
                flakyGateway,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        SyncExecutionApplicationService executionService = new SyncExecutionApplicationService(
                taskRepository,
                executionRecordRepository,
                flakyGateway,
                eventPublisher,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        SyncTaskDetailView task = taskService.create(DataSyncTestSupport.createTaskCommand(
                "sync-retryable-failure",
                SyncMode.FULL,
                CheckpointMode.NONE,
                SyncCheckpointConfig.empty(),
                SyncTriggerConfig.manualOnly(),
                new SyncRetryPolicy(3, true, false, List.of("TARGET_WRITE_FAILED")),
                SyncCompensationPolicy.manualDefault(),
                new SyncReconciliationPolicy(false, false, false, 0),
                SyncScheduleConfig.disabled(),
                ConflictStrategy.OVERWRITE
        ));
        taskService.activate(task.summary().taskId());
        flakyGateway.upsertRecord(
                DataSyncTestSupport.SOURCE_CONNECTOR_ID,
                DataSyncTestSupport.payloadRecord("retry-1", "1", null, FIXED_TIME, Map.of("id", "retry-1", "name", "Retry User"))
        );

        SyncExecutionDetailView failed = executionService.triggerTask(
                task.summary().taskId(),
                new TriggerSyncTaskCommand("fail-once", "ops-admin", "person-1", Map.of())
        );

        assertThat(failed.summary().executionStatus()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(failed.summary().retryable()).isTrue();
        assertThat(failed.summary().failureCode()).isEqualTo("TARGET_WRITE_FAILED");
        assertThat(eventPublisher.eventsOfType(DataSyncFailedEvent.class))
                .isNotEmpty()
                .last()
                .satisfies(event -> assertThat(event.eventType()).isEqualTo("data.sync.failed"));

        flakyGateway.failPush = false;
        SyncExecutionDetailView retried = executionService.retryExecution(
                failed.summary().executionId(),
                new RetrySyncExecutionCommand("retry-once", "ops-admin", "person-1", "retry target failure")
        );

        assertThat(retried.summary().executionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(retried.summary().parentExecutionId()).isEqualTo(failed.summary().executionId());
        assertThat(retried.summary().retryCount()).isEqualTo(1);
    }

    @Test
    void shouldDetectDifferencesAllowManualCompensationAndReconcileSuccessfully() {
        SyncTaskDetailView task = createAndActivateTask(
                "sync-diff-compensation",
                SyncMode.FULL,
                CheckpointMode.NONE,
                SyncCheckpointConfig.empty(),
                SyncTriggerConfig.manualOnly(),
                SyncRetryPolicy.manualOnly(),
                SyncCompensationPolicy.manualDefault(),
                new SyncReconciliationPolicy(true, true, true, 0),
                SyncScheduleConfig.disabled(),
                ConflictStrategy.OVERWRITE
        );
        connectorGateway.upsertRecord(
                DataSyncTestSupport.SOURCE_CONNECTOR_ID,
                DataSyncTestSupport.payloadRecord("user-1", "1", null, FIXED_TIME, Map.of("id", "user-1", "name", "Source User"))
        );
        connectorGateway.upsertRecord(
                DataSyncTestSupport.TARGET_CONNECTOR_ID,
                DataSyncTestSupport.payloadRecord("stale-1", "9", null, FIXED_TIME, Map.of("id", "stale-1", "name", "Stale User"))
        );

        SyncExecutionDetailView failed = executionApplicationService.triggerTask(
                task.summary().taskId(),
                new TriggerSyncTaskCommand("diff-batch", "ops-admin", "person-1", Map.of())
        );

        assertThat(failed.summary().executionStatus()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(failed.summary().reconciliationStatus()).isEqualTo(ReconciliationStatus.MANUAL_REVIEW_REQUIRED);
        assertThat(failed.differences()).hasSize(1);

        SyncExecutionDetailView compensated = executionApplicationService.compensateExecution(
                failed.summary().executionId(),
                new SubmitManualCompensationCommand(
                        "compensate-1",
                        "ops-admin",
                        "person-1",
                        "remove stale target record",
                        List.of(new SyncCompensationDecision(
                                failed.differences().get(0).differenceCode(),
                                CompensationAction.RETRY_WRITE,
                                "delete stale target"
                        ))
                )
        );

        assertThat(compensated.summary().executionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(compensated.differences()).singleElement()
                .satisfies(item -> assertThat(item.resolved()).isTrue());

        SyncExecutionDetailView reconciled = executionApplicationService.reconcileExecution(
                failed.summary().executionId(),
                new ReconcileSyncExecutionCommand("reconcile-1", "ops-admin", "person-1", "verify after compensation")
        );

        assertThat(reconciled.summary().executionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(reconciled.summary().triggerType()).isEqualTo(ExecutionTriggerType.RECONCILIATION);
        assertThat(reconciled.summary().reconciliationStatus()).isEqualTo(ReconciliationStatus.CONSISTENT);
        assertThat(reconciled.differences()).isEmpty();
    }

    private SyncTaskDetailView createAndActivateTask(
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
        SyncTaskDetailView created = taskApplicationService.create(DataSyncTestSupport.createTaskCommand(
                code,
                syncMode,
                checkpointMode,
                checkpointConfig,
                triggerConfig,
                retryPolicy,
                compensationPolicy,
                reconciliationPolicy,
                scheduleConfig,
                conflictStrategy
        ));
        return taskApplicationService.activate(created.summary().taskId());
    }

    private static final class FlakyPushConnectorGateway extends InMemorySyncConnectorGateway {

        private boolean failPush = true;

        @Override
        public SyncPushResult push(
                com.hjo2oa.data.data.sync.domain.SyncExchangeTask task,
                List<com.hjo2oa.data.data.sync.domain.SyncMappedRecord> mappedRecords
        ) {
            if (failPush) {
                return new SyncPushResult(new SyncResultSummary(mappedRecords.size(), 0, 0, 0, 1), List.of());
            }
            return super.push(task, mappedRecords);
        }

        @Override
        public SyncPullBatch pull(
                com.hjo2oa.data.data.sync.domain.SyncExchangeTask task,
                String startCheckpoint,
                ExecutionTriggerType triggerType,
                Map<String, Object> triggerContext
        ) {
            return super.pull(task, startCheckpoint, triggerType, triggerContext);
        }
    }
}
