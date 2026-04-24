package com.hjo2oa.data.data.sync.domain;

import org.springframework.scheduling.support.CronExpression;

public record SyncScheduleConfig(
        boolean enabled,
        String cron,
        String zoneId,
        String schedulerJobCode
) {

    public SyncScheduleConfig {
        cron = normalize(cron);
        zoneId = normalize(zoneId);
        schedulerJobCode = normalize(schedulerJobCode);
        if (enabled) {
            if (cron == null && schedulerJobCode == null) {
                throw new IllegalArgumentException("enabled schedule must define cron or schedulerJobCode");
            }
            if (cron != null) {
                CronExpression.parse(cron);
            }
        }
    }

    public static SyncScheduleConfig disabled() {
        return new SyncScheduleConfig(false, null, null, null);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
