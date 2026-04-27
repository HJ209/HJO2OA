package com.hjo2oa.process.monitor.domain;

import java.time.Instant;
import java.util.UUID;

public record MonitorQueryFilter(
        UUID tenantId,
        UUID definitionId,
        String definitionCode,
        String category,
        Instant startedFrom,
        Instant startedTo,
        int limit,
        long stalledThresholdMinutes
) {
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 500;
    private static final long DEFAULT_STALLED_MINUTES = 24 * 60;

    public MonitorQueryFilter {
        limit = normalizeLimit(limit);
        stalledThresholdMinutes = stalledThresholdMinutes > 0
                ? stalledThresholdMinutes
                : DEFAULT_STALLED_MINUTES;
    }

    public static MonitorQueryFilter of(
            UUID tenantId,
            UUID definitionId,
            String definitionCode,
            String category,
            Instant startedFrom,
            Instant startedTo,
            Integer limit,
            Long stalledThresholdMinutes
    ) {
        return new MonitorQueryFilter(
                tenantId,
                definitionId,
                blankToNull(definitionCode),
                blankToNull(category),
                startedFrom,
                startedTo,
                limit == null ? DEFAULT_LIMIT : limit,
                stalledThresholdMinutes == null ? DEFAULT_STALLED_MINUTES : stalledThresholdMinutes
        );
    }

    private static int normalizeLimit(int value) {
        if (value <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(value, MAX_LIMIT);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
