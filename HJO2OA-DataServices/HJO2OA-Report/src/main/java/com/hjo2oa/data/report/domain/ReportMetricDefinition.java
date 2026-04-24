package com.hjo2oa.data.report.domain;

import java.util.Objects;

public record ReportMetricDefinition(
        String id,
        String metricCode,
        String metricName,
        ReportMetricAggregationType aggregationType,
        String sourceField,
        String formula,
        String filterExpression,
        String unit,
        boolean trendEnabled,
        boolean rankEnabled,
        int displayOrder
) {

    public ReportMetricDefinition {
        id = blankToNull(id);
        metricCode = requireText(metricCode, "metricCode");
        metricName = requireText(metricName, "metricName");
        Objects.requireNonNull(aggregationType, "aggregationType must not be null");
        sourceField = blankToNull(sourceField);
        formula = blankToNull(formula);
        filterExpression = blankToNull(filterExpression);
        unit = blankToNull(unit);
        if (displayOrder < 0) {
            throw new IllegalArgumentException("displayOrder must not be negative");
        }
        if (aggregationType != ReportMetricAggregationType.COUNT
                && aggregationType != ReportMetricAggregationType.RATIO
                && sourceField == null) {
            throw new IllegalArgumentException("sourceField must not be blank for aggregation " + aggregationType);
        }
        if (aggregationType == ReportMetricAggregationType.RATIO && formula == null) {
            throw new IllegalArgumentException("formula must not be blank for ratio metric");
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
