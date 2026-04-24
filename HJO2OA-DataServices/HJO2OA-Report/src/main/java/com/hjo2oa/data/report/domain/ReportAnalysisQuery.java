package com.hjo2oa.data.report.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record ReportAnalysisQuery(
        Instant from,
        Instant to,
        String dimensionCode,
        String metricCode,
        Integer topN,
        Map<String, String> filters
) {

    public ReportAnalysisQuery {
        filters = filters == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(filters));
    }

    public int resolvedTopN() {
        return topN == null ? 10 : topN;
    }
}
