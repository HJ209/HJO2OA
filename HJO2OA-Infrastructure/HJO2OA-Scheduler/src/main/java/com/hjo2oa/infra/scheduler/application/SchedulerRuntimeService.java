package com.hjo2oa.infra.scheduler.application;

import com.hjo2oa.infra.scheduler.domain.JobStatus;
import com.hjo2oa.infra.scheduler.domain.ScheduledJob;
import com.hjo2oa.infra.scheduler.domain.ScheduledJobRepository;
import com.hjo2oa.infra.scheduler.domain.TriggerSource;
import com.hjo2oa.infra.scheduler.domain.TriggerType;
import com.hjo2oa.shared.tenant.TenantContextHolder;
import com.hjo2oa.shared.tenant.TenantRequestContext;
import jakarta.annotation.PreDestroy;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

@Service
public class SchedulerRuntimeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerRuntimeService.class);

    private final ScheduledJobRepository scheduledJobRepository;
    private final SchedulerExecutionService executionService;
    private final TaskScheduler taskScheduler;
    private final Map<UUID, ScheduledFuture<?>> cronTasks = new ConcurrentHashMap<>();

    public SchedulerRuntimeService(
            ScheduledJobRepository scheduledJobRepository,
            SchedulerExecutionService executionService,
            TaskScheduler taskScheduler
    ) {
        this.scheduledJobRepository = Objects.requireNonNull(scheduledJobRepository, "scheduledJobRepository");
        this.executionService = Objects.requireNonNull(executionService, "executionService");
        this.taskScheduler = Objects.requireNonNull(taskScheduler, "taskScheduler");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        refreshAll();
    }

    public void refreshAll() {
        scheduledJobRepository.findAll().forEach(job -> {
            cancel(job.id());
            if (isSchedulableCronJob(job)) {
                scheduleCronJob(job);
            }
        });
    }

    public void refreshJob(UUID jobId) {
        cancel(jobId);
        scheduledJobRepository.findById(jobId)
                .filter(this::isSchedulableCronJob)
                .ifPresent(this::scheduleCronJob);
    }

    @PreDestroy
    public void stop() {
        cronTasks.keySet().forEach(this::cancel);
    }

    private boolean isSchedulableCronJob(ScheduledJob scheduledJob) {
        return scheduledJob.triggerType() == TriggerType.CRON && scheduledJob.status() == JobStatus.ACTIVE;
    }

    private void scheduleCronJob(ScheduledJob scheduledJob) {
        ZoneId zoneId = scheduledJob.timezoneId() == null ? ZoneId.of("UTC") : ZoneId.of(scheduledJob.timezoneId());
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> triggerCron(scheduledJob),
                new CronTrigger(scheduledJob.cronExpr(), zoneId)
        );
        if (future != null) {
            cronTasks.put(scheduledJob.id(), future);
        }
    }

    private void triggerCron(ScheduledJob scheduledJob) {
        try {
            if (scheduledJob.tenantId() == null) {
                executionService.triggerJob(
                        scheduledJob.id(),
                        TriggerSource.CRON,
                        SchedulerOperationContext.system("cron:" + scheduledJob.jobCode())
                );
            } else {
                TenantRequestContext tenantContext = TenantRequestContext.builder()
                        .tenantId(scheduledJob.tenantId())
                        .requestId("scheduler-cron:" + scheduledJob.jobCode())
                        .timezone(scheduledJob.timezoneId())
                        .build();
                try (TenantContextHolder.Scope ignored = TenantContextHolder.bind(tenantContext)) {
                    executionService.triggerJob(
                            scheduledJob.id(),
                            TriggerSource.CRON,
                            SchedulerOperationContext.system("cron:" + scheduledJob.jobCode())
                    );
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Cron scheduler failed to trigger job {}", scheduledJob.jobCode(), ex);
        }
    }

    private void cancel(UUID jobId) {
        ScheduledFuture<?> existing = cronTasks.remove(jobId);
        if (existing != null) {
            existing.cancel(false);
        }
    }
}
