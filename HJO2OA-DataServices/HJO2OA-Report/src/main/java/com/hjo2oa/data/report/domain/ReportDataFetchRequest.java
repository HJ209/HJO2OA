package com.hjo2oa.data.report.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record ReportDataFetchRequest(
        String reportCode,
        String tenantId,
        ReportSourceScope sourceScope,
        String subjectCode,
        String dataServiceCode,
        String defaultTimeField,
        Map<String, String> baseFilters,
        int maxRows,
        ReportRefreshTriggerMode triggerMode,
        String triggerReason,
        Instant requestedAt
) {

    public ReportDataFetchRequest {
        subjectCode = blankToNull(subjectCode);
        dataServiceCode = blankToNull(dataServiceCode);
        defaultTimeField = blankToNull(defaultTimeField);
        baseFilters = baseFilters == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(baseFilters));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
