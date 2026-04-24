package com.hjo2oa.data.connector.interfaces;

import com.hjo2oa.data.connector.domain.TimeoutRetryConfig;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record TimeoutRetryConfigRequest(
        @Positive Integer connectTimeoutMs,
        @Positive Integer readTimeoutMs,
        @PositiveOrZero Integer retryCount,
        @PositiveOrZero Long retryIntervalMs
) {

    public TimeoutRetryConfig toValue() {
        return new TimeoutRetryConfig(
                connectTimeoutMs == null ? 3_000 : connectTimeoutMs,
                readTimeoutMs == null ? 5_000 : readTimeoutMs,
                retryCount == null ? 0 : retryCount,
                retryIntervalMs == null ? 0L : retryIntervalMs
        );
    }
}
