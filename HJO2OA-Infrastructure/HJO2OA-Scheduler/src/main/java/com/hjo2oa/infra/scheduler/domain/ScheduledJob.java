package com.hjo2oa.infra.scheduler.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import org.springframework.scheduling.support.CronExpression;

public record ScheduledJob(
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

    public ScheduledJob {
        Objects.requireNonNull(id, "id must not be null");
        jobCode = requireText(jobCode, "jobCode");
        name = requireText(name, "name");
        Objects.requireNonNull(triggerType, "triggerType must not be null");
        cronExpr = normalizeNullableText(cronExpr);
        timezoneId = normalizeNullableText(timezoneId);
        Objects.requireNonNull(concurrencyPolicy, "concurrencyPolicy must not be null");
        if (timeoutSeconds != null && timeoutSeconds <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be greater than zero");
        }
        retryPolicy = normalizeNullableText(retryPolicy);
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");

        if (triggerType == TriggerType.CRON) {
            if (cronExpr == null) {
                throw new IllegalArgumentException("cronExpr must not be blank for CRON jobs");
            }
            if (!CronExpression.isValidExpression(cronExpr)) {
                throw new IllegalArgumentException("cronExpr is not a valid cron expression");
            }
        }
        if (timezoneId != null) {
            ZoneId.of(timezoneId);
        }
    }

    public static ScheduledJob create(
            String jobCode,
            String name,
            TriggerType triggerType,
            String cronExpr,
            String timezoneId,
            ConcurrencyPolicy concurrencyPolicy,
            Integer timeoutSeconds,
            String retryPolicy,
            UUID tenantId,
            Instant now
    ) {
        return new ScheduledJob(
                UUID.randomUUID(),
                jobCode,
                name,
                triggerType,
                cronExpr,
                timezoneId,
                concurrencyPolicy,
                timeoutSeconds,
                retryPolicy,
                JobStatus.ACTIVE,
                tenantId,
                now,
                now
        );
    }

    public ScheduledJob pause(Instant now) {
        if (status == JobStatus.DISABLED) {
            throw new IllegalStateException("Disabled job cannot be paused");
        }
        if (status == JobStatus.PAUSED) {
            return this;
        }
        return new ScheduledJob(
                id,
                jobCode,
                name,
                triggerType,
                cronExpr,
                timezoneId,
                concurrencyPolicy,
                timeoutSeconds,
                retryPolicy,
                JobStatus.PAUSED,
                tenantId,
                createdAt,
                now
        );
    }

    public ScheduledJob resume(Instant now) {
        if (status == JobStatus.DISABLED) {
            throw new IllegalStateException("Disabled job cannot be resumed");
        }
        if (status == JobStatus.ACTIVE) {
            return this;
        }
        if (status != JobStatus.PAUSED) {
            throw new IllegalStateException("Only paused job can be resumed");
        }
        return new ScheduledJob(
                id,
                jobCode,
                name,
                triggerType,
                cronExpr,
                timezoneId,
                concurrencyPolicy,
                timeoutSeconds,
                retryPolicy,
                JobStatus.ACTIVE,
                tenantId,
                createdAt,
                now
        );
    }

    public ScheduledJob disable(Instant now) {
        if (status == JobStatus.DISABLED) {
            return this;
        }
        return new ScheduledJob(
                id,
                jobCode,
                name,
                triggerType,
                cronExpr,
                timezoneId,
                concurrencyPolicy,
                timeoutSeconds,
                retryPolicy,
                JobStatus.DISABLED,
                tenantId,
                createdAt,
                now
        );
    }

    public ScheduledJobView toView() {
        return new ScheduledJobView(
                id,
                jobCode,
                name,
                triggerType,
                cronExpr,
                timezoneId,
                concurrencyPolicy,
                timeoutSeconds,
                retryPolicy,
                status,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
