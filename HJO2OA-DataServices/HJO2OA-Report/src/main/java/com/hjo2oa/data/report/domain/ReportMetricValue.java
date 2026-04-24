package com.hjo2oa.data.report.domain;

import java.math.BigDecimal;

public record ReportMetricValue(
        String metricCode,
        String metricName,
        BigDecimal value,
        String unit
) {
}
