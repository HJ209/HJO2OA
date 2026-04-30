package com.hjo2oa.infra.scheduler.application;

import com.hjo2oa.infra.scheduler.domain.ConcurrencyPolicy;
import com.hjo2oa.infra.scheduler.domain.ExecutionStatus;
import com.hjo2oa.infra.scheduler.domain.JobExecutionRecord;
import com.hjo2oa.infra.scheduler.domain.JobExecutionRecordRepository;
import com.hjo2oa.infra.scheduler.domain.JobExecutionRecordView;
import com.hjo2oa.infra.scheduler.domain.JobStatus;
import com.hjo2oa.infra.scheduler.domain.ScheduledJob;
import com.hjo2oa.infra.scheduler.domain.ScheduledJobRepository;
import com.hjo2oa.infra.scheduler.domain.SchedulerErrorDescriptors;
import com.hjo2oa.infra.scheduler.domain.SchedulerTaskFailedEvent;
import com.hjo2oa.infra.scheduler.domain.SchedulerTaskRetryingEvent;
import com.hjo2oa.infra.scheduler.domain.SchedulerTaskSucceededEvent;
import com.hjo2oa.infra.scheduler.domain.TriggerSource;
import com.hjo2oa.infra.scheduler.domain.TriggerType;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Service
public class SchedulerExecutionService {

    private static final String REPLACED_ERROR_CODE = "REPLACED";
    private static final String REPLACED_ERROR_MESSAGE = "Running execution was replaced by a new execution";
    private static final String REPLACED_EXECUTION_LOG = "{\"reason\":\"REPLACED\"}";
    private static final String HANDLER_NOT_FOUND_CODE = "JOB_HANDLER_NOT_FOUND";
    private static final String TIMEOUT_ERROR_CODE = "JOB_TIMEOUT";

    private final ScheduledJobRepository scheduledJobRepository;
    private final JobExecutionRecordRepository jobExecutionRecordRepository;
    private final SchedulerJobHandlerRegistry handlerRegistry;
    private final SchedulerJobLock jobLock;
    private final DomainEventPublisher domainEventPublisher;
    private final TaskScheduler taskScheduler;
    private final ExecutorService handlerExecutor;
    private final Clock clock;

    @Autowired
    public SchedulerExecutionService(
            ScheduledJobRepository scheduledJobRepository,
            JobExecutionRecordRepository jobExecutionRecordRepository,
            SchedulerJobHandlerRegistry handlerRegistry,
            SchedulerJobLock jobLock,
            DomainEventPublisher domainEventPublisher,
            TaskScheduler taskScheduler,
            @Qualifier("infraSchedulerHandlerExecutor") ExecutorService handlerExecutor
    ) {
        this(
                scheduledJobRepository,
                jobExecutionRecordRepository,
                handlerRegistry,
                jobLock,
                domainEventPublisher,
                taskScheduler,
                handlerExecutor,
                Clock.systemUTC()
        );
    }

    public SchedulerExecutionService(
            ScheduledJobRepository scheduledJobRepository,
            JobExecutionRecordRepository jobExecutionRecordRepository,
            SchedulerJobHandlerRegistry handlerRegistry,
            SchedulerJobLock jobLock,
            DomainEventPublisher domainEventPublisher,
            TaskScheduler taskScheduler,
            ExecutorService handlerExecutor,
            Clock clock
    ) {
        this.scheduledJobRepository = Objects.requireNonNull(scheduledJobRepository, "scheduledJobRepository");
        this.jobExecutionRecordRepository = Objects.requireNonNull(
                jobExecutionRecordRepository,
                "jobExecutionRecordRepository"
        );
        this.handlerRegistry = Objects.requireNonNull(handlerRegistry, "handlerRegistry");
        this.jobLock = Objects.requireNonNull(jobLock, "jobLock");
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher");
        this.taskScheduler = Objects.requireNonNull(taskScheduler, "taskScheduler");
        this.handlerExecutor = Objects.requireNonNull(handlerExecutor, "handlerExecutor");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public JobExecutionRecordView triggerJob(
            UUID jobId,
            TriggerSource triggerSource,
            SchedulerOperationContext context
    ) {
        ScheduledJob scheduledJob = loadJob(jobId);
        RetryPolicy retryPolicy = RetryPolicy.parse(scheduledJob.retryPolicy());
        return executeAttempt(scheduledJob, triggerSource, context, null, 1, retryPolicy.maxAttempts(), retryPolicy);
    }

    public JobExecutionRecordView retryExecution(UUID executionId, SchedulerOperationContext context) {
        JobExecutionRecord executionRecord = loadExecution(executionId);
        ScheduledJob scheduledJob = loadJob(executionRecord.scheduledJobId());
        int nextAttemptNo = executionRecord.attemptNo() + 1;
        if (nextAttemptNo > executionRecord.maxAttempts()) {
            throw new BizException(
                    SchedulerErrorDescriptors.EXECUTION_STATE_INVALID,
                    "Execution has no remaining retry attempts: " + executionId
            );
        }
        UUID parentExecutionId = executionRecord.parentExecutionId() == null
                ? executionRecord.id()
                : executionRecord.parentExecutionId();
        RetryPolicy retryPolicy = RetryPolicy.parse(scheduledJob.retryPolicy());
        return executeAttempt(
                scheduledJob,
                TriggerSource.RETRY,
                context,
                parentExecutionId,
                nextAttemptNo,
                executionRecord.maxAttempts(),
                retryPolicy
        );
    }

    private JobExecutionRecordView executeAttempt(
            ScheduledJob scheduledJob,
            TriggerSource triggerSource,
            SchedulerOperationContext context,
            UUID parentExecutionId,
            int attemptNo,
            int maxAttempts,
            RetryPolicy retryPolicy
    ) {
        validateStart(scheduledJob, triggerSource);
        SchedulerOperationContext effectiveContext = context == null
                ? SchedulerOperationContext.system(triggerSource.name())
                : context.withTenantFallback(scheduledJob.tenantId());
        if (attemptNo == 1 && effectiveContext.idempotencyKey() != null) {
            return jobExecutionRecordRepository
                    .findByJobIdAndIdempotencyKey(scheduledJob.id(), effectiveContext.idempotencyKey())
                    .map(JobExecutionRecord::toView)
                    .orElseGet(() -> runLockedAttempt(
                            scheduledJob,
                            triggerSource,
                            effectiveContext,
                            parentExecutionId,
                            attemptNo,
                            maxAttempts,
                            retryPolicy
                    ));
        }
        return runLockedAttempt(
                scheduledJob,
                triggerSource,
                effectiveContext,
                parentExecutionId,
                attemptNo,
                maxAttempts,
                retryPolicy
        );
    }

    private JobExecutionRecordView runLockedAttempt(
            ScheduledJob scheduledJob,
            TriggerSource triggerSource,
            SchedulerOperationContext context,
            UUID parentExecutionId,
            int attemptNo,
            int maxAttempts,
            RetryPolicy retryPolicy
    ) {
        if (scheduledJob.concurrencyPolicy() == ConcurrencyPolicy.FORBID) {
            Duration ttl = scheduledJob.timeoutSeconds() == null
                    ? Duration.ofMinutes(30)
                    : Duration.ofSeconds(scheduledJob.timeoutSeconds()).plusMinutes(1);
            return jobLock.tryAcquire(scheduledJob.id(), ttl)
                    .map(lease -> {
                        try (lease) {
                            return startAndRun(
                                    scheduledJob,
                                    triggerSource,
                                    context,
                                    parentExecutionId,
                                    attemptNo,
                                    maxAttempts,
                                    retryPolicy
                            );
                        }
                    })
                    .orElseThrow(() -> new BizException(
                            SchedulerErrorDescriptors.EXECUTION_CONFLICT,
                            "Scheduled job is already running: " + scheduledJob.jobCode()
                    ));
        }
        return startAndRun(scheduledJob, triggerSource, context, parentExecutionId, attemptNo, maxAttempts, retryPolicy);
    }

    private JobExecutionRecordView startAndRun(
            ScheduledJob scheduledJob,
            TriggerSource triggerSource,
            SchedulerOperationContext context,
            UUID parentExecutionId,
            int attemptNo,
            int maxAttempts,
            RetryPolicy retryPolicy
    ) {
        Instant startedAt = now();
        enforceConcurrency(scheduledJob, startedAt);
        JobExecutionRecord executionRecord = JobExecutionRecord.start(
                scheduledJob.id(),
                triggerSource,
                startedAt,
                parentExecutionId,
                attemptNo,
                maxAttempts,
                triggerContextJson(triggerSource, context),
                context.idempotencyKey()
        );
        JobExecutionRecord persisted = jobExecutionRecordRepository.save(executionRecord);

        SchedulerJobHandler handler = handlerRegistry.find(scheduledJob.handlerName()).orElse(null);
        if (handler == null) {
            return handleFailure(
                    scheduledJob,
                    persisted,
                    HANDLER_NOT_FOUND_CODE,
                    "Scheduler job handler not found: " + scheduledJob.handlerName(),
                    null,
                    retryPolicy
            );
        }

        try {
            SchedulerJobExecutionContext executionContext = new SchedulerJobExecutionContext(
                    persisted.id(),
                    scheduledJob.id(),
                    scheduledJob.jobCode(),
                    scheduledJob.tenantId(),
                    triggerSource,
                    attemptNo,
                    maxAttempts,
                    context.requestId(),
                    context.idempotencyKey(),
                    context.language(),
                    context.timezone(),
                    context.triggerContext()
            );
            SchedulerJobResult result = runHandler(handler, executionContext, scheduledJob.timeoutSeconds());
            return markSuccess(scheduledJob, persisted, result);
        } catch (JobTimedOutException ex) {
            return handleFailure(
                    scheduledJob,
                    persisted,
                    TIMEOUT_ERROR_CODE,
                    ex.getMessage(),
                    stackTrace(ex),
                    retryPolicy
            );
        } catch (Exception ex) {
            return handleFailure(
                    scheduledJob,
                    persisted,
                    ex.getClass().getSimpleName(),
                    ex.getMessage() == null ? ex.toString() : ex.getMessage(),
                    stackTrace(ex),
                    retryPolicy
            );
        }
    }

    private SchedulerJobResult runHandler(
            SchedulerJobHandler handler,
            SchedulerJobExecutionContext context,
            Integer timeoutSeconds
    ) throws Exception {
        Callable<SchedulerJobResult> call = () -> {
            SchedulerJobResult result = handler.execute(context);
            return result == null ? SchedulerJobResult.success() : result;
        };
        if (timeoutSeconds == null) {
            return call.call();
        }
        Future<SchedulerJobResult> future = handlerExecutor.submit(call);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new JobTimedOutException("Scheduler job timed out after " + timeoutSeconds + " seconds", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new RuntimeException(cause);
        }
    }

    private JobExecutionRecordView markSuccess(
            ScheduledJob scheduledJob,
            JobExecutionRecord executionRecord,
            SchedulerJobResult result
    ) {
        JobExecutionRecord current = jobExecutionRecordRepository.findById(executionRecord.id()).orElse(executionRecord);
        if (current.executionStatus() != ExecutionStatus.RUNNING) {
            return current.toView();
        }
        JobExecutionRecord succeeded = current.markSuccess(result.executionLog(), now());
        JobExecutionRecord persisted = jobExecutionRecordRepository.save(succeeded);
        domainEventPublisher.publish(SchedulerTaskSucceededEvent.from(scheduledJob, persisted, now()));
        return persisted.toView();
    }

    private JobExecutionRecordView handleFailure(
            ScheduledJob scheduledJob,
            JobExecutionRecord executionRecord,
            String errorCode,
            String errorMessage,
            String errorStack,
            RetryPolicy retryPolicy
    ) {
        JobExecutionRecord current = jobExecutionRecordRepository.findById(executionRecord.id()).orElse(executionRecord);
        if (current.executionStatus() != ExecutionStatus.RUNNING) {
            return current.toView();
        }
        Instant finishedAt = now();
        if (retryPolicy.canRetry(current.attemptNo())) {
            Instant nextRetryAt = finishedAt.plus(retryPolicy.backoff());
            JobExecutionRecord retrying = current.markRetrying(
                    errorCode,
                    errorMessage,
                    errorStack,
                    "{\"nextAttemptNo\":" + (current.attemptNo() + 1) + "}",
                    nextRetryAt,
                    finishedAt
            );
            JobExecutionRecord persisted = jobExecutionRecordRepository.save(retrying);
            domainEventPublisher.publish(SchedulerTaskRetryingEvent.from(scheduledJob, persisted, finishedAt));
            scheduleRetry(scheduledJob, persisted, retryPolicy);
            return persisted.toView();
        }

        JobExecutionRecord failed = TIMEOUT_ERROR_CODE.equals(errorCode)
                ? current.markTimeout(errorCode, errorMessage, errorStack, finishedAt)
                : current.markFailure(errorCode, errorMessage, errorStack, finishedAt);
        JobExecutionRecord persisted = jobExecutionRecordRepository.save(failed);
        domainEventPublisher.publish(SchedulerTaskFailedEvent.from(scheduledJob, persisted, finishedAt));
        return persisted.toView();
    }

    private void scheduleRetry(ScheduledJob scheduledJob, JobExecutionRecord executionRecord, RetryPolicy retryPolicy) {
        UUID parentExecutionId = executionRecord.parentExecutionId() == null
                ? executionRecord.id()
                : executionRecord.parentExecutionId();
        taskScheduler.schedule(
                () -> executeAttempt(
                        scheduledJob,
                        TriggerSource.RETRY,
                        SchedulerOperationContext.system("retry:" + executionRecord.id()),
                        parentExecutionId,
                        executionRecord.attemptNo() + 1,
                        executionRecord.maxAttempts(),
                        retryPolicy
                ),
                executionRecord.nextRetryAt()
        );
    }

    private void enforceConcurrency(ScheduledJob scheduledJob, Instant now) {
        List<JobExecutionRecord> runningExecutions =
                jobExecutionRecordRepository.findRunningByScheduledJobId(scheduledJob.id());
        if (runningExecutions.isEmpty()) {
            return;
        }
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

    private String triggerContextJson(TriggerSource triggerSource, SchedulerOperationContext context) {
        return "{"
                + "\"source\":\"" + escape(triggerSource.name()) + "\","
                + "\"requestId\":" + quoted(context.requestId()) + ","
                + "\"language\":" + quoted(context.language()) + ","
                + "\"timezone\":" + quoted(context.timezone()) + ","
                + "\"operatorAccountId\":" + quoted(context.operatorAccountId()) + ","
                + "\"operatorPersonId\":" + quoted(context.operatorPersonId()) + ","
                + "\"context\":" + quoted(context.triggerContext())
                + "}";
    }

    private static String quoted(Object value) {
        if (value == null) {
            return "null";
        }
        return "\"" + escape(value.toString()) + "\"";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private static final class JobTimedOutException extends RuntimeException {

        private JobTimedOutException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
