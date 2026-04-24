package com.hjo2oa.data.report.domain;

import java.time.Instant;
import java.util.List;

public record ReportTrendView(
        String reportCode,
        String reportName,
        String dimensionCode,
        String metricCode,
        Instant refreshedAt,
        ReportFreshnessStatus freshnessStatus,
        List<ReportTrendPoint> points
) {

    public ReportTrendView {
        points = points == null ? List.of() : List.copyOf(points);
    }
}
