package com.hjo2oa.data.data.sync.domain;

import java.util.List;

public record SyncReconciliationResult(
        ReconciliationStatus reconciliationStatus,
        List<SyncDifferenceItem> differences
) {

    public SyncReconciliationResult {
        reconciliationStatus = reconciliationStatus == null
                ? ReconciliationStatus.NOT_CHECKED
                : reconciliationStatus;
        differences = differences == null ? List.of() : List.copyOf(differences);
    }
}
