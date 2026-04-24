package com.hjo2oa.data.governance.interfaces;

import com.hjo2oa.data.governance.application.GovernanceMonitoringApplicationService;
import com.hjo2oa.data.governance.domain.GovernanceActionAuditRecord;
import com.hjo2oa.data.governance.domain.GovernanceHealthSnapshot;
import com.hjo2oa.data.governance.domain.GovernanceQueries.AuditQuery;
import com.hjo2oa.data.governance.domain.GovernanceQueries.HealthSnapshotQuery;
import com.hjo2oa.data.governance.domain.GovernanceQueries.TraceQuery;
import com.hjo2oa.data.governance.domain.GovernanceTraceRecord;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.TraceStatus;
import com.hjo2oa.data.governance.interfaces.GovernanceRequests.ManualGovernanceInterventionRequest;
import com.hjo2oa.data.governance.interfaces.GovernanceRequests.RunHealthCheckRequest;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.Pagination;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/data/governance")
public class GovernanceOperationsController {

    private final GovernanceMonitoringApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public GovernanceOperationsController(
            GovernanceMonitoringApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/health-checks/run")
    public ApiResponse<com.hjo2oa.shared.web.PageData<GovernanceHealthSnapshot>> runHealthChecks(
            @Valid @RequestBody RunHealthCheckRequest body,
            HttpServletRequest request
    ) {
        List<GovernanceHealthSnapshot> items = applicationService.runHealthChecks(body.toCommand()).items();
        return ApiResponse.page(items, Pagination.of(1, Math.max(1, items.size()), items.size()), responseMetaFactory.create(request));
    }

    @GetMapping("/health-snapshots")
    public ApiResponse<com.hjo2oa.shared.web.PageData<GovernanceHealthSnapshot>> listSnapshots(
            @RequestParam(required = false) GovernanceScopeType targetType,
            @RequestParam(required = false) String targetCode,
            @RequestParam(required = false) String ruleCode,
            @RequestParam(required = false) Instant checkedFrom,
            @RequestParam(required = false) Instant checkedTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        List<GovernanceHealthSnapshot> items = applicationService.listSnapshots(new HealthSnapshotQuery(
                targetType,
                targetCode,
                ruleCode,
                checkedFrom,
                checkedTo
        )).items();
        return ApiResponse.page(page(items, page, size), Pagination.of(page, size, items.size()), responseMetaFactory.create(request));
    }

    @GetMapping("/traces")
    public ApiResponse<com.hjo2oa.shared.web.PageData<GovernanceTraceRecord>> listTraces(
            @RequestParam(required = false) GovernanceScopeType targetType,
            @RequestParam(required = false) String targetCode,
            @RequestParam(required = false) TraceStatus status,
            @RequestParam(required = false) Instant openedFrom,
            @RequestParam(required = false) Instant openedTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        List<GovernanceTraceRecord> items = applicationService.listTraces(new TraceQuery(
                targetType,
                targetCode,
                status,
                openedFrom,
                openedTo
        )).items();
        return ApiResponse.page(page(items, page, size), Pagination.of(page, size, items.size()), responseMetaFactory.create(request));
    }

    @GetMapping("/audits")
    public ApiResponse<com.hjo2oa.shared.web.PageData<GovernanceActionAuditRecord>> listAudits(
            @RequestParam(required = false) GovernanceScopeType targetType,
            @RequestParam(required = false) String targetCode,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        List<GovernanceActionAuditRecord> items = applicationService.listAudits(new AuditQuery(
                targetType,
                targetCode,
                createdFrom,
                createdTo
        )).items();
        return ApiResponse.page(page(items, page, size), Pagination.of(page, size, items.size()), responseMetaFactory.create(request));
    }

    @PostMapping("/interventions")
    public ApiResponse<GovernanceActionAuditRecord> intervene(
            @Valid @RequestBody ManualGovernanceInterventionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.submitIntervention(body.toCommand()), responseMetaFactory.create(request));
    }

    private <T> List<T> page(List<T> items, int page, int size) {
        int fromIndex = Math.max(0, (page - 1) * size);
        int toIndex = Math.min(items.size(), fromIndex + size);
        if (fromIndex >= items.size()) {
            return List.of();
        }
        return items.subList(fromIndex, toIndex);
    }
}
