package com.hjo2oa.data.data.sync.domain;

import java.util.List;

public record SyncPushResult(
        SyncResultSummary resultSummary,
        List<SyncDifferenceItem> differences
) {

    public SyncPushResult {
        resultSummary = resultSummary == null ? SyncResultSummary.empty() : resultSummary;
        differences = differences == null ? List.of() : List.copyOf(differences);
    }
}
