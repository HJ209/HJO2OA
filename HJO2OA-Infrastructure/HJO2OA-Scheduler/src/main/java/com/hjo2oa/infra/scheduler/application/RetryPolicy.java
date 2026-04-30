package com.hjo2oa.infra.scheduler.application;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record RetryPolicy(int maxRetries, Duration backoff) {

    private static final Pattern MAX_RETRIES_PATTERN = Pattern.compile("\"?maxRetries\"?\\s*[:=]\\s*(\\d+)");
    private static final Pattern MAX_FAILURES_PATTERN = Pattern.compile("\"?maxFailures\"?\\s*[:=]\\s*(\\d+)");
    private static final Pattern BACKOFF_SECONDS_PATTERN = Pattern.compile("\"?backoffSeconds\"?\\s*[:=]\\s*(\\d+)");
    private static final Pattern RETRY_INTERVAL_PATTERN = Pattern.compile("\"?retryIntervalSeconds\"?\\s*[:=]\\s*(\\d+)");

    RetryPolicy {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must not be negative");
        }
        if (backoff == null || backoff.isNegative()) {
            backoff = Duration.ofSeconds(60);
        }
    }

    int maxAttempts() {
        return maxRetries + 1;
    }

    boolean canRetry(int attemptNo) {
        return attemptNo < maxAttempts();
    }

    static RetryPolicy parse(String retryPolicy) {
        if (retryPolicy == null || retryPolicy.isBlank()) {
            return new RetryPolicy(0, Duration.ofSeconds(60));
        }
        int maxRetries = extractInt(retryPolicy, MAX_RETRIES_PATTERN, 0);
        int maxFailures = extractInt(retryPolicy, MAX_FAILURES_PATTERN, -1);
        if (maxFailures > 0) {
            maxRetries = Math.max(maxRetries, maxFailures - 1);
        }
        long backoffSeconds = extractInt(retryPolicy, BACKOFF_SECONDS_PATTERN, -1);
        if (backoffSeconds < 0) {
            backoffSeconds = extractInt(retryPolicy, RETRY_INTERVAL_PATTERN, 60);
        }
        return new RetryPolicy(maxRetries, Duration.ofSeconds(backoffSeconds));
    }

    private static int extractInt(String value, Pattern pattern, int defaultValue) {
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) {
            return defaultValue;
        }
        return Integer.parseInt(matcher.group(1));
    }
}
