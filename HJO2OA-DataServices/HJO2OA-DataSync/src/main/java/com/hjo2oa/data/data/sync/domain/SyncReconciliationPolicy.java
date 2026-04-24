package com.hjo2oa.data.data.sync.domain;

public record SyncReconciliationPolicy(
        boolean enabled,
        boolean checkExtraTargetRecords,
        boolean failWhenDifferenceDetected,
        int manualReviewThreshold
) {

    public SyncReconciliationPolicy {
        if (manualReviewThreshold < 0) {
            throw new IllegalArgumentException("manualReviewThreshold must not be negative");
        }
    }

    public static SyncReconciliationPolicy enabledByDefault() {
        return new SyncReconciliationPolicy(true, true, true, 0);
    }
}
