package com.hjo2oa.data.report.domain;

import java.time.Instant;
import java.util.List;

public record ReportSummaryView(
        String reportCode,
        String reportName,
        Instant refreshedAt,
        ReportFreshnessStatus freshnessStatus,
        List<ReportMetricValue> metrics
) {

    public ReportSummaryView {
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }
}
