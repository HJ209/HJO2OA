package com.hjo2oa.data.report.domain;

import java.math.BigDecimal;

public record ReportTrendPoint(
        String bucket,
        BigDecimal value
) {
}
