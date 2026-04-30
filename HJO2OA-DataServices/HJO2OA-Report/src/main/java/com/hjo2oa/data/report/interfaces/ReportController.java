package com.hjo2oa.data.report.interfaces;

import com.hjo2oa.data.report.application.ReportDefinitionApplicationService;
import com.hjo2oa.data.report.application.ReportQueryApplicationService;
import com.hjo2oa.data.report.application.ReportRefreshApplicationService;
import com.hjo2oa.data.report.application.SaveReportDefinitionCommand;
import com.hjo2oa.data.report.domain.ReportAnalysisQuery;
import com.hjo2oa.data.report.domain.ReportCaliberDefinition;
import com.hjo2oa.data.report.domain.ReportCardProtocol;
import com.hjo2oa.data.report.domain.ReportDefinitionPage;
import com.hjo2oa.data.report.domain.ReportDefinitionQuery;
import com.hjo2oa.data.report.domain.ReportDimensionDefinition;
import com.hjo2oa.data.report.domain.ReportMetricDefinition;
import com.hjo2oa.data.report.domain.ReportRefreshConfig;
import com.hjo2oa.data.report.domain.ReportRefreshTriggerMode;
import com.hjo2oa.data.report.domain.ReportSnapshotPage;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.Pagination;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/data/report")
public class ReportController {

    private static final List<String> RESERVED_QUERY_KEYS = List.of(
            "from",
            "to",
            "dimensionCode",
            "metricCode",
            "topN",
            "page",
            "size",
            "reportType",
            "sourceScope",
            "status",
            "visibilityMode"
    );

    private final ReportDefinitionApplicationService reportDefinitionApplicationService;
    private final ReportRefreshApplicationService reportRefreshApplicationService;
    private final ReportQueryApplicationService reportQueryApplicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public ReportController(
            ReportDefinitionApplicationService reportDefinitionApplicationService,
            ReportRefreshApplicationService reportRefreshApplicationService,
            ReportQueryApplicationService reportQueryApplicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.reportDefinitionApplicationService = Objects.requireNonNull(
                reportDefinitionApplicationService,
                "reportDefinitionApplicationService must not be null");
        this.reportRefreshApplicationService = Objects.requireNonNull(
                reportRefreshApplicationService,
                "reportRefreshApplicationService must not be null");
        this.reportQueryApplicationService = Objects.requireNonNull(
                reportQueryApplicationService,
                "reportQueryApplicationService must not be null");
        this.responseMetaFactory = Objects.requireNonNull(responseMetaFactory, "responseMetaFactory must not be null");
    }

    @GetMapping("/definitions")
    public ApiResponse<?> definitions(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "reportType", required = false) com.hjo2oa.data.report.domain.ReportType reportType,
            @RequestParam(name = "sourceScope", required = false) com.hjo2oa.data.report.domain.ReportSourceScope sourceScope,
            @RequestParam(name = "status", required = false) com.hjo2oa.data.report.domain.ReportStatus status,
            @RequestParam(name = "visibilityMode", required = false)
            com.hjo2oa.data.report.domain.ReportVisibilityMode visibilityMode,
            HttpServletRequest request
    ) {
        ReportDefinitionPage result = reportDefinitionApplicationService.page(
                new ReportDefinitionQuery(page, size, reportType, sourceScope, status, visibilityMode)
        );
        List<ReportDefinitionView> items = result.items().stream().map(ReportDefinitionView::from).toList();
        return ApiResponse.page(items, Pagination.of(page, size, result.total()), responseMetaFactory.create(request));
    }

    @GetMapping("/definitions/{code}")
    public ApiResponse<ReportDefinitionView> definition(
            @PathVariable String code,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                ReportDefinitionView.from(reportDefinitionApplicationService.getByCode(code)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/definitions")
    public ApiResponse<ReportDefinitionView> create(
            @Valid @RequestBody SaveReportDefinitionRequest requestBody,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                ReportDefinitionView.from(reportDefinitionApplicationService.create(toCommand(requestBody))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/definitions/{code}")
    public ApiResponse<ReportDefinitionView> update(
            @PathVariable String code,
            @Valid @RequestBody SaveReportDefinitionRequest requestBody,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                ReportDefinitionView.from(reportDefinitionApplicationService.update(code, toCommand(requestBody))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/definitions/{code}/status")
    public ApiResponse<ReportDefinitionView> updateStatus(
            @PathVariable String code,
            @Valid @RequestBody UpdateReportStatusRequest requestBody,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                ReportDefinitionView.from(reportDefinitionApplicationService.changeStatus(code, requestBody.status())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/definitions/{code}/refresh")
    public ApiResponse<ReportSnapshotView> refresh(
            @PathVariable String code,
            @RequestBody(required = false) RefreshReportRequest requestBody,
            HttpServletRequest request
    ) {
        RefreshReportRequest resolvedRequest = requestBody == null ? new RefreshReportRequest(null, null) : requestBody;
        return ApiResponse.success(
                ReportSnapshotView.from(reportRefreshApplicationService.refreshByCode(
                        code,
                        ReportRefreshTriggerMode.MANUAL,
                        resolvedRequest.reason(),
                        resolvedRequest.batchId()
                )),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/definitions/{code}/summary")
    public ApiResponse<?> summary(
            @PathVariable String code,
            @RequestParam Map<String, String> queryParams,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                reportQueryApplicationService.summary(code, toAnalysisQuery(queryParams)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/definitions/{code}/trend")
    public ApiResponse<?> trend(
            @PathVariable String code,
            @RequestParam Map<String, String> queryParams,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                reportQueryApplicationService.trend(code, toAnalysisQuery(queryParams)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/definitions/{code}/ranking")
    public ApiResponse<?> ranking(
            @PathVariable String code,
            @RequestParam Map<String, String> queryParams,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                reportQueryApplicationService.ranking(code, toAnalysisQuery(queryParams)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/cards/{code}")
    public ApiResponse<?> cardDataSource(
            @PathVariable String code,
            @RequestParam Map<String, String> queryParams,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                reportQueryApplicationService.cardDataSource(code, toAnalysisQuery(queryParams)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/definitions/{code}/export")
    public ResponseEntity<byte[]> exportCsv(
            @PathVariable String code,
            @RequestParam Map<String, String> queryParams
    ) {
        var exportFile = reportQueryApplicationService.exportCsv(code, toAnalysisQuery(queryParams));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, exportFile.contentType())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + exportFile.filename().replace("\"", "") + "\""
                )
                .body(exportFile.content());
    }

    @GetMapping("/definitions/{code}/snapshots")
    public ApiResponse<?> snapshots(
            @PathVariable String code,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        ReportSnapshotPage snapshots = reportQueryApplicationService.snapshots(code, page, size);
        List<ReportSnapshotView> items = snapshots.items().stream().map(ReportSnapshotView::from).toList();
        return ApiResponse.page(items, Pagination.of(page, size, snapshots.total()), responseMetaFactory.create(request));
    }

    private SaveReportDefinitionCommand toCommand(SaveReportDefinitionRequest requestBody) {
        return new SaveReportDefinitionCommand(
                requestBody.code(),
                requestBody.name(),
                requestBody.reportType(),
                requestBody.sourceScope(),
                requestBody.refreshMode(),
                requestBody.visibilityMode(),
                requestBody.tenantId(),
                new ReportCaliberDefinition(
                        requestBody.caliber().sourceProviderKey(),
                        requestBody.caliber().subjectCode(),
                        requestBody.caliber().defaultTimeField(),
                        requestBody.caliber().organizationField(),
                        requestBody.caliber().dataServiceCode(),
                        requestBody.caliber().baseFilters(),
                        requestBody.caliber().triggerEventTypes(),
                        requestBody.caliber().description()
                ),
                requestBody.refreshConfig() == null
                        ? null
                        : new ReportRefreshConfig(
                        requestBody.refreshConfig().refreshIntervalSeconds(),
                        requestBody.refreshConfig().staleAfterSeconds(),
                        requestBody.refreshConfig().maxRows()
                ),
                requestBody.cardProtocol() == null
                        ? null
                        : new ReportCardProtocol(
                        requestBody.cardProtocol().cardCode(),
                        requestBody.cardProtocol().title(),
                        requestBody.cardProtocol().cardType(),
                        requestBody.cardProtocol().summaryMetricCode(),
                        requestBody.cardProtocol().trendMetricCode(),
                        requestBody.cardProtocol().rankMetricCode(),
                        requestBody.cardProtocol().rankDimensionCode(),
                        requestBody.cardProtocol().maxItems()
                ),
                requestBody.metrics().stream()
                        .map(metric -> new ReportMetricDefinition(
                                metric.id(),
                                metric.metricCode(),
                                metric.metricName(),
                                metric.aggregationType(),
                                metric.sourceField(),
                                metric.formula(),
                                metric.filterExpression(),
                                metric.unit(),
                                metric.trendEnabled(),
                                metric.rankEnabled(),
                                metric.displayOrder()
                        ))
                        .toList(),
                requestBody.dimensions() == null
                        ? List.of()
                        : requestBody.dimensions().stream()
                        .map(dimension -> new ReportDimensionDefinition(
                                dimension.id(),
                                dimension.dimensionCode(),
                                dimension.dimensionName(),
                                dimension.dimensionType(),
                                dimension.sourceField(),
                                dimension.timeGranularity(),
                                dimension.filterable(),
                                dimension.displayOrder()
                        ))
                        .toList()
        );
    }

    private ReportAnalysisQuery toAnalysisQuery(Map<String, String> queryParams) {
        Map<String, String> filters = new LinkedHashMap<>(queryParams);
        Instant from = parseInstant(filters.remove("from"));
        Instant to = parseInstant(filters.remove("to"));
        String dimensionCode = filters.remove("dimensionCode");
        String metricCode = filters.remove("metricCode");
        Integer topN = parseInteger(filters.remove("topN"));
        RESERVED_QUERY_KEYS.forEach(filters::remove);
        return new ReportAnalysisQuery(from, to, dimensionCode, metricCode, topN, filters);
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value);
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value);
    }
}
