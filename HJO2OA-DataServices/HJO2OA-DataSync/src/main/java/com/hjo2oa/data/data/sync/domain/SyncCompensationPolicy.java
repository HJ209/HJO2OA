package com.hjo2oa.data.data.sync.domain;

public record SyncCompensationPolicy(
        boolean manualCompensationEnabled,
        boolean allowIgnoreDifference,
        boolean requireReason,
        int maxCompensationAttempts
) {

    public SyncCompensationPolicy {
        if (maxCompensationAttempts < 0) {
            throw new IllegalArgumentException("maxCompensationAttempts must not be negative");
        }
    }

    public static SyncCompensationPolicy manualDefault() {
        return new SyncCompensationPolicy(true, true, true, 3);
    }
}
