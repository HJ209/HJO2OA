package com.hjo2oa.data.report.domain;

import java.util.Objects;

public record ReportCardProtocol(
        String cardCode,
        String title,
        ReportCardType cardType,
        String summaryMetricCode,
        String trendMetricCode,
        String rankMetricCode,
        String rankDimensionCode,
        Integer maxItems
) {

    public ReportCardProtocol {
        cardCode = requireText(cardCode, "cardCode");
        title = requireText(title, "title");
        Objects.requireNonNull(cardType, "cardType must not be null");
        summaryMetricCode = blankToNull(summaryMetricCode);
        trendMetricCode = blankToNull(trendMetricCode);
        rankMetricCode = blankToNull(rankMetricCode);
        rankDimensionCode = blankToNull(rankDimensionCode);
        maxItems = maxItems == null ? 5 : maxItems;
        if (maxItems < 1) {
            throw new IllegalArgumentException("maxItems must be greater than 0");
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
