package com.hjo2oa.infra.scheduler.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.infra.scheduler.domain.ConcurrencyPolicy;
import com.hjo2oa.infra.scheduler.domain.ExecutionStatus;
import com.hjo2oa.infra.scheduler.domain.JobExecutionRecordView;
import com.hjo2oa.infra.scheduler.domain.ScheduledJobView;
import com.hjo2oa.infra.scheduler.domain.SchedulerTaskFailedEvent;
import com.hjo2oa.infra.scheduler.domain.SchedulerTaskRetryingEvent;
import com.hjo2oa.infra.scheduler.domain.SchedulerTaskSucceededEvent;
import com.hjo2oa.infra.scheduler.domain.TriggerSource;
import com.hjo2oa.infra.scheduler.domain.TriggerType;
import com.hjo2oa.infra.scheduler.infrastructure.InMemoryJobExecutionRecordRepository;
import com.hjo2oa.infra.scheduler.infrastructure.InMemoryScheduledJobRepository;
import com.hjo2oa.infra.scheduler.infrastructure.InMemorySchedulerJobLock;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class ScheduledJobApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T00:00:00Z");

    @Test
    void shouldRegisterPauseResumeEnableAndDisableJob() {
        try (Fixture fixture = new Fixture()) {
            ScheduledJobView registered = fixture.applicationService.registerJob(
                    "nightly-cleanup",
                    "success-handler",
                    "Nightly Cleanup",
                    TriggerType.CRON,
                    "0 0 2 * * *",
                    "Asia/Shanghai",
                    ConcurrencyPolicy.FORBID,
                    300,
                    "{\"maxRetries\":2}",
                    null
            );

            assertThat(registered.status().name()).isEqualTo("ACTIVE");
            assertThat(registered.handlerName()).isEqualTo("success-handler");
            assertThat(registered.cronExpr()).isEqualTo("0 0 2 * * *");

            ScheduledJobView paused = fixture.applicationService.pauseJob(registered.id());
            assertThat(paused.status().name()).isEqualTo("PAUSED");

            ScheduledJobView resumed = fixture.applicationService.resumeJob(registered.id());
            assertThat(resumed.status().name()).isEqualTo("ACTIVE");

            ScheduledJobView disabled = fixture.applicationService.disableJob(registered.id());
            assertThat(disabled.status().name()).isEqualTo("DISABLED");

            ScheduledJobView enabled = fixture.applicationService.enableJob(registered.id());
            assertThat(enabled.status().name()).isEqualTo("ACTIVE");
        }
    }

    @Test
    void shouldExecuteManualTriggerAndRecordSuccess() {
        try (Fixture fixture = new Fixture()) {
            ScheduledJobView job = fixture.applicationService.registerJob(
                    "sync-users",
                    "success-handler",
                    "Sync Users",
                    TriggerType.MANUAL,
                    null,
                    null,
                    ConcurrencyPolicy.FORBID,
                    120,
                    null,
                    null
            );

            JobExecutionRecordView execution = fixture.applicationService.triggerJob(job.id(), operationContext());

            assertThat(execution.executionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
            assertThat(execution.triggerSource()).isEqualTo(TriggerSource.MANUAL);
            assertThat(execution.durationMs()).isZero();
            assertThat(execution.executionLog()).contains("success");
            assertThat(fixture.events).singleElement().isInstanceOf(SchedulerTaskSucceededEvent.class);
        }
    }

    @Test
    void shouldRunCronTriggerThroughRuntimeService() throws Exception {
        try (Fixture fixture = new Fixture()) {
            ScheduledJobView job = fixture.applicationService.registerJob(
                    "cron-refresh",
                    "cron-handler",
                    "Cron Refresh",
                    TriggerType.CRON,
                    "*/1 * * * * *",
                    "UTC",
                    ConcurrencyPolicy.FORBID,
                    30,
                    null,
                    null
            );
            SchedulerRuntimeService runtimeService = new SchedulerRuntimeService(
                    fixture.scheduledJobRepository,
                    fixture.executionService,
                    fixture.taskScheduler
            );

            runtimeService.refreshJob(job.id());

            assertThat(fixture.cronLatch.await(3, TimeUnit.SECONDS)).isTrue();
            awaitSuccessfulCronExecution(fixture, job);
            runtimeService.stop();
        }
    }

    @Test
    void shouldRecordFailureRetryAndPublishEvents() throws Exception {
        try (Fixture fixture = new Fixture()) {
            ScheduledJobView job = fixture.applicationService.registerJob(
                    "failing-sync",
                    "failing-handler",
                    "Failing Sync",
                    TriggerType.MANUAL,
                    null,
                    null,
                    ConcurrencyPolicy.FORBID,
                    120,
                    "{\"maxRetries\":1,\"backoffSeconds\":0}",
                    null
            );

            JobExecutionRecordView firstAttempt = fixture.applicationService.triggerJob(job.id(), operationContext());

            assertThat(firstAttempt.executionStatus()).isEqualTo(ExecutionStatus.RETRYING);
            assertThat(fixture.failureLatch.await(2, TimeUnit.SECONDS)).isTrue();
            List<JobExecutionRecordView> executions = fixture.applicationService.queryExecutions(job.id(), null, null);
            assertThat(executions).hasSize(2);
            assertThat(executions).anySatisfy(execution -> {
                assertThat(execution.executionStatus()).isEqualTo(ExecutionStatus.RETRYING);
                assertThat(execution.nextRetryAt()).isEqualTo(FIXED_TIME);
            });
            assertThat(executions).anySatisfy(execution -> {
                assertThat(execution.executionStatus()).isEqualTo(ExecutionStatus.FAILED);
                assertThat(execution.errorStack()).contains("IllegalStateException");
            });
            assertThat(fixture.events).anyMatch(event -> event instanceof SchedulerTaskRetryingEvent);
            assertThat(fixture.events).anyMatch(event -> event instanceof SchedulerTaskFailedEvent);
        }
    }

    @Test
    void shouldReplaceRunningExecutionWhenConcurrencyPolicyIsReplace() {
        try (Fixture fixture = new Fixture()) {
            ScheduledJobView job = fixture.applicationService.registerJob(
                    "refresh-cache",
                    "success-handler",
                    "Refresh Cache",
                    TriggerType.MANUAL,
                    null,
                    null,
                    ConcurrencyPolicy.REPLACE,
                    60,
                    null,
                    null
            );

            JobExecutionRecordView first = fixture.applicationService.recordStart(job.id(), TriggerSource.MANUAL);
            JobExecutionRecordView second = fixture.applicationService.recordStart(job.id(), TriggerSource.MANUAL);
            List<JobExecutionRecordView> executions = fixture.applicationService.queryExecutions(job.id(), null, null);

            assertThat(second.executionStatus()).isEqualTo(ExecutionStatus.RUNNING);
            assertThat(executions).hasSize(2);
            assertThat(executions).anySatisfy(execution -> {
                assertThat(execution.id()).isEqualTo(first.id());
                assertThat(execution.executionStatus()).isEqualTo(ExecutionStatus.CANCELLED);
                assertThat(execution.errorCode()).isEqualTo("REPLACED");
            });
        }
    }

    private static SchedulerOperationContext operationContext() {
        return new SchedulerOperationContext(
                null,
                null,
                null,
                "req-scheduler-test",
                "idem-scheduler-test",
                "zh-CN",
                "Asia/Shanghai",
                "test"
        );
    }

    private static void awaitSuccessfulCronExecution(Fixture fixture, ScheduledJobView job) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        List<JobExecutionRecordView> executions;
        do {
            executions = fixture.applicationService.queryExecutions(job.id(), null, null);
            boolean hasSuccess = executions.stream()
                    .anyMatch(execution -> execution.triggerSource() == TriggerSource.CRON
                            && execution.executionStatus() == ExecutionStatus.SUCCESS);
            if (hasSuccess) {
                return;
            }
            Thread.sleep(20);
        } while (System.nanoTime() < deadline);

        assertThat(fixture.applicationService.queryExecutions(job.id(), null, null))
                .anySatisfy(execution -> {
                    assertThat(execution.triggerSource()).isEqualTo(TriggerSource.CRON);
                    assertThat(execution.executionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
                });
    }

    private static final class Fixture implements AutoCloseable {

        private final CountDownLatch cronLatch = new CountDownLatch(1);
        private final CountDownLatch failureLatch = new CountDownLatch(2);
        private final List<DomainEvent> events = new ArrayList<>();
        private final InMemoryScheduledJobRepository scheduledJobRepository = new InMemoryScheduledJobRepository();
        private final InMemoryJobExecutionRecordRepository executionRecordRepository =
                new InMemoryJobExecutionRecordRepository();
        private final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        private final ExecutorService handlerExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });
        private final SchedulerExecutionService executionService;
        private final ScheduledJobApplicationService applicationService;

        private Fixture() {
            taskScheduler.setPoolSize(2);
            taskScheduler.initialize();
            SchedulerJobHandlerRegistry handlerRegistry = new SchedulerJobHandlerRegistry(List.of(
                    new SimpleHandler("success-handler", null),
                    new SimpleHandler("cron-handler", cronLatch),
                    new FailingHandler(failureLatch)
            ));
            executionService = new SchedulerExecutionService(
                    scheduledJobRepository,
                    executionRecordRepository,
                    handlerRegistry,
                    new InMemorySchedulerJobLock(),
                    events::add,
                    taskScheduler,
                    handlerExecutor,
                    Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
            );
            applicationService = new ScheduledJobApplicationService(
                    scheduledJobRepository,
                    executionRecordRepository,
                    events::add,
                    executionService,
                    Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
            );
        }

        @Override
        public void close() {
            taskScheduler.shutdown();
            handlerExecutor.shutdownNow();
        }
    }

    private record SimpleHandler(String handlerName, CountDownLatch latch) implements SchedulerJobHandler {

        @Override
        public SchedulerJobResult execute(SchedulerJobExecutionContext context) {
            if (latch != null) {
                latch.countDown();
            }
            return SchedulerJobResult.success("{\"result\":\"success\"}");
        }
    }

    private record FailingHandler(CountDownLatch latch) implements SchedulerJobHandler {

        @Override
        public String handlerName() {
            return "failing-handler";
        }

        @Override
        public SchedulerJobResult execute(SchedulerJobExecutionContext context) {
            latch.countDown();
            throw new IllegalStateException("planned failure");
        }
    }
}
