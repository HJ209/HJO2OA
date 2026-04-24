package com.hjo2oa.data.report.domain;

import java.time.Instant;
import java.util.List;

public record ReportRankingView(
        String reportCode,
        String reportName,
        String dimensionCode,
        String metricCode,
        Instant refreshedAt,
        ReportFreshnessStatus freshnessStatus,
        List<ReportRankingItem> items
) {

    public ReportRankingView {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
