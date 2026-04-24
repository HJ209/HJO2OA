package com.hjo2oa.data.data.sync.domain;

public record SyncResultSummary(
        long sourceCount,
        long insertedCount,
        long updatedCount,
        long skippedCount,
        long failedCount
) {

    public SyncResultSummary {
        if (sourceCount < 0 || insertedCount < 0 || updatedCount < 0 || skippedCount < 0 || failedCount < 0) {
            throw new IllegalArgumentException("summary counts must not be negative");
        }
    }

    public static SyncResultSummary empty() {
        return new SyncResultSummary(0, 0, 0, 0, 0);
    }

    public long successCount() {
        return insertedCount + updatedCount + skippedCount;
    }
}
