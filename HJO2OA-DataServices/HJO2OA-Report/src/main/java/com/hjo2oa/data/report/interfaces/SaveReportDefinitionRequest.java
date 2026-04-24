package com.hjo2oa.data.report.interfaces;

import com.hjo2oa.data.report.domain.ReportCardType;
import com.hjo2oa.data.report.domain.ReportDimensionType;
import com.hjo2oa.data.report.domain.ReportMetricAggregationType;
import com.hjo2oa.data.report.domain.ReportRefreshMode;
import com.hjo2oa.data.report.domain.ReportSourceScope;
import com.hjo2oa.data.report.domain.ReportTimeGranularity;
import com.hjo2oa.data.report.domain.ReportType;
import com.hjo2oa.data.report.domain.ReportVisibilityMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record SaveReportDefinitionRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull ReportType reportType,
        @NotNull ReportSourceScope sourceScope,
        @NotNull ReportRefreshMode refreshMode,
        @NotNull ReportVisibilityMode visibilityMode,
        @NotBlank String tenantId,
        @NotNull @Valid ReportCaliberRequest caliber,
        @Valid ReportRefreshConfigRequest refreshConfig,
        @Valid ReportCardProtocolRequest cardProtocol,
        @NotEmpty List<@Valid ReportMetricRequest> metrics,
        List<@Valid ReportDimensionRequest> dimensions
) {

    public record ReportCaliberRequest(
            @NotBlank String sourceProviderKey,
            @NotBlank String subjectCode,
            String defaultTimeField,
            String organizationField,
            String dataServiceCode,
            Map<String, String> baseFilters,
            List<String> triggerEventTypes,
            String description
    ) {
    }

    public record ReportRefreshConfigRequest(
            @Min(1) Integer refreshIntervalSeconds,
            @Min(1) Integer staleAfterSeconds,
            @Min(1) Integer maxRows
    ) {
    }

    public record ReportCardProtocolRequest(
            @NotBlank String cardCode,
            @NotBlank String title,
            @NotNull ReportCardType cardType,
            String summaryMetricCode,
            String trendMetricCode,
            String rankMetricCode,
            String rankDimensionCode,
            @Min(1) Integer maxItems
    ) {
    }

    public record ReportMetricRequest(
            String id,
            @NotBlank String metricCode,
            @NotBlank String metricName,
            @NotNull ReportMetricAggregationType aggregationType,
            String sourceField,
            String formula,
            String filterExpression,
            String unit,
            boolean trendEnabled,
            boolean rankEnabled,
            @Min(0) int displayOrder
    ) {
    }

    public record ReportDimensionRequest(
            String id,
            @NotBlank String dimensionCode,
            @NotBlank String dimensionName,
            @NotNull ReportDimensionType dimensionType,
            @NotBlank String sourceField,
            ReportTimeGranularity timeGranularity,
            boolean filterable,
            @Min(0) int displayOrder
    ) {
    }
}
