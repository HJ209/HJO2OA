package com.hjo2oa.data.data.sync.interfaces;

import com.hjo2oa.data.data.sync.domain.SyncRetryPolicy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

public record SyncRetryPolicyRequest(
        @Min(0) @Max(5) int maxRetries,
        boolean manualRetryEnabled,
        boolean automaticRetryEnabled,
        List<String> retryableErrorCodes
) {

    public SyncRetryPolicy toPolicy() {
        return new SyncRetryPolicy(maxRetries, manualRetryEnabled, automaticRetryEnabled, retryableErrorCodes);
    }
}
