package com.hjo2oa.data.report.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hjo2oa.data.report.domain.ReportCaliberDefinition;
import com.hjo2oa.data.report.domain.ReportCardProtocol;
import com.hjo2oa.data.report.domain.ReportDefinition;
import com.hjo2oa.data.report.domain.ReportDefinitionPage;
import com.hjo2oa.data.report.domain.ReportDefinitionQuery;
import com.hjo2oa.data.report.domain.ReportDefinitionRepository;
import com.hjo2oa.data.report.domain.ReportDimensionDefinition;
import com.hjo2oa.data.report.domain.ReportDimensionType;
import com.hjo2oa.data.report.domain.ReportFreshnessStatus;
import com.hjo2oa.data.report.domain.ReportMetricAggregationType;
import com.hjo2oa.data.report.domain.ReportMetricDefinition;
import com.hjo2oa.data.report.domain.ReportRefreshConfig;
import com.hjo2oa.data.report.domain.ReportRefreshMode;
import com.hjo2oa.data.report.domain.ReportSourceScope;
import com.hjo2oa.data.report.domain.ReportStatus;
import com.hjo2oa.data.report.domain.ReportTimeGranularity;
import com.hjo2oa.data.report.domain.ReportType;
import com.hjo2oa.data.report.domain.ReportVisibilityMode;
import javax.sql.DataSource;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisPlusReportDefinitionRepository implements ReportDefinitionRepository {

    private final ReportDefinitionMapper reportDefinitionMapper;
    private final ReportMetricDefinitionMapper reportMetricDefinitionMapper;
    private final ReportDimensionDefinitionMapper reportDimensionDefinitionMapper;
    private final ReportJsonCodec reportJsonCodec;

    public MybatisPlusReportDefinitionRepository(
            ReportDefinitionMapper reportDefinitionMapper,
            ReportMetricDefinitionMapper reportMetricDefinitionMapper,
            ReportDimensionDefinitionMapper reportDimensionDefinitionMapper,
            ReportJsonCodec reportJsonCodec
    ) {
        this.reportDefinitionMapper = Objects.requireNonNull(reportDefinitionMapper, "reportDefinitionMapper must not be null");
        this.reportMetricDefinitionMapper = Objects.requireNonNull(
                reportMetricDefinitionMapper,
                "reportMetricDefinitionMapper must not be null");
        this.reportDimensionDefinitionMapper = Objects.requireNonNull(
                reportDimensionDefinitionMapper,
                "reportDimensionDefinitionMapper must not be null");
        this.reportJsonCodec = Objects.requireNonNull(reportJsonCodec, "reportJsonCodec must not be null");
    }

    @Override
    public Optional<ReportDefinition> findByCode(String code) {
        QueryWrapper<ReportDefinitionDO> queryWrapper = new QueryWrapper<ReportDefinitionDO>()
                .eq("code", code)
                .eq("deleted", 0);
        return Optional.ofNullable(reportDefinitionMapper.selectOne(queryWrapper)).map(this::toDomain);
    }

    @Override
    public Optional<ReportDefinition> findById(String id) {
        QueryWrapper<ReportDefinitionDO> queryWrapper = new QueryWrapper<ReportDefinitionDO>()
                .eq("id", id)
                .eq("deleted", 0);
        return Optional.ofNullable(reportDefinitionMapper.selectOne(queryWrapper)).map(this::toDomain);
    }

    @Override
    @Transactional
    public ReportDefinition save(ReportDefinition reportDefinition) {
        Instant now = Instant.now();
        ReportDefinitionDO definitionDO = toDefinitionDO(reportDefinition, now);
        if (reportDefinition.id() == null) {
            reportDefinitionMapper.insert(definitionDO);
        } else {
            reportDefinitionMapper.updateById(definitionDO);
        }

        QueryWrapper<ReportMetricDefinitionDO> metricDelete = new QueryWrapper<ReportMetricDefinitionDO>()
                .eq("report_id", definitionDO.getId());
        reportMetricDefinitionMapper.delete(metricDelete);
        for (ReportMetricDefinition metricDefinition : reportDefinition.metrics()) {
            reportMetricDefinitionMapper.insert(toMetricDO(definitionDO.getId(), metricDefinition, now));
        }

        QueryWrapper<ReportDimensionDefinitionDO> dimensionDelete = new QueryWrapper<ReportDimensionDefinitionDO>()
                .eq("report_id", definitionDO.getId());
        reportDimensionDefinitionMapper.delete(dimensionDelete);
        for (ReportDimensionDefinition dimensionDefinition : reportDefinition.dimensions()) {
            reportDimensionDefinitionMapper.insert(toDimensionDO(definitionDO.getId(), dimensionDefinition, now));
        }
        return findById(definitionDO.getId()).orElseThrow();
    }

    @Override
    public ReportDefinitionPage page(ReportDefinitionQuery query) {
        QueryWrapper<ReportDefinitionDO> queryWrapper = new QueryWrapper<ReportDefinitionDO>()
                .eq("deleted", 0)
                .orderByDesc("updated_at");
        if (query.reportType() != null) {
            queryWrapper.eq("report_type", query.reportType().name());
        }
        if (query.sourceScope() != null) {
            queryWrapper.eq("source_scope", query.sourceScope().name());
        }
        if (query.status() != null) {
            queryWrapper.eq("status", query.status().name());
        }
        if (query.visibilityMode() != null) {
            queryWrapper.eq("visibility_mode", query.visibilityMode().name());
        }
        List<ReportDefinitionDO> allItems = reportDefinitionMapper.selectList(queryWrapper);
        int fromIndex = Math.min((query.page() - 1) * query.size(), allItems.size());
        int toIndex = Math.min(fromIndex + query.size(), allItems.size());
        List<ReportDefinition> items = allItems.subList(fromIndex, toIndex).stream().map(this::toDomain).toList();
        return new ReportDefinitionPage(items, allItems.size());
    }

    @Override
    public List<ReportDefinition> findByRefreshModeAndStatus(ReportRefreshMode refreshMode, ReportStatus status) {
        QueryWrapper<ReportDefinitionDO> queryWrapper = new QueryWrapper<ReportDefinitionDO>()
                .eq("refresh_mode", refreshMode.name())
                .eq("status", status.name())
                .eq("deleted", 0)
                .orderByDesc("updated_at");
        return reportDefinitionMapper.selectList(queryWrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public List<ReportDefinition> findDueScheduledReports(Instant now) {
        QueryWrapper<ReportDefinitionDO> queryWrapper = new QueryWrapper<ReportDefinitionDO>()
                .eq("refresh_mode", ReportRefreshMode.SCHEDULED.name())
                .eq("status", ReportStatus.ACTIVE.name())
                .eq("deleted", 0)
                .le("next_refresh_at", now)
                .orderByAsc("next_refresh_at");
        return reportDefinitionMapper.selectList(queryWrapper).stream().map(this::toDomain).toList();
    }

    private ReportDefinition toDomain(ReportDefinitionDO definitionDO) {
        List<ReportMetricDefinition> metrics = reportMetricDefinitionMapper.selectList(new QueryWrapper<ReportMetricDefinitionDO>()
                        .eq("report_id", definitionDO.getId())
                        .eq("deleted", 0)
                        .orderByAsc("display_order"))
                .stream()
                .map(this::toMetric)
                .toList();
        List<ReportDimensionDefinition> dimensions = reportDimensionDefinitionMapper.selectList(
                        new QueryWrapper<ReportDimensionDefinitionDO>()
                                .eq("report_id", definitionDO.getId())
                                .eq("deleted", 0)
                                .orderByAsc("display_order"))
                .stream()
                .map(this::toDimension)
                .toList();
        return new ReportDefinition(
                definitionDO.getId(),
                definitionDO.getCode(),
                definitionDO.getName(),
                ReportType.valueOf(definitionDO.getReportType()),
                ReportSourceScope.valueOf(definitionDO.getSourceScope()),
                ReportRefreshMode.valueOf(definitionDO.getRefreshMode()),
                ReportVisibilityMode.valueOf(definitionDO.getVisibilityMode()),
                ReportStatus.valueOf(definitionDO.getStatus()),
                definitionDO.getTenantId(),
                definitionDO.getDefinitionVersion(),
                reportJsonCodec.read(definitionDO.getCaliberDefinition(), ReportCaliberDefinition.class),
                reportJsonCodec.read(definitionDO.getRefreshConfig(), ReportRefreshConfig.class),
                reportJsonCodec.read(definitionDO.getCardProtocol(), ReportCardProtocol.class),
                definitionDO.getLastRefreshedAt(),
                definitionDO.getLastFreshnessStatus() == null
                        ? null
                        : ReportFreshnessStatus.valueOf(definitionDO.getLastFreshnessStatus()),
                definitionDO.getLastRefreshBatch(),
                definitionDO.getNextRefreshAt(),
                definitionDO.getCreatedAt(),
                definitionDO.getUpdatedAt(),
                metrics,
                dimensions
        );
    }

    private ReportMetricDefinition toMetric(ReportMetricDefinitionDO metricDO) {
        return new ReportMetricDefinition(
                metricDO.getId(),
                metricDO.getMetricCode(),
                metricDO.getMetricName(),
                ReportMetricAggregationType.valueOf(metricDO.getAggregationType()),
                metricDO.getSourceField(),
                metricDO.getFormula(),
                metricDO.getFilterExpression(),
                metricDO.getUnit(),
                Boolean.TRUE.equals(metricDO.getTrendEnabled()),
                Boolean.TRUE.equals(metricDO.getRankEnabled()),
                metricDO.getDisplayOrder() == null ? 0 : metricDO.getDisplayOrder()
        );
    }

    private ReportDimensionDefinition toDimension(ReportDimensionDefinitionDO dimensionDO) {
        return new ReportDimensionDefinition(
                dimensionDO.getId(),
                dimensionDO.getDimensionCode(),
                dimensionDO.getDimensionName(),
                ReportDimensionType.valueOf(dimensionDO.getDimensionType()),
                dimensionDO.getSourceField(),
                dimensionDO.getTimeGranularity() == null
                        ? null
                        : ReportTimeGranularity.valueOf(dimensionDO.getTimeGranularity()),
                Boolean.TRUE.equals(dimensionDO.getFilterable()),
                dimensionDO.getDisplayOrder() == null ? 0 : dimensionDO.getDisplayOrder()
        );
    }

    private ReportDefinitionDO toDefinitionDO(ReportDefinition reportDefinition, Instant now) {
        Instant createdAt = reportDefinition.createdAt() == null ? now : reportDefinition.createdAt();
        return new ReportDefinitionDO()
                .setId(reportDefinition.id() == null ? UUID.randomUUID().toString() : reportDefinition.id())
                .setCode(reportDefinition.code())
                .setName(reportDefinition.name())
                .setReportType(reportDefinition.reportType().name())
                .setSourceScope(reportDefinition.sourceScope().name())
                .setRefreshMode(reportDefinition.refreshMode().name())
                .setVisibilityMode(reportDefinition.visibilityMode().name())
                .setStatus(reportDefinition.status().name())
                .setTenantId(reportDefinition.tenantId())
                .setDefinitionVersion(reportDefinition.definitionVersion())
                .setCaliberDefinition(reportJsonCodec.write(reportDefinition.caliberDefinition()))
                .setRefreshConfig(reportJsonCodec.write(reportDefinition.refreshConfig()))
                .setCardProtocol(reportJsonCodec.write(reportDefinition.cardProtocol()))
                .setLastRefreshedAt(reportDefinition.lastRefreshedAt())
                .setLastFreshnessStatus(reportDefinition.lastFreshnessStatus() == null
                        ? null
                        : reportDefinition.lastFreshnessStatus().name())
                .setLastRefreshBatch(reportDefinition.lastRefreshBatch())
                .setNextRefreshAt(reportDefinition.nextRefreshAt())
                .setDeleted(Boolean.FALSE)
                .setCreatedAt(createdAt)
                .setUpdatedAt(now);
    }

    private ReportMetricDefinitionDO toMetricDO(String reportId, ReportMetricDefinition metricDefinition, Instant now) {
        return new ReportMetricDefinitionDO()
                .setId(metricDefinition.id() == null ? UUID.randomUUID().toString() : metricDefinition.id())
                .setReportId(reportId)
                .setMetricCode(metricDefinition.metricCode())
                .setMetricName(metricDefinition.metricName())
                .setAggregationType(metricDefinition.aggregationType().name())
                .setSourceField(metricDefinition.sourceField())
                .setFormula(metricDefinition.formula())
                .setFilterExpression(metricDefinition.filterExpression())
                .setUnit(metricDefinition.unit())
                .setTrendEnabled(metricDefinition.trendEnabled())
                .setRankEnabled(metricDefinition.rankEnabled())
                .setDisplayOrder(metricDefinition.displayOrder())
                .setDeleted(Boolean.FALSE)
                .setCreatedAt(now)
                .setUpdatedAt(now);
    }

    private ReportDimensionDefinitionDO toDimensionDO(
            String reportId,
            ReportDimensionDefinition dimensionDefinition,
            Instant now
    ) {
        return new ReportDimensionDefinitionDO()
                .setId(dimensionDefinition.id() == null ? UUID.randomUUID().toString() : dimensionDefinition.id())
                .setReportId(reportId)
                .setDimensionCode(dimensionDefinition.dimensionCode())
                .setDimensionName(dimensionDefinition.dimensionName())
                .setDimensionType(dimensionDefinition.dimensionType().name())
                .setSourceField(dimensionDefinition.sourceField())
                .setTimeGranularity(dimensionDefinition.timeGranularity().name())
                .setFilterable(dimensionDefinition.filterable())
                .setDisplayOrder(dimensionDefinition.displayOrder())
                .setDeleted(Boolean.FALSE)
                .setCreatedAt(now)
                .setUpdatedAt(now);
    }
}
