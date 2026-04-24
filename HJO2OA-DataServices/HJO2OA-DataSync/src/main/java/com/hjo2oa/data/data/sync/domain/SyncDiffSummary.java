package com.hjo2oa.data.data.sync.domain;

import java.util.List;

public record SyncDiffSummary(
        long differenceCount,
        long pendingCount,
        long ignoredCount,
        long manualConfirmedCount,
        long compensatedCount,
        ReconciliationStatus reconciliationStatus
) {

    public SyncDiffSummary {
        if (differenceCount < 0 || pendingCount < 0 || ignoredCount < 0
                || manualConfirmedCount < 0 || compensatedCount < 0) {
            throw new IllegalArgumentException("difference counts must not be negative");
        }
    }

    public static SyncDiffSummary clean() {
        return new SyncDiffSummary(0, 0, 0, 0, 0, ReconciliationStatus.CONSISTENT);
    }

    public static SyncDiffSummary from(List<SyncDifferenceItem> differences) {
        if (differences == null || differences.isEmpty()) {
            return clean();
        }
        long pending = differences.stream().filter(item -> item.status() == DifferenceStatus.DETECTED).count();
        long ignored = differences.stream().filter(item -> item.status() == DifferenceStatus.IGNORED).count();
        long manualConfirmed = differences.stream()
                .filter(item -> item.status() == DifferenceStatus.MANUAL_CONFIRMED)
                .count();
        long compensated = differences.stream()
                .filter(item -> item.status() == DifferenceStatus.COMPENSATED)
                .count();
        ReconciliationStatus reconciliationStatus = pending > 0
                ? ReconciliationStatus.MANUAL_REVIEW_REQUIRED
                : ReconciliationStatus.DIFFERENCES_FOUND;
        return new SyncDiffSummary(
                differences.size(),
                pending,
                ignored,
                manualConfirmed,
                compensated,
                reconciliationStatus
        );
    }
}
