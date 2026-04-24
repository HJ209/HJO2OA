package com.hjo2oa.infra.scheduler.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.infra.scheduler.domain.ConcurrencyPolicy;
import com.hjo2oa.infra.scheduler.domain.ExecutionStatus;
import com.hjo2oa.infra.scheduler.domain.JobExecutionRecordView;
import com.hjo2oa.infra.scheduler.domain.ScheduledJobView;
import com.hjo2oa.infra.scheduler.domain.SchedulerTaskFailedEvent;
import com.hjo2oa.infra.scheduler.domain.TriggerType;
import com.hjo2oa.infra.scheduler.infrastructure.InMemoryJobExecutionRecordRepository;
import com.hjo2oa.infra.scheduler.infrastructure.InMemoryScheduledJobRepository;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScheduledJobApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T00:00:00Z");

    @Test
    void shouldRegisterPauseResumeAndDisableJob() {
        Fixture fixture = new Fixture();

        ScheduledJobView registered = fixture.applicationService.registerJob(
                "nightly-cleanup",
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
        assertThat(registered.cronExpr()).isEqualTo("0 0 2 * * *");

        ScheduledJobView paused = fixture.applicationService.pauseJob(registered.id());
        assertThat(paused.status().name()).isEqualTo("PAUSED");

        ScheduledJobView resumed = fixture.applicationService.resumeJob(registered.id());
        assertThat(resumed.status().name()).isEqualTo("ACTIVE");

        ScheduledJobView disabled = fixture.applicationService.disableJob(registered.id());
        assertThat(disabled.status().name()).isEqualTo("DISABLED");
    }

    @Test
    void shouldRecordFailureAndPublishTaskFailedEvent() {
        Fixture fixture = new Fixture();
        ScheduledJobView job = fixture.applicationService.registerJob(
                "sync-users",
                "Sync Users",
                TriggerType.MANUAL,
                null,
                null,
                ConcurrencyPolicy.FORBID,
                120,
                null,
                null
        );

        JobExecutionRecordView started = fixture.applicationService.triggerJob(job.jobCode());
        JobExecutionRecordView failed = fixture.applicationService.recordFailure(
                started.id(),
                "UPSTREAM_TIMEOUT",
                "Upstream request timed out"
        );

        assertThat(failed.executionStatus()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(fixture.events).singleElement().isInstanceOfSatisfying(SchedulerTaskFailedEvent.class, event -> {
            assertThat(event.eventType()).isEqualTo(SchedulerTaskFailedEvent.EVENT_TYPE);
            assertThat(event.jobCode()).isEqualTo(job.jobCode());
            assertThat(event.executionId()).isEqualTo(failed.id());
            assertThat(event.errorCode()).isEqualTo("UPSTREAM_TIMEOUT");
        });
    }

    @Test
    void shouldReplaceRunningExecutionWhenConcurrencyPolicyIsReplace() {
        Fixture fixture = new Fixture();
        ScheduledJobView job = fixture.applicationService.registerJob(
                "refresh-cache",
                "Refresh Cache",
                TriggerType.MANUAL,
                null,
                null,
                ConcurrencyPolicy.REPLACE,
                60,
                null,
                null
        );

        JobExecutionRecordView first = fixture.applicationService.triggerJob(job.jobCode());
        JobExecutionRecordView second = fixture.applicationService.triggerJob(job.jobCode());
        List<JobExecutionRecordView> executions = fixture.applicationService.queryExecutions(job.id(), null, null);

        assertThat(second.executionStatus()).isEqualTo(ExecutionStatus.RUNNING);
        assertThat(executions).hasSize(2);
        assertThat(executions).anySatisfy(execution -> {
            assertThat(execution.id()).isEqualTo(first.id());
            assertThat(execution.executionStatus()).isEqualTo(ExecutionStatus.CANCELLED);
            assertThat(execution.errorCode()).isEqualTo("REPLACED");
        });
    }

    private static final class Fixture {

        private final List<DomainEvent> events = new ArrayList<>();
        private final ScheduledJobApplicationService applicationService = new ScheduledJobApplicationService(
                new InMemoryScheduledJobRepository(),
                new InMemoryJobExecutionRecordRepository(),
                events::add,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
