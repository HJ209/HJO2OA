package com.hjo2oa.data.data.sync.interfaces;

import com.hjo2oa.data.data.sync.domain.SyncScheduleConfig;

public record SyncScheduleConfigRequest(
        boolean enabled,
        String cron,
        String zoneId,
        String schedulerJobCode
) {

    public SyncScheduleConfig toConfig() {
        return new SyncScheduleConfig(enabled, cron, zoneId, schedulerJobCode);
    }
}
