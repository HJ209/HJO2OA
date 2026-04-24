package com.hjo2oa.data.report.domain;

import java.time.Instant;
import java.util.List;

public record ReportCardDataSource(
        String reportCode,
        String reportName,
        String cardCode,
        String title,
        ReportCardType cardType,
        Instant refreshedAt,
        ReportFreshnessStatus freshnessStatus,
        ReportMetricValue summaryMetric,
        List<ReportTrendPoint> trend,
        List<ReportRankingItem> ranking
) {

    public ReportCardDataSource {
        trend = trend == null ? List.of() : List.copyOf(trend);
        ranking = ranking == null ? List.of() : List.copyOf(ranking);
    }
}
