package com.hjo2oa.data.data.sync.domain;

import java.util.List;

public record SyncCompensationResult(
        SyncResultSummary resultSummary,
        ReconciliationStatus reconciliationStatus,
        List<SyncDifferenceItem> differences
) {

    public SyncCompensationResult {
        resultSummary = resultSummary == null ? SyncResultSummary.empty() : resultSummary;
        reconciliationStatus = reconciliationStatus == null
                ? ReconciliationStatus.NOT_CHECKED
                : reconciliationStatus;
        differences = differences == null ? List.of() : List.copyOf(differences);
    }
}
