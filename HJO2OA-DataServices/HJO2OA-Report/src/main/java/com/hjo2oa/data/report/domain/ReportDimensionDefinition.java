package com.hjo2oa.data.report.domain;

import java.util.Objects;

public record ReportDimensionDefinition(
        String id,
        String dimensionCode,
        String dimensionName,
        ReportDimensionType dimensionType,
        String sourceField,
        ReportTimeGranularity timeGranularity,
        boolean filterable,
        int displayOrder
) {

    public ReportDimensionDefinition {
        id = blankToNull(id);
        dimensionCode = requireText(dimensionCode, "dimensionCode");
        dimensionName = requireText(dimensionName, "dimensionName");
        Objects.requireNonNull(dimensionType, "dimensionType must not be null");
        sourceField = requireText(sourceField, "sourceField");
        timeGranularity = timeGranularity == null ? ReportTimeGranularity.NONE : timeGranularity;
        if (displayOrder < 0) {
            throw new IllegalArgumentException("displayOrder must not be negative");
        }
        if (dimensionType == ReportDimensionType.TIME && timeGranularity == ReportTimeGranularity.NONE) {
            throw new IllegalArgumentException("time dimension must declare time granularity");
        }
    }

    public boolean timeDimension() {
        return dimensionType == ReportDimensionType.TIME;
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
