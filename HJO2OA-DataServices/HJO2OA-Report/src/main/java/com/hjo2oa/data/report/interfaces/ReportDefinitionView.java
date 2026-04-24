package com.hjo2oa.data.report.interfaces;

import com.hjo2oa.data.report.domain.ReportDefinition;
import com.hjo2oa.data.report.domain.ReportDimensionDefinition;
import com.hjo2oa.data.report.domain.ReportMetricDefinition;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ReportDefinitionView(
        String code,
        String name,
        String reportType,
        String sourceScope,
        String refreshMode,
        String visibilityMode,
        String status,
        String tenantId,
        Integer definitionVersion,
        Instant lastRefreshedAt,
        String lastFreshnessStatus,
        Instant nextRefreshAt,
        CaliberView caliber,
        RefreshConfigView refreshConfig,
        CardProtocolView cardProtocol,
        List<MetricView> metrics,
        List<DimensionView> dimensions
) {

    public static ReportDefinitionView from(ReportDefinition reportDefinition) {
        return new ReportDefinitionView(
                reportDefinition.code(),
                reportDefinition.name(),
                reportDefinition.reportType().name(),
                reportDefinition.sourceScope().name(),
                reportDefinition.refreshMode().name(),
                reportDefinition.visibilityMode().name(),
                reportDefinition.status().name(),
                reportDefinition.tenantId(),
                reportDefinition.definitionVersion(),
                reportDefinition.lastRefreshedAt(),
                reportDefinition.lastFreshnessStatus() == null ? null : reportDefinition.lastFreshnessStatus().name(),
                reportDefinition.nextRefreshAt(),
                new CaliberView(
                        reportDefinition.caliberDefinition().sourceProviderKey(),
                        reportDefinition.caliberDefinition().subjectCode(),
                        reportDefinition.caliberDefinition().defaultTimeField(),
                        reportDefinition.caliberDefinition().organizationField(),
                        reportDefinition.caliberDefinition().dataServiceCode(),
                        reportDefinition.caliberDefinition().baseFilters(),
                        reportDefinition.caliberDefinition().triggerEventTypes(),
                        reportDefinition.caliberDefinition().description()
                ),
                new RefreshConfigView(
                        reportDefinition.refreshConfig().refreshIntervalSeconds(),
                        reportDefinition.refreshConfig().staleAfterSeconds(),
                        reportDefinition.refreshConfig().maxRows()
                ),
                reportDefinition.cardProtocol() == null
                        ? null
                        : new CardProtocolView(
                        reportDefinition.cardProtocol().cardCode(),
                        reportDefinition.cardProtocol().title(),
                        reportDefinition.cardProtocol().cardType().name(),
                        reportDefinition.cardProtocol().summaryMetricCode(),
                        reportDefinition.cardProtocol().trendMetricCode(),
                        reportDefinition.cardProtocol().rankMetricCode(),
                        reportDefinition.cardProtocol().rankDimensionCode(),
                        reportDefinition.cardProtocol().maxItems()
                ),
                reportDefinition.metrics().stream().map(MetricView::from).toList(),
                reportDefinition.dimensions().stream().map(DimensionView::from).toList()
        );
    }

    public record CaliberView(
            String sourceProviderKey,
            String subjectCode,
            String defaultTimeField,
            String organizationField,
            String dataServiceCode,
            Map<String, String> baseFilters,
            List<String> triggerEventTypes,
            String description
    ) {
    }

    public record RefreshConfigView(
            Integer refreshIntervalSeconds,
            Integer staleAfterSeconds,
            Integer maxRows
    ) {
    }

    public record CardProtocolView(
            String cardCode,
            String title,
            String cardType,
            String summaryMetricCode,
            String trendMetricCode,
            String rankMetricCode,
            String rankDimensionCode,
            Integer maxItems
    ) {
    }

    public record MetricView(
            String id,
            String metricCode,
            String metricName,
            String aggregationType,
            String sourceField,
            String formula,
            String filterExpression,
            String unit,
            boolean trendEnabled,
            boolean rankEnabled,
            int displayOrder
    ) {

        public static MetricView from(ReportMetricDefinition metricDefinition) {
            return new MetricView(
                    metricDefinition.id(),
                    metricDefinition.metricCode(),
                    metricDefinition.metricName(),
                    metricDefinition.aggregationType().name(),
                    metricDefinition.sourceField(),
                    metricDefinition.formula(),
                    metricDefinition.filterExpression(),
                    metricDefinition.unit(),
                    metricDefinition.trendEnabled(),
                    metricDefinition.rankEnabled(),
                    metricDefinition.displayOrder()
            );
        }
    }

    public record DimensionView(
            String id,
            String dimensionCode,
            String dimensionName,
            String dimensionType,
            String sourceField,
            String timeGranularity,
            boolean filterable,
            int displayOrder
    ) {

        public static DimensionView from(ReportDimensionDefinition dimensionDefinition) {
            return new DimensionView(
                    dimensionDefinition.id(),
                    dimensionDefinition.dimensionCode(),
                    dimensionDefinition.dimensionName(),
                    dimensionDefinition.dimensionType().name(),
                    dimensionDefinition.sourceField(),
                    dimensionDefinition.timeGranularity().name(),
                    dimensionDefinition.filterable(),
                    dimensionDefinition.displayOrder()
            );
        }
    }
}
