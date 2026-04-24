package com.hjo2oa.data.connector.domain;

public record TimeoutRetryConfig(
        int connectTimeoutMs,
        int readTimeoutMs,
        int retryCount,
        long retryIntervalMs
) {

    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 3_000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 5_000;

    public TimeoutRetryConfig {
        if (connectTimeoutMs <= 0) {
            throw new IllegalArgumentException("connectTimeoutMs must be greater than 0");
        }
        if (readTimeoutMs <= 0) {
            throw new IllegalArgumentException("readTimeoutMs must be greater than 0");
        }
        if (retryCount < 0) {
            throw new IllegalArgumentException("retryCount must not be negative");
        }
        if (retryIntervalMs < 0) {
            throw new IllegalArgumentException("retryIntervalMs must not be negative");
        }
    }

    public static TimeoutRetryConfig defaultConfig() {
        return new TimeoutRetryConfig(DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS, 0, 0L);
    }
}
