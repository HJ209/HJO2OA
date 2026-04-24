package com.hjo2oa.data.data.sync.interfaces;

import com.hjo2oa.data.data.sync.domain.SyncReconciliationPolicy;
import jakarta.validation.constraints.Min;

public record SyncReconciliationPolicyRequest(
        boolean enabled,
        boolean checkExtraTargetRecords,
        boolean failWhenDifferenceDetected,
        @Min(0) int manualReviewThreshold
) {

    public SyncReconciliationPolicy toPolicy() {
        return new SyncReconciliationPolicy(
                enabled,
                checkExtraTargetRecords,
                failWhenDifferenceDetected,
                manualReviewThreshold
        );
    }
}
