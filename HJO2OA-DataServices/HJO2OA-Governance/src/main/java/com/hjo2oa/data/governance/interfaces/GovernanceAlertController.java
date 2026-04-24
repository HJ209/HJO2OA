package com.hjo2oa.data.governance.interfaces;

import com.hjo2oa.data.governance.application.GovernanceMonitoringApplicationService;
import com.hjo2oa.data.governance.domain.GovernanceAlertRecord;
import com.hjo2oa.data.governance.domain.GovernanceQueries.AlertQuery;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertLevel;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceActionType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertStatus;
import com.hjo2oa.data.governance.interfaces.GovernanceRequests.AlertActionRequest;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.Pagination;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/data/governance/alerts")
public class GovernanceAlertController {

    private final GovernanceMonitoringApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public GovernanceAlertController(
            GovernanceMonitoringApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping
    public ApiResponse<com.hjo2oa.shared.web.PageData<GovernanceAlertRecord>> list(
            @RequestParam(required = false) GovernanceScopeType targetType,
            @RequestParam(required = false) String targetCode,
            @RequestParam(required = false) AlertLevel alertLevel,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) Instant occurredFrom,
            @RequestParam(required = false) Instant occurredTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        List<GovernanceAlertRecord> items = applicationService.listAlerts(new AlertQuery(
                targetType,
                targetCode,
                alertLevel,
                status,
                occurredFrom,
                occurredTo
        )).items();
        return ApiResponse.page(page(items, page, size), Pagination.of(page, size, items.size()), responseMetaFactory.create(request));
    }

    @GetMapping("/{alertId}")
    public ApiResponse<GovernanceAlertRecord> detail(@PathVariable String alertId, HttpServletRequest request) {
        return ApiResponse.success(applicationService.getAlert(alertId), responseMetaFactory.create(request));
    }

    @PostMapping("/{alertId}/acknowledge")
    public ApiResponse<GovernanceAlertRecord> acknowledge(
            @PathVariable String alertId,
            @Valid @RequestBody AlertActionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.handleAlertAction(body.toCommand(alertId, GovernanceActionType.ACKNOWLEDGE_ALERT)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{alertId}/escalate")
    public ApiResponse<GovernanceAlertRecord> escalate(
            @PathVariable String alertId,
            @Valid @RequestBody AlertActionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.handleAlertAction(body.toCommand(alertId, GovernanceActionType.ESCALATE_ALERT)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{alertId}/close")
    public ApiResponse<GovernanceAlertRecord> close(
            @PathVariable String alertId,
            @Valid @RequestBody AlertActionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.handleAlertAction(body.toCommand(alertId, GovernanceActionType.CLOSE_ALERT)),
                responseMetaFactory.create(request)
        );
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
