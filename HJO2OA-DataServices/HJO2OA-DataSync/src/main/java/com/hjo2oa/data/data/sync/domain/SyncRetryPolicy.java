package com.hjo2oa.data.data.sync.domain;

import java.util.List;

public record SyncRetryPolicy(
        int maxRetries,
        boolean manualRetryEnabled,
        boolean automaticRetryEnabled,
        List<String> retryableErrorCodes
) {

    public SyncRetryPolicy {
        if (maxRetries < 0 || maxRetries > 5) {
            throw new IllegalArgumentException("maxRetries must be between 0 and 5");
        }
        retryableErrorCodes = retryableErrorCodes == null ? List.of() : List.copyOf(retryableErrorCodes);
    }

    public static SyncRetryPolicy manualOnly() {
        return new SyncRetryPolicy(3, true, false, List.of());
    }

    public boolean canRetry(int currentRetryCount, String errorCode) {
        if (!manualRetryEnabled && !automaticRetryEnabled) {
            return false;
        }
        if (currentRetryCount >= maxRetries) {
            return false;
        }
        if (retryableErrorCodes.isEmpty()) {
            return true;
        }
        return errorCode != null && retryableErrorCodes.contains(errorCode.trim());
    }
}
