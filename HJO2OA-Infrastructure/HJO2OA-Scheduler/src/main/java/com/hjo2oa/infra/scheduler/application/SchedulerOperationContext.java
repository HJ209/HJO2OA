package com.hjo2oa.infra.scheduler.application;

import java.util.UUID;

public record SchedulerOperationContext(
        UUID tenantId,
        UUID operatorAccountId,
        UUID operatorPersonId,
        String requestId,
        String idempotencyKey,
        String language,
        String timezone,
        String triggerContext
) {

    public SchedulerOperationContext {
        requestId = normalize(requestId);
        idempotencyKey = normalize(idempotencyKey);
        language = normalize(language);
        timezone = normalize(timezone);
        triggerContext = normalize(triggerContext);
    }

    public static SchedulerOperationContext system(String triggerContext) {
        return new SchedulerOperationContext(null, null, null, null, null, null, "UTC", triggerContext);
    }

    public SchedulerOperationContext withTenantFallback(UUID fallbackTenantId) {
        if (tenantId != null || fallbackTenantId == null) {
            return this;
        }
        return new SchedulerOperationContext(
                fallbackTenantId,
                operatorAccountId,
                operatorPersonId,
                requestId,
                idempotencyKey,
                language,
                timezone,
                triggerContext
        );
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
