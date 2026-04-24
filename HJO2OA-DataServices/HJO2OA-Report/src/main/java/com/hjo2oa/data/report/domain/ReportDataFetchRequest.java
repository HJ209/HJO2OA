package com.hjo2oa.data.report.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record ReportDataFetchRequest(
        String reportCode,
        String tenantId,
        ReportSourceScope sourceScope,
        Map<String, String> baseFilters,
        int maxRows,
        ReportRefreshTriggerMode triggerMode,
        String triggerReason,
        Instant requestedAt
) {

    public ReportDataFetchRequest {
        baseFilters = baseFilters == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(baseFilters));
    }
}
