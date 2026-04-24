package com.hjo2oa.data.report.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ReportDefinition {

    private final String id;
    private final String code;
    private final String name;
    private final ReportType reportType;
    private final ReportSourceScope sourceScope;
    private final ReportRefreshMode refreshMode;
    private final ReportVisibilityMode visibilityMode;
    private final ReportStatus status;
    private final String tenantId;
    private final Integer definitionVersion;
    private final ReportCaliberDefinition caliberDefinition;
    private final ReportRefreshConfig refreshConfig;
    private final ReportCardProtocol cardProtocol;
    private final Instant lastRefreshedAt;
    private final ReportFreshnessStatus lastFreshnessStatus;
    private final String lastRefreshBatch;
    private final Instant nextRefreshAt;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<ReportMetricDefinition> metrics;
    private final List<ReportDimensionDefinition> dimensions;

    public ReportDefinition(
            String id,
            String code,
            String name,
            ReportType reportType,
            ReportSourceScope sourceScope,
            ReportRefreshMode refreshMode,
            ReportVisibilityMode visibilityMode,
            ReportStatus status,
            String tenantId,
            Integer definitionVersion,
            ReportCaliberDefinition caliberDefinition,
            ReportRefreshConfig refreshConfig,
            ReportCardProtocol cardProtocol,
            Instant lastRefreshedAt,
            ReportFreshnessStatus lastFreshnessStatus,
            String lastRefreshBatch,
            Instant nextRefreshAt,
            Instant createdAt,
            Instant updatedAt,
            List<ReportMetricDefinition> metrics,
            List<ReportDimensionDefinition> dimensions
    ) {
        this.id = blankToNull(id);
        this.code = requireText(code, "code");
        this.name = requireText(name, "name");
        this.reportType = Objects.requireNonNull(reportType, "reportType must not be null");
        this.sourceScope = Objects.requireNonNull(sourceScope, "sourceScope must not be null");
        this.refreshMode = Objects.requireNonNull(refreshMode, "refreshMode must not be null");
        this.visibilityMode = Objects.requireNonNull(visibilityMode, "visibilityMode must not be null");
        this.status = status == null ? ReportStatus.DRAFT : status;
        this.tenantId = requireText(tenantId, "tenantId");
        this.definitionVersion = definitionVersion == null ? 1 : definitionVersion;
        this.caliberDefinition = Objects.requireNonNull(caliberDefinition, "caliberDefinition must not be null");
        this.refreshConfig = refreshConfig == null ? new ReportRefreshConfig(null, null, null) : refreshConfig;
        this.cardProtocol = cardProtocol;
        this.lastRefreshedAt = lastRefreshedAt;
        this.lastFreshnessStatus = lastFreshnessStatus;
        this.lastRefreshBatch = blankToNull(lastRefreshBatch);
        this.nextRefreshAt = nextRefreshAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.metrics = metrics == null ? List.of() : List.copyOf(metrics);
        this.dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        validate();
    }

    public static ReportDefinition draft(
            String code,
            String name,
            ReportType reportType,
            ReportSourceScope sourceScope,
            ReportRefreshMode refreshMode,
            ReportVisibilityMode visibilityMode,
            String tenantId,
            ReportCaliberDefinition caliberDefinition,
            ReportRefreshConfig refreshConfig,
            ReportCardProtocol cardProtocol,
            List<ReportMetricDefinition> metrics,
            List<ReportDimensionDefinition> dimensions,
            Instant now
    ) {
        return new ReportDefinition(
                null,
                code,
                name,
                reportType,
                sourceScope,
                refreshMode,
                visibilityMode,
                ReportStatus.DRAFT,
                tenantId,
                1,
                caliberDefinition,
                refreshConfig,
                cardProtocol,
                null,
                null,
                null,
                nextRefreshAt(refreshMode, refreshConfig, now),
                now,
                now,
                metrics,
                dimensions
        );
    }

    public ReportDefinition activate(Instant now) {
        return withStatus(ReportStatus.ACTIVE, now);
    }

    public ReportDefinition archive(Instant now) {
        return withStatus(ReportStatus.ARCHIVED, now);
    }

    public ReportDefinition markRefreshCompleted(
            Instant refreshedAt,
            ReportFreshnessStatus freshnessStatus,
            String refreshBatch
    ) {
        return new ReportDefinition(
                id,
                code,
                name,
                reportType,
                sourceScope,
                refreshMode,
                visibilityMode,
                status,
                tenantId,
                definitionVersion,
                caliberDefinition,
                refreshConfig,
                cardProtocol,
                refreshedAt,
                freshnessStatus,
                refreshBatch,
                calculateNextRefreshAt(refreshedAt),
                createdAt,
                refreshedAt,
                metrics,
                dimensions
        );
    }

    public ReportDefinition markStale(Instant now) {
        return new ReportDefinition(
                id,
                code,
                name,
                reportType,
                sourceScope,
                refreshMode,
                visibilityMode,
                status,
                tenantId,
                definitionVersion,
                caliberDefinition,
                refreshConfig,
                cardProtocol,
                lastRefreshedAt,
                ReportFreshnessStatus.STALE,
                lastRefreshBatch,
                nextRefreshAt,
                createdAt,
                now,
                metrics,
                dimensions
        );
    }

    public ReportDefinition withNewVersion(
            String newName,
            ReportType newReportType,
            ReportSourceScope newSourceScope,
            ReportRefreshMode newRefreshMode,
            ReportVisibilityMode newVisibilityMode,
            ReportCaliberDefinition newCaliberDefinition,
            ReportRefreshConfig newRefreshConfig,
            ReportCardProtocol newCardProtocol,
            List<ReportMetricDefinition> newMetrics,
            List<ReportDimensionDefinition> newDimensions,
            Instant now
    ) {
        return new ReportDefinition(
                id,
                code,
                newName,
                newReportType,
                newSourceScope,
                newRefreshMode,
                newVisibilityMode,
                status == ReportStatus.ARCHIVED ? ReportStatus.DRAFT : status,
                tenantId,
                definitionVersion + 1,
                newCaliberDefinition,
                newRefreshConfig,
                newCardProtocol,
                lastRefreshedAt,
                lastFreshnessStatus,
                lastRefreshBatch,
                nextRefreshAt(newRefreshMode, newRefreshConfig, now),
                createdAt,
                now,
                newMetrics,
                newDimensions
        );
    }

    public boolean dueForScheduledRefresh(Instant now) {
        return status == ReportStatus.ACTIVE
                && refreshMode == ReportRefreshMode.SCHEDULED
                && nextRefreshAt != null
                && !nextRefreshAt.isAfter(now);
    }

    public boolean portalCardVisible() {
        return visibilityMode == ReportVisibilityMode.PORTAL_CARD;
    }

    public ReportDimensionDefinition defaultTimeDimension() {
        return dimensions.stream().filter(ReportDimensionDefinition::timeDimension).findFirst().orElse(null);
    }

    public ReportDimensionDefinition dimension(String dimensionCode) {
        return dimensions.stream()
                .filter(dimension -> dimension.dimensionCode().equals(dimensionCode))
                .findFirst()
                .orElse(null);
    }

    public ReportMetricDefinition metric(String metricCode) {
        return metrics.stream()
                .filter(metric -> metric.metricCode().equals(metricCode))
                .findFirst()
                .orElse(null);
    }

    public String id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public ReportType reportType() {
        return reportType;
    }

    public ReportSourceScope sourceScope() {
        return sourceScope;
    }

    public ReportRefreshMode refreshMode() {
        return refreshMode;
    }

    public ReportVisibilityMode visibilityMode() {
        return visibilityMode;
    }

    public ReportStatus status() {
        return status;
    }

    public String tenantId() {
        return tenantId;
    }

    public Integer definitionVersion() {
        return definitionVersion;
    }

    public ReportCaliberDefinition caliberDefinition() {
        return caliberDefinition;
    }

    public ReportRefreshConfig refreshConfig() {
        return refreshConfig;
    }

    public ReportCardProtocol cardProtocol() {
        return cardProtocol;
    }

    public Instant lastRefreshedAt() {
        return lastRefreshedAt;
    }

    public ReportFreshnessStatus lastFreshnessStatus() {
        return lastFreshnessStatus;
    }

    public String lastRefreshBatch() {
        return lastRefreshBatch;
    }

    public Instant nextRefreshAt() {
        return nextRefreshAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<ReportMetricDefinition> metrics() {
        return metrics;
    }

    public List<ReportDimensionDefinition> dimensions() {
        return dimensions;
    }

    private void validate() {
        if (metrics.isEmpty()) {
            throw new IllegalArgumentException("report definition must contain at least one metric definition");
        }
        ensureUniqueMetricCodes();
        ensureUniqueDimensionCodes();
        if (portalCardVisible() && cardProtocol == null) {
            throw new IllegalArgumentException("portal card report must define card protocol");
        }
        if (portalCardVisible() && metrics.isEmpty()) {
            throw new IllegalArgumentException("portal card report must define statistical caliber and metrics");
        }
        if (refreshMode == ReportRefreshMode.SCHEDULED && refreshConfig.refreshIntervalSeconds() == null) {
            throw new IllegalArgumentException("scheduled report must declare refresh interval");
        }
    }

    private void ensureUniqueMetricCodes() {
        Set<String> codes = new LinkedHashSet<>();
        for (ReportMetricDefinition metric : metrics) {
            if (!codes.add(metric.metricCode())) {
                throw new IllegalArgumentException("duplicate metricCode: " + metric.metricCode());
            }
        }
    }

    private void ensureUniqueDimensionCodes() {
        Set<String> codes = new LinkedHashSet<>();
        for (ReportDimensionDefinition dimension : dimensions) {
            if (!codes.add(dimension.dimensionCode())) {
                throw new IllegalArgumentException("duplicate dimensionCode: " + dimension.dimensionCode());
            }
        }
    }

    private ReportDefinition withStatus(ReportStatus newStatus, Instant now) {
        return new ReportDefinition(
                id,
                code,
                name,
                reportType,
                sourceScope,
                refreshMode,
                visibilityMode,
                newStatus,
                tenantId,
                definitionVersion,
                caliberDefinition,
                refreshConfig,
                cardProtocol,
                lastRefreshedAt,
                lastFreshnessStatus,
                lastRefreshBatch,
                nextRefreshAt(refreshMode, refreshConfig, now),
                createdAt,
                now,
                metrics,
                dimensions
        );
    }

    private Instant calculateNextRefreshAt(Instant baseTime) {
        return nextRefreshAt(refreshMode, refreshConfig, baseTime);
    }

    private static Instant nextRefreshAt(
            ReportRefreshMode refreshMode,
            ReportRefreshConfig refreshConfig,
            Instant baseTime
    ) {
        if (baseTime == null) {
            return null;
        }
        if (refreshMode != ReportRefreshMode.SCHEDULED) {
            return null;
        }
        return baseTime.plusSeconds(refreshConfig.effectiveRefreshIntervalSeconds());
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
