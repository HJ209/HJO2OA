package com.hjo2oa.data.report.domain;

import java.math.BigDecimal;

public record ReportRankingItem(
        int rank,
        String dimensionValue,
        BigDecimal value
) {
}
