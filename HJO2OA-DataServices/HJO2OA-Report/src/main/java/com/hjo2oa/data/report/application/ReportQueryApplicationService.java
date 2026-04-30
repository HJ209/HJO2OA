package com.hjo2oa.data.report.application;

import com.hjo2oa.data.report.domain.ReportAnalysisQuery;
import com.hjo2oa.data.report.domain.ReportCardDataSource;
import com.hjo2oa.data.report.domain.ReportCardProtocol;
import com.hjo2oa.data.report.domain.ReportCardType;
import com.hjo2oa.data.report.domain.ReportDataRecord;
import com.hjo2oa.data.report.domain.ReportDefinition;
import com.hjo2oa.data.report.domain.ReportDefinitionRepository;
import com.hjo2oa.data.report.domain.ReportFreshnessStatus;
import com.hjo2oa.data.report.domain.ReportMetricDefinition;
import com.hjo2oa.data.report.domain.ReportMetricValue;
import com.hjo2oa.data.report.domain.ReportRankingItem;
import com.hjo2oa.data.report.domain.ReportRankingView;
import com.hjo2oa.data.report.domain.ReportRefreshTriggerMode;
import com.hjo2oa.data.report.domain.ReportSnapshot;
import com.hjo2oa.data.report.domain.ReportSnapshotCache;
import com.hjo2oa.data.report.domain.ReportSnapshotPage;
import com.hjo2oa.data.report.domain.ReportSnapshotRepository;
import com.hjo2oa.data.report.domain.ReportSummaryView;
import com.hjo2oa.data.report.domain.ReportTrendPoint;
import com.hjo2oa.data.report.domain.ReportTrendView;
import com.hjo2oa.shared.kernel.BizException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import org.springframework.stereotype.Service;

@Service
public class ReportQueryApplicationService {

    private final ReportDefinitionRepository reportDefinitionRepository;
    private final ReportSnapshotRepository reportSnapshotRepository;
    private final ReportSnapshotCache reportSnapshotCache;
    private final ReportAnalysisEngine reportAnalysisEngine;
    private final ReportRefreshApplicationService reportRefreshApplicationService;

    public ReportQueryApplicationService(
            ReportDefinitionRepository reportDefinitionRepository,
            ReportSnapshotRepository reportSnapshotRepository,
            ReportSnapshotCache reportSnapshotCache,
            ReportAnalysisEngine reportAnalysisEngine,
            ReportRefreshApplicationService reportRefreshApplicationService
    ) {
        this.reportDefinitionRepository = Objects.requireNonNull(
                reportDefinitionRepository,
                "reportDefinitionRepository must not be null");
        this.reportSnapshotRepository = Objects.requireNonNull(
                reportSnapshotRepository,
                "reportSnapshotRepository must not be null");
        this.reportSnapshotCache = Objects.requireNonNull(reportSnapshotCache, "reportSnapshotCache must not be null");
        this.reportAnalysisEngine = Objects.requireNonNull(reportAnalysisEngine, "reportAnalysisEngine must not be null");
        this.reportRefreshApplicationService = Objects.requireNonNull(
                reportRefreshApplicationService,
                "reportRefreshApplicationService must not be null");
    }

    public ReportSummaryView summary(String code, ReportAnalysisQuery query) {
        ReportDefinition reportDefinition = getByCode(code);
        SnapshotHolder snapshotHolder = loadSnapshot(reportDefinition);
        List<ReportMetricValue> metrics = reportAnalysisEngine.summarize(
                reportDefinition,
                snapshotHolder.snapshot().payload().rows(),
                query
        );
        return new ReportSummaryView(
                reportDefinition.code(),
                reportDefinition.name(),
                snapshotHolder.snapshot().snapshotAt(),
                snapshotHolder.freshnessStatus(),
                metrics
        );
    }

    public ReportTrendView trend(String code, ReportAnalysisQuery query) {
        ReportDefinition reportDefinition = getByCode(code);
        SnapshotHolder snapshotHolder = loadSnapshot(reportDefinition);
        String metricCode = resolveMetricCode(reportDefinition, query.metricCode(), true);
        String dimensionCode = query.dimensionCode() == null || query.dimensionCode().isBlank()
                ? reportDefinition.defaultTimeDimension().dimensionCode()
                : query.dimensionCode();
        List<ReportTrendPoint> points = reportAnalysisEngine.trend(
                reportDefinition,
                snapshotHolder.snapshot().payload().rows(),
                query
        );
        return new ReportTrendView(
                reportDefinition.code(),
                reportDefinition.name(),
                dimensionCode,
                metricCode,
                snapshotHolder.snapshot().snapshotAt(),
                snapshotHolder.freshnessStatus(),
                points
        );
    }

    public ReportRankingView ranking(String code, ReportAnalysisQuery query) {
        ReportDefinition reportDefinition = getByCode(code);
        SnapshotHolder snapshotHolder = loadSnapshot(reportDefinition);
        String metricCode = resolveMetricCode(reportDefinition, query.metricCode(), false);
        String dimensionCode = resolveRankingDimensionCode(reportDefinition, query.dimensionCode());
        ReportAnalysisQuery resolvedQuery = new ReportAnalysisQuery(
                query.from(),
                query.to(),
                dimensionCode,
                metricCode,
                query.topN(),
                query.filters()
        );
        List<ReportRankingItem> items = reportAnalysisEngine.ranking(
                reportDefinition,
                snapshotHolder.snapshot().payload().rows(),
                resolvedQuery
        );
        return new ReportRankingView(
                reportDefinition.code(),
                reportDefinition.name(),
                dimensionCode,
                metricCode,
                snapshotHolder.snapshot().snapshotAt(),
                snapshotHolder.freshnessStatus(),
                items
        );
    }

    public ReportCardDataSource cardDataSource(String code, ReportAnalysisQuery query) {
        ReportDefinition reportDefinition = getByCode(code);
        if (!reportDefinition.portalCardVisible() || reportDefinition.cardProtocol() == null) {
            throw new BizException(
                    ReportErrorDescriptors.REPORT_CARD_NOT_AVAILABLE,
                    "report does not expose portal card data source: " + code
            );
        }
        SnapshotHolder snapshotHolder = loadSnapshot(reportDefinition);
        ReportCardProtocol cardProtocol = reportDefinition.cardProtocol();
        List<ReportMetricValue> summaryMetrics = reportAnalysisEngine.summarize(
                reportDefinition,
                snapshotHolder.snapshot().payload().rows(),
                query
        );
        ReportMetricValue summaryMetric = cardProtocol.summaryMetricCode() == null
                ? summaryMetrics.stream().findFirst().orElse(null)
                : summaryMetrics.stream()
                .filter(metric -> metric.metricCode().equals(cardProtocol.summaryMetricCode()))
                .findFirst()
                .orElse(null);
        List<ReportTrendPoint> trend = List.of();
        if (cardProtocol.cardType() == ReportCardType.TREND || cardProtocol.cardType() == ReportCardType.MIXED) {
            trend = reportAnalysisEngine.trend(
                    reportDefinition,
                    snapshotHolder.snapshot().payload().rows(),
                    new ReportAnalysisQuery(
                            query.from(),
                            query.to(),
                            null,
                            cardProtocol.trendMetricCode(),
                            query.topN(),
                            query.filters()
                    )
            );
        }
        List<ReportRankingItem> ranking = List.of();
        if (cardProtocol.cardType() == ReportCardType.RANK || cardProtocol.cardType() == ReportCardType.MIXED) {
            ranking = reportAnalysisEngine.ranking(
                    reportDefinition,
                    snapshotHolder.snapshot().payload().rows(),
                    new ReportAnalysisQuery(
                            query.from(),
                            query.to(),
                            cardProtocol.rankDimensionCode(),
                            cardProtocol.rankMetricCode(),
                            cardProtocol.maxItems(),
                            query.filters()
                    )
            );
        }
        return new ReportCardDataSource(
                reportDefinition.code(),
                reportDefinition.name(),
                cardProtocol.cardCode(),
                cardProtocol.title(),
                cardProtocol.cardType(),
                snapshotHolder.snapshot().snapshotAt(),
                snapshotHolder.freshnessStatus(),
                summaryMetric,
                trend,
                ranking
        );
    }

    public ReportSnapshotPage snapshots(String code, int page, int size) {
        ReportDefinition reportDefinition = getByCode(code);
        return reportSnapshotRepository.pageByReportId(reportDefinition.id(), page, size);
    }

    public ReportExportFile exportCsv(String code, ReportAnalysisQuery query) {
        ReportDefinition reportDefinition = getByCode(code);
        SnapshotHolder snapshotHolder = loadSnapshot(reportDefinition);
        List<ReportDataRecord> rows = reportAnalysisEngine.filterForExport(
                reportDefinition,
                snapshotHolder.snapshot().payload().rows(),
                query
        );
        List<String> headers = exportHeaders(rows);
        StringBuilder csv = new StringBuilder();
        csv.append(toCsvLine(headers)).append('\n');
        for (ReportDataRecord row : rows) {
            List<String> values = headers.stream()
                    .map(header -> "occurredAt".equals(header)
                            ? stringValue(row.occurredAt())
                            : stringValue(row.fields().get(header)))
                    .toList();
            csv.append(toCsvLine(values)).append('\n');
        }
        String filename = reportDefinition.code().toLowerCase(Locale.ROOT) + "-report.csv";
        byte[] content = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        return new ReportExportFile(filename, "text/csv; charset=UTF-8", content);
    }

    private SnapshotHolder loadSnapshot(ReportDefinition reportDefinition) {
        reportRefreshApplicationService.markStaleIfNecessary(reportDefinition);
        ReportDefinition latestDefinition = getByCode(reportDefinition.code());
        ReportSnapshot snapshot = reportSnapshotCache.findReadySnapshotByCode(latestDefinition.code())
                .or(() -> reportSnapshotRepository.findLatestReadyByReportId(latestDefinition.id()))
                .orElseGet(() -> reportRefreshApplicationService.refreshByCode(
                        latestDefinition.code(),
                        ReportRefreshTriggerMode.ON_DEMAND,
                        "query-bootstrap",
                        null
                ));
        reportSnapshotCache.put(latestDefinition.code(), snapshot);
        ReportFreshnessStatus freshnessStatus = latestDefinition.lastFreshnessStatus() == null
                ? snapshot.freshnessStatus()
                : latestDefinition.lastFreshnessStatus();
        return new SnapshotHolder(snapshot, freshnessStatus);
    }

    private ReportDefinition getByCode(String code) {
        return reportDefinitionRepository.findByCode(code)
                .orElseThrow(() -> new BizException(
                        ReportErrorDescriptors.REPORT_NOT_FOUND,
                        "report definition not found: " + code
                ));
    }

    private String resolveMetricCode(ReportDefinition reportDefinition, String requestedMetricCode, boolean trend) {
        if (requestedMetricCode != null && !requestedMetricCode.isBlank()) {
            return requestedMetricCode;
        }
        return reportDefinition.metrics().stream()
                .filter(metric -> trend ? metric.trendEnabled() : metric.rankEnabled())
                .findFirst()
                .map(ReportMetricDefinition::metricCode)
                .orElse(reportDefinition.metrics().get(0).metricCode());
    }

    private String resolveRankingDimensionCode(ReportDefinition reportDefinition, String requestedDimensionCode) {
        if (requestedDimensionCode != null && !requestedDimensionCode.isBlank()) {
            return requestedDimensionCode;
        }
        if (reportDefinition.cardProtocol() != null
                && reportDefinition.cardProtocol().rankDimensionCode() != null
                && !reportDefinition.cardProtocol().rankDimensionCode().isBlank()) {
            return reportDefinition.cardProtocol().rankDimensionCode();
        }
        return reportDefinition.dimensions().stream()
                .filter(dimension -> !dimension.timeDimension())
                .findFirst()
                .or(() -> reportDefinition.dimensions().stream().findFirst())
                .map(dimension -> dimension.dimensionCode())
                .orElse(null);
    }

    private List<String> exportHeaders(List<ReportDataRecord> rows) {
        TreeSet<String> fieldNames = new TreeSet<>();
        for (ReportDataRecord row : rows) {
            for (Map.Entry<String, Object> entry : row.fields().entrySet()) {
                fieldNames.add(entry.getKey());
            }
        }
        return java.util.stream.Stream.concat(java.util.stream.Stream.of("occurredAt"), fieldNames.stream()).toList();
    }

    private String toCsvLine(List<String> values) {
        return values.stream().map(this::escapeCsv).collect(java.util.stream.Collectors.joining(","));
    }

    private String escapeCsv(String value) {
        String normalized = value == null ? "" : value;
        boolean requiresQuotes = normalized.contains(",")
                || normalized.contains("\"")
                || normalized.contains("\n")
                || normalized.contains("\r");
        String escaped = normalized.replace("\"", "\"\"");
        return requiresQuotes ? "\"" + escaped + "\"" : escaped;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record SnapshotHolder(
            ReportSnapshot snapshot,
            ReportFreshnessStatus freshnessStatus
    ) {
    }
}
