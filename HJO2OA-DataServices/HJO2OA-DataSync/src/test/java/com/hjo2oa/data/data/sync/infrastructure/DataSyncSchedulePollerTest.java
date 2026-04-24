package com.hjo2oa.data.data.sync.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.hjo2oa.data.data.sync.DataSyncTestSupport;
import com.hjo2oa.data.data.sync.application.SyncExecutionApplicationService;
import com.hjo2oa.data.data.sync.application.SyncTaskApplicationService;
import com.hjo2oa.data.data.sync.domain.CheckpointMode;
import com.hjo2oa.data.data.sync.domain.ConflictStrategy;
import com.hjo2oa.data.data.sync.domain.SyncCheckpointConfig;
import com.hjo2oa.data.data.sync.domain.SyncCompensationPolicy;
import com.hjo2oa.data.data.sync.domain.SyncMode;
import com.hjo2oa.data.data.sync.domain.SyncReconciliationPolicy;
import com.hjo2oa.data.data.sync.domain.SyncRetryPolicy;
import com.hjo2oa.data.data.sync.domain.SyncScheduleConfig;
import com.hjo2oa.data.data.sync.domain.SyncTriggerConfig;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DataSyncSchedulePollerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T06:01:00Z");

    private InMemorySyncExchangeTaskRepository taskRepository;
    private SyncExecutionApplicationService executionApplicationService;
    private SyncTaskApplicationService taskApplicationService;

    @BeforeEach
    void setUp() {
        taskRepository = new InMemorySyncExchangeTaskRepository();
        InMemorySyncExecutionRecordRepository executionRecordRepository = new InMemorySyncExecutionRecordRepository();
        InMemorySyncConnectorGateway connectorGateway = new InMemorySyncConnectorGateway();
        connectorGateway.registerConnector(DataSyncTestSupport.SOURCE_CONNECTOR_ID, "ACTIVE");
        connectorGateway.registerConnector(DataSyncTestSupport.TARGET_CONNECTOR_ID, "ACTIVE");
        executionApplicationService = mock(SyncExecutionApplicationService.class);
        taskApplicationService = new SyncTaskApplicationService(
                taskRepository,
                executionRecordRepository,
                connectorGateway,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldTriggerScheduledTaskWithDeterministicBatchKey() {
        var task = taskApplicationService.create(DataSyncTestSupport.createTaskCommand(
                "sync-cron-job",
                SyncMode.INCREMENTAL,
                CheckpointMode.OFFSET,
                new SyncCheckpointConfig("offset", "eventId", false, "0", null, null, null, null),
                SyncTriggerConfig.manualOnly(),
                SyncRetryPolicy.manualOnly(),
                SyncCompensationPolicy.manualDefault(),
                new SyncReconciliationPolicy(false, false, false, 0),
                new SyncScheduleConfig(true, "0 * * * * *", "UTC", "sync-cron-job"),
                ConflictStrategy.OVERWRITE
        ));
        taskApplicationService.activate(task.summary().taskId());
        DataSyncSchedulePoller poller = new DataSyncSchedulePoller(
                taskRepository,
                executionApplicationService,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);

        poller.scanAndTrigger();

        verify(executionApplicationService).triggerScheduledTask(
                eq(taskRepository.findById(task.summary().taskId()).orElseThrow()),
                eq("cron:" + task.summary().taskId() + ":1777010460000"),
                contextCaptor.capture()
        );
        assertThat(contextCaptor.getValue())
                .containsEntry("triggerAt", "2026-04-24T06:01:00Z")
                .containsEntry("jobCode", "sync-cron-job");
    }

    @Test
    void shouldNotRequireSchedulerJobCodeWhenCronIsPresent() {
        var task = taskApplicationService.create(DataSyncTestSupport.createTaskCommand(
                "sync-cron-no-job-code",
                SyncMode.INCREMENTAL,
                CheckpointMode.OFFSET,
                new SyncCheckpointConfig("offset", "eventId", false, "0", null, null, null, null),
                SyncTriggerConfig.manualOnly(),
                SyncRetryPolicy.manualOnly(),
                SyncCompensationPolicy.manualDefault(),
                new SyncReconciliationPolicy(false, false, false, 0),
                new SyncScheduleConfig(true, "0 * * * * *", "UTC", null),
                ConflictStrategy.OVERWRITE
        ));
        taskApplicationService.activate(task.summary().taskId());
        DataSyncSchedulePoller poller = new DataSyncSchedulePoller(
                taskRepository,
                executionApplicationService,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);

        poller.scanAndTrigger();

        verify(executionApplicationService).triggerScheduledTask(
                any(),
                eq("cron:" + task.summary().taskId() + ":1777010460000"),
                contextCaptor.capture()
        );
        assertThat(contextCaptor.getValue())
                .containsEntry("triggerAt", "2026-04-24T06:01:00Z")
                .doesNotContainKey("jobCode");
    }

    @Test
    void shouldTriggerWhenPollerRunsAfterCronBoundary() {
        var task = taskApplicationService.create(DataSyncTestSupport.createTaskCommand(
                "sync-cron-delayed-poller",
                SyncMode.INCREMENTAL,
                CheckpointMode.OFFSET,
                new SyncCheckpointConfig("offset", "eventId", false, "0", null, null, null, null),
                SyncTriggerConfig.manualOnly(),
                SyncRetryPolicy.manualOnly(),
                SyncCompensationPolicy.manualDefault(),
                new SyncReconciliationPolicy(false, false, false, 0),
                new SyncScheduleConfig(true, "0 * * * * *", "UTC", null),
                ConflictStrategy.OVERWRITE
        ));
        taskApplicationService.activate(task.summary().taskId());
        DataSyncSchedulePoller poller = new DataSyncSchedulePoller(
                taskRepository,
                executionApplicationService,
                Clock.fixed(FIXED_TIME.plus(Duration.ofMillis(250)), ZoneOffset.UTC)
        );
        ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);

        poller.scanAndTrigger();

        verify(executionApplicationService).triggerScheduledTask(
                any(),
                eq("cron:" + task.summary().taskId() + ":1777010460000"),
                contextCaptor.capture()
        );
        assertThat(contextCaptor.getValue())
                .containsEntry("triggerAt", "2026-04-24T06:01:00Z");
    }
}
