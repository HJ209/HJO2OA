package com.hjo2oa.infra.scheduler.application;

import com.hjo2oa.infra.scheduler.domain.ConcurrencyPolicy;
import com.hjo2oa.infra.scheduler.domain.TriggerSource;
import com.hjo2oa.infra.scheduler.domain.TriggerType;
import java.util.Objects;
import java.util.UUID;

public final class ScheduledJobCommands {

    private ScheduledJobCommands() {
    }

    public record RegisterJobCommand(
            String jobCode,
            String name,
            TriggerType triggerType,
            String cronExpr,
            String timezoneId,
            ConcurrencyPolicy concurrencyPolicy,
            Integer timeoutSeconds,
            String retryPolicy,
            UUID tenantId
    ) {

        public RegisterJobCommand {
            jobCode = requireText(jobCode, "jobCode");
            name = requireText(name, "name");
            Objects.requireNonNull(triggerType, "triggerType must not be null");
            cronExpr = normalizeNullableText(cronExpr);
            timezoneId = normalizeNullableText(timezoneId);
            Objects.requireNonNull(concurrencyPolicy, "concurrencyPolicy must not be null");
            retryPolicy = normalizeNullableText(retryPolicy);
        }
    }

    public record RecordStartCommand(UUID jobId, TriggerSource triggerSource) {

        public RecordStartCommand {
            Objects.requireNonNull(jobId, "jobId must not be null");
            Objects.requireNonNull(triggerSource, "triggerSource must not be null");
        }
    }

    public record RecordSuccessCommand(UUID executionId, String executionLog) {

        public RecordSuccessCommand {
            Objects.requireNonNull(executionId, "executionId must not be null");
            executionLog = normalizeNullableText(executionLog);
        }
    }

    public record RecordFailureCommand(UUID executionId, String errorCode, String errorMessage) {

        public RecordFailureCommand {
            Objects.requireNonNull(executionId, "executionId must not be null");
            errorCode = requireText(errorCode, "errorCode");
            errorMessage = requireText(errorMessage, "errorMessage");
        }
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
