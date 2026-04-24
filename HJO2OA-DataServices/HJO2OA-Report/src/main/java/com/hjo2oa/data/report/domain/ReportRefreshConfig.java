package com.hjo2oa.data.report.domain;

public record ReportRefreshConfig(
        Integer refreshIntervalSeconds,
        Integer staleAfterSeconds,
        Integer maxRows
) {

    public ReportRefreshConfig {
        if (refreshIntervalSeconds != null && refreshIntervalSeconds < 1) {
            throw new IllegalArgumentException("refreshIntervalSeconds must be greater than 0");
        }
        if (staleAfterSeconds != null && staleAfterSeconds < 1) {
            throw new IllegalArgumentException("staleAfterSeconds must be greater than 0");
        }
        if (maxRows != null && maxRows < 1) {
            throw new IllegalArgumentException("maxRows must be greater than 0");
        }
    }

    public int effectiveRefreshIntervalSeconds() {
        return refreshIntervalSeconds == null ? 300 : refreshIntervalSeconds;
    }

    public int effectiveStaleAfterSeconds() {
        return staleAfterSeconds == null ? 900 : staleAfterSeconds;
    }

    public int effectiveMaxRows() {
        return maxRows == null ? 10000 : maxRows;
    }
}
