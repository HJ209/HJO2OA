package com.hjo2oa.infra.scheduler.application;

import com.hjo2oa.infra.scheduler.domain.ConcurrencyPolicy;
import com.hjo2oa.infra.scheduler.domain.JobExecutionRecord;
import com.hjo2oa.infra.scheduler.domain.JobExecutionRecordRepository;
import com.hjo2oa.infra.scheduler.domain.JobExecutionRecordView;
import com.hjo2oa.infra.scheduler.domain.JobStatus;
import com.hjo2oa.infra.scheduler.domain.ScheduledJob;
import com.hjo2oa.infra.scheduler.domain.ScheduledJobRepository;
import com.hjo2oa.infra.scheduler.domain.ScheduledJobView;
import com.hjo2oa.infra.scheduler.domain.SchedulerErrorDescriptors;
import com.hjo2oa.infra.scheduler.domain.SchedulerTaskFailedEvent;
import com.hjo2oa.infra.scheduler.domain.TriggerSource;
import com.hjo2oa.infra.scheduler.domain.TriggerType;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduledJobApplicationService {

    private static final String REPLACED_ERROR_CODE = "REPLACED";
    private static final String REPLACED_ERROR_MESSAGE = "Running execution was replaced by a new execution";
    private static final String REPLACED_EXECUTION_LOG = "{\"reason\":\"REPLACED\"}";

    private final ScheduledJobRepository scheduledJobRepository;
    private final JobExecutionRecordRepository jobExecutionRecordRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;
    @Autowired
    public ScheduledJobApplicationService(
            ScheduledJobRepository scheduledJobRepository,
            JobExecutionRecordRepository jobExecutionRecordRepository,
            DomainEventPublisher domainEventPublisher
    ) {
        this(
                scheduledJobRepository,
                jobExecutionRecordRepository,
                domainEventPublisher,
                Clock.systemUTC()
        );
    }
    public ScheduledJobApplicationService(
            ScheduledJobRepository scheduledJobRepository,
            JobExecutionRecordRepository jobExecutionRecordRepository,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.scheduledJobRepository = Objects.requireNonNull(
                scheduledJobRepository,
                "scheduledJobRepository must not be null"
        );
        this.jobExecutionRecordRepository = Objects.requireNonNull(
                jobExecutionRecordRepository,
                "jobExecutionRecordRepository must not be null"
        );
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public ScheduledJobView registerJob(
            String code,
            String name,
            TriggerType triggerType,
            String cronExpr,
            String timezoneId,
            ConcurrencyPolicy concurrencyPolicy,
            Integer timeoutSeconds,
            String retryPolicy,
            UUID tenantId
    ) {
        scheduledJobRepository.findByJobCode(code).ifPresent(existing -> {
            throw new BizException(
                    SchedulerErrorDescriptors.JOB_CODE_CONFLICT,
                    "Scheduled job code already exists: " + code
            );
        });
        Instant now = now();
        try {
            ScheduledJob scheduledJob = ScheduledJob.create(
                    code,
                    name,
                    triggerType,
                    cronExpr,
                    timezoneId,
                    concurrencyPolicy,
                    timeoutSeconds,
                    retryPolicy,
                    tenantId,
                    now
            );
            return scheduledJobRepository.save(scheduledJob).toView();
        } catch (IllegalArgumentException | DateTimeException ex) {
            throw new BizException(SchedulerErrorDescriptors.JOB_DEFINITION_INVALID, ex.getMessage(), ex);
        }
    }

    @Transactional
    public ScheduledJobView pauseJob(UUID jobId) {
        return transitionJob(jobId, scheduledJob -> scheduledJob.pause(now()));
    }

    @Transactional
    public ScheduledJobView resumeJob(UUID jobId) {
        return transitionJob(jobId, scheduledJob -> scheduledJob.resume(now()));
    }

    @Transactional
    public ScheduledJobView disableJob(UUID jobId) {
        return transitionJob(jobId, scheduledJob -> scheduledJob.disable(now()));
    }

    @Transactional
    public JobExecutionRecordView triggerJob(String jobCode) {
        ScheduledJob scheduledJob = loadJobByCode(jobCode);
        return recordStart(scheduledJob.id(), TriggerSource.MANUAL);
    }

    @Transactional
    public JobExecutionRecordView recordStart(UUID jobId, TriggerSource triggerSource) {
        ScheduledJob scheduledJob = loadJob(jobId);
        validateStart(scheduledJob, triggerSource);
        Instant now = now();

        List<JobExecutionRecord> runningExecutions = jobExecutionRecordRepository.findRunningByScheduledJobId(jobId);
        if (!runningExecutions.isEmpty()) {
            if (scheduledJob.concurrencyPolicy() == ConcurrencyPolicy.FORBID) {
                throw new BizException(
                        SchedulerErrorDescriptors.EXECUTION_CONFLICT,
                        "Scheduled job is already running: " + scheduledJob.jobCode()
                );
            }
            if (scheduledJob.concurrencyPolicy() == ConcurrencyPolicy.REPLACE) {
                for (JobExecutionRecord runningExecution : runningExecutions) {
                    jobExecutionRecordRepository.save(runningExecution.markCancelled(
                            REPLACED_ERROR_CODE,
                            REPLACED_ERROR_MESSAGE,
                            REPLACED_EXECUTION_LOG,
                            now
                    ));
                }
            }
        }

        JobExecutionRecord executionRecord = JobExecutionRecord.start(jobId, triggerSource, now);
        return jobExecutionRecordRepository.save(executionRecord).toView();
    }

    @Transactional
    public JobExecutionRecordView recordSuccess(UUID executionId, String executionLog) {
        JobExecutionRecord executionRecord = loadExecution(executionId);
        try {
            return jobExecutionRecordRepository.save(executionRecord.markSuccess(executionLog, now())).toView();
        } catch (IllegalStateException ex) {
            throw new BizException(SchedulerErrorDescriptors.EXECUTION_STATE_INVALID, ex.getMessage(), ex);
        }
    }

    @Transactional
    public JobExecutionRecordView recordFailure(UUID executionId, String errorCode, String errorMessage) {
        JobExecutionRecord executionRecord = loadExecution(executionId);
        Instant now = now();
        JobExecutionRecord failedRecord;
        try {
            failedRecord = executionRecord.markFailure(errorCode, errorMessage, now);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BizException(SchedulerErrorDescriptors.EXECUTION_STATE_INVALID, ex.getMessage(), ex);
        }
        JobExecutionRecord persisted = jobExecutionRecordRepository.save(failedRecord);
        ScheduledJob scheduledJob = loadJob(persisted.scheduledJobId());
        domainEventPublisher.publish(SchedulerTaskFailedEvent.from(scheduledJob, persisted, now));
        return persisted.toView();
    }

    public List<ScheduledJobView> queryJobs(UUID tenantId) {
        List<ScheduledJob> scheduledJobs = tenantId == null
                ? scheduledJobRepository.findAll()
                : scheduledJobRepository.findByTenantId(tenantId);
        return scheduledJobs.stream()
                .sorted(Comparator.comparing(ScheduledJob::jobCode))
                .map(ScheduledJob::toView)
                .toList();
    }

    public List<JobExecutionRecordView> queryExecutions(UUID jobId, Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "from must not be after to");
        }
        return jobExecutionRecordRepository.findByCriteria(jobId, from, to).stream()
                .sorted(Comparator.comparing(JobExecutionRecord::startedAt).reversed())
                .map(JobExecutionRecord::toView)
                .toList();
    }

    private ScheduledJobView transitionJob(UUID jobId, JobTransition transition) {
        ScheduledJob scheduledJob = loadJob(jobId);
        try {
            return scheduledJobRepository.save(transition.apply(scheduledJob)).toView();
        } catch (IllegalStateException ex) {
            throw new BizException(SchedulerErrorDescriptors.JOB_STATE_INVALID, ex.getMessage(), ex);
        }
    }

    private void validateStart(ScheduledJob scheduledJob, TriggerSource triggerSource) {
        if (scheduledJob.status() == JobStatus.DISABLED) {
            throw new BizException(
                    SchedulerErrorDescriptors.JOB_STATE_INVALID,
                    "Disabled job cannot be triggered: " + scheduledJob.jobCode()
            );
        }
        if (scheduledJob.status() == JobStatus.PAUSED && triggerSource != TriggerSource.MANUAL) {
            throw new BizException(
                    SchedulerErrorDescriptors.JOB_STATE_INVALID,
                    "Paused job only supports manual trigger: " + scheduledJob.jobCode()
            );
        }
        if (triggerSource == TriggerSource.CRON && scheduledJob.triggerType() != TriggerType.CRON) {
            throw new BizException(
                    SchedulerErrorDescriptors.TRIGGER_SOURCE_INVALID,
                    "Cron trigger is not supported by job: " + scheduledJob.jobCode()
            );
        }
        if (triggerSource == TriggerSource.DEPENDENCY && scheduledJob.triggerType() != TriggerType.DEPENDENCY) {
            throw new BizException(
                    SchedulerErrorDescriptors.TRIGGER_SOURCE_INVALID,
                    "Dependency trigger is not supported by job: " + scheduledJob.jobCode()
            );
        }
    }

    private ScheduledJob loadJob(UUID jobId) {
        return scheduledJobRepository.findById(jobId)
                .orElseThrow(() -> new BizException(
                        SchedulerErrorDescriptors.SCHEDULED_JOB_NOT_FOUND,
                        "Scheduled job not found: " + jobId
                ));
    }

    private ScheduledJob loadJobByCode(String jobCode) {
        return scheduledJobRepository.findByJobCode(jobCode)
                .orElseThrow(() -> new BizException(
                        SchedulerErrorDescriptors.SCHEDULED_JOB_NOT_FOUND,
                        "Scheduled job not found: " + jobCode
                ));
    }

    private JobExecutionRecord loadExecution(UUID executionId) {
        return jobExecutionRecordRepository.findById(executionId)
                .orElseThrow(() -> new BizException(
                        SchedulerErrorDescriptors.JOB_EXECUTION_NOT_FOUND,
                        "Job execution not found: " + executionId
                ));
    }

    private Instant now() {
        return clock.instant();
    }

    @FunctionalInterface
    private interface JobTransition {

        ScheduledJob apply(ScheduledJob scheduledJob);
    }
}
