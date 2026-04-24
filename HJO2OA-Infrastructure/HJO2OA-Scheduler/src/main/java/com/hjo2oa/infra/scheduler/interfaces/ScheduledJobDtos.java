package com.hjo2oa.infra.scheduler.interfaces;

import com.hjo2oa.infra.scheduler.domain.ConcurrencyPolicy;
import com.hjo2oa.infra.scheduler.domain.ExecutionStatus;
import com.hjo2oa.infra.scheduler.domain.JobStatus;
import com.hjo2oa.infra.scheduler.domain.TriggerSource;
import com.hjo2oa.infra.scheduler.domain.TriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;

public final class ScheduledJobDtos {

    private ScheduledJobDtos() {
    }

    public record RegisterJobRequest(
            @NotBlank String jobCode,
            @NotBlank String name,
            @NotNull TriggerType triggerType,
            String cronExpr,
            String timezoneId,
            @NotNull ConcurrencyPolicy concurrencyPolicy,
            @Positive Integer timeoutSeconds,
            String retryPolicy,
            UUID tenantId
    ) {
    }

    public record ScheduledJobResponse(
            UUID id,
            String jobCode,
            String name,
            TriggerType triggerType,
            String cronExpr,
            String timezoneId,
            ConcurrencyPolicy concurrencyPolicy,
            Integer timeoutSeconds,
            String retryPolicy,
            JobStatus status,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record JobExecutionRecordResponse(
            UUID id,
            UUID scheduledJobId,
            TriggerSource triggerSource,
            ExecutionStatus executionStatus,
            Instant startedAt,
            Instant finishedAt,
            String errorCode,
            String errorMessage,
            String executionLog
    ) {
    }
}
