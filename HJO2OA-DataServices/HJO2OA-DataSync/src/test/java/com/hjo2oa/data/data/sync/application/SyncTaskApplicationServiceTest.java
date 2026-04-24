package com.hjo2oa.data.data.sync.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.data.data.sync.DataSyncTestSupport;
import com.hjo2oa.data.data.sync.domain.CheckpointMode;
import com.hjo2oa.data.data.sync.domain.ConflictStrategy;
import com.hjo2oa.data.data.sync.domain.ConnectorDependencyStatus;
import com.hjo2oa.data.data.sync.domain.SyncCheckpointConfig;
import com.hjo2oa.data.data.sync.domain.SyncCompensationPolicy;
import com.hjo2oa.data.data.sync.domain.SyncMode;
import com.hjo2oa.data.data.sync.domain.SyncReconciliationPolicy;
import com.hjo2oa.data.data.sync.domain.SyncRetryPolicy;
import com.hjo2oa.data.data.sync.domain.SyncScheduleConfig;
import com.hjo2oa.data.data.sync.domain.SyncTaskFilter;
import com.hjo2oa.data.data.sync.domain.SyncTaskStatus;
import com.hjo2oa.data.data.sync.domain.SyncTriggerConfig;
import com.hjo2oa.data.data.sync.domain.SyncTaskType;
import com.hjo2oa.data.data.sync.infrastructure.InMemorySyncConnectorGateway;
import com.hjo2oa.data.data.sync.infrastructure.InMemorySyncExchangeTaskRepository;
import com.hjo2oa.data.data.sync.infrastructure.InMemorySyncExecutionRecordRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SyncTaskApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T02:00:00Z");

    private InMemorySyncExchangeTaskRepository taskRepository;
    private InMemorySyncExecutionRecordRepository executionRecordRepository;
    private InMemorySyncConnectorGateway connectorGateway;
    private SyncTaskApplicationService taskApplicationService;

    @BeforeEach
    void setUp() {
        taskRepository = new InMemorySyncExchangeTaskRepository();
        executionRecordRepository = new InMemorySyncExecutionRecordRepository();
        connectorGateway = new InMemorySyncConnectorGateway();
        connectorGateway.registerConnector(DataSyncTestSupport.SOURCE_CONNECTOR_ID, "ACTIVE");
        connectorGateway.registerConnector(DataSyncTestSupport.TARGET_CONNECTOR_ID, "ACTIVE");
        taskApplicationService = new SyncTaskApplicationService(
                taskRepository,
                executionRecordRepository,
                connectorGateway,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldCreateUpdateActivatePauseAndDeleteTask() {
        SyncTaskDetailView created = taskApplicationService.create(DataSyncTestSupport.createTaskCommand(
                "sync-task-crud",
                SyncMode.FULL,
                CheckpointMode.NONE,
                SyncCheckpointConfig.empty(),
                SyncTriggerConfig.manualOnly(),
                SyncRetryPolicy.manualOnly(),
                SyncCompensationPolicy.manualDefault(),
                new SyncReconciliationPolicy(false, false, false, 0),
                SyncScheduleConfig.disabled(),
                ConflictStrategy.OVERWRITE
        ));

        assertThat(created.summary().code()).isEqualTo("sync-task-crud");
        assertThat(created.summary().status()).isEqualTo(SyncTaskStatus.DRAFT);
        assertThat(created.mappingRules()).hasSize(2);
        assertThat(created.summary().dependencyStatus()).isEqualTo(ConnectorDependencyStatus.READY);

        SyncTaskDetailView updated = taskApplicationService.update(
                created.summary().taskId(),
                new UpdateSyncExchangeTaskCommand(
                        "sync-task-crud-updated",
                        "updated-description",
                        SyncTaskType.IMPORT,
                        SyncMode.INCREMENTAL,
                        DataSyncTestSupport.SOURCE_CONNECTOR_ID,
                        DataSyncTestSupport.TARGET_CONNECTOR_ID,
                        CheckpointMode.OFFSET,
                        new SyncCheckpointConfig("offset", "eventId", true, "10", null, null, null, null),
                        new SyncTriggerConfig(true, java.util.List.of(), null),
                        new SyncRetryPolicy(2, true, false, java.util.List.of()),
                        SyncCompensationPolicy.manualDefault(),
                        new SyncReconciliationPolicy(false, false, false, 0),
                        SyncScheduleConfig.disabled(),
                        DataSyncTestSupport.defaultMappingRules(ConflictStrategy.OVERWRITE)
                )
        );

        assertThat(updated.summary().name()).isEqualTo("sync-task-crud-updated");
        assertThat(updated.summary().syncMode()).isEqualTo(SyncMode.INCREMENTAL);
        assertThat(updated.summary().checkpointMode()).isEqualTo(CheckpointMode.OFFSET);

        SyncTaskDetailView activated = taskApplicationService.activate(created.summary().taskId());
        assertThat(activated.summary().status()).isEqualTo(SyncTaskStatus.ACTIVE);

        SyncTaskDetailView paused = taskApplicationService.pause(created.summary().taskId());
        assertThat(paused.summary().status()).isEqualTo(SyncTaskStatus.PAUSED);

        assertThat(taskApplicationService.pageTasks(new SyncTaskFilter(
                DataSyncTestSupport.TENANT_ID,
                "sync-task-crud",
                null,
                null,
                null,
                null,
                1,
                20
        )).total()).isEqualTo(1);

        taskApplicationService.delete(created.summary().taskId());
        assertThat(taskApplicationService.pageTasks(new SyncTaskFilter(
                DataSyncTestSupport.TENANT_ID,
                null,
                null,
                null,
                null,
                null,
                1,
                20
        )).total()).isZero();
    }

    @Test
    void shouldRefreshConnectorDependencyStatusWhenConnectorChanges() {
        SyncTaskDetailView created = taskApplicationService.create(DataSyncTestSupport.createTaskCommand(
                "sync-task-dependency",
                SyncMode.FULL,
                CheckpointMode.NONE,
                SyncCheckpointConfig.empty(),
                SyncTriggerConfig.manualOnly(),
                SyncRetryPolicy.manualOnly(),
                SyncCompensationPolicy.manualDefault(),
                new SyncReconciliationPolicy(false, false, false, 0),
                SyncScheduleConfig.disabled(),
                ConflictStrategy.OVERWRITE
        ));

        taskApplicationService.refreshConnectorDependency(DataSyncTestSupport.SOURCE_CONNECTOR_ID, "DISABLED");
        assertThat(taskApplicationService.getTask(created.summary().taskId()).summary().dependencyStatus())
                .isEqualTo(ConnectorDependencyStatus.SOURCE_UNAVAILABLE);

        taskApplicationService.refreshConnectorDependency(DataSyncTestSupport.SOURCE_CONNECTOR_ID, "ACTIVE");
        assertThat(taskApplicationService.getTask(created.summary().taskId()).summary().dependencyStatus())
                .isEqualTo(ConnectorDependencyStatus.READY);
    }

    @Test
    void shouldResetCheckpointWithManualOverride() {
        SyncTaskDetailView created = taskApplicationService.create(DataSyncTestSupport.createTaskCommand(
                "sync-task-checkpoint",
                SyncMode.INCREMENTAL,
                CheckpointMode.OFFSET,
                new SyncCheckpointConfig("offset", "eventId", true, "100", null, null, null, null),
                SyncTriggerConfig.manualOnly(),
                SyncRetryPolicy.manualOnly(),
                SyncCompensationPolicy.manualDefault(),
                new SyncReconciliationPolicy(false, false, false, 0),
                SyncScheduleConfig.disabled(),
                ConflictStrategy.OVERWRITE
        ));

        SyncTaskDetailView updated = taskApplicationService.resetCheckpoint(
                created.summary().taskId(),
                new ResetSyncCheckpointCommand("150", "ops-admin", "rewind checkpoint")
        );

        assertThat(updated.checkpointConfig().manualOverrideValue()).isEqualTo("150");
        assertThat(updated.checkpointConfig().lastResetBy()).isEqualTo("ops-admin");
        assertThat(updated.checkpointConfig().lastResetReason()).isEqualTo("rewind checkpoint");
    }
}
