package com.hjo2oa.data.data.sync.interfaces;

import com.hjo2oa.data.data.sync.domain.SyncCompensationPolicy;
import jakarta.validation.constraints.Min;

public record SyncCompensationPolicyRequest(
        boolean manualCompensationEnabled,
        boolean allowIgnoreDifference,
        boolean requireReason,
        @Min(0) int maxCompensationAttempts
) {

    public SyncCompensationPolicy toPolicy() {
        return new SyncCompensationPolicy(
                manualCompensationEnabled,
                allowIgnoreDifference,
                requireReason,
                maxCompensationAttempts
        );
    }
}
