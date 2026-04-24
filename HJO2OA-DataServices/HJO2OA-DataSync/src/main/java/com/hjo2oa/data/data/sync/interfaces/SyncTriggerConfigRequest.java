package com.hjo2oa.data.data.sync.interfaces;

import com.hjo2oa.data.data.sync.domain.SyncTriggerConfig;
import java.util.List;

public record SyncTriggerConfigRequest(
        boolean manualTriggerEnabled,
        List<String> eventPatterns,
        String schedulerJobCode
) {

    public SyncTriggerConfig toConfig() {
        return new SyncTriggerConfig(manualTriggerEnabled, eventPatterns, schedulerJobCode);
    }
}
