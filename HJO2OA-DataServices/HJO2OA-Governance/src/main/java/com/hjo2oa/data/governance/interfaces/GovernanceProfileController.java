package com.hjo2oa.data.governance.interfaces;

import com.hjo2oa.data.governance.application.GovernanceCommands.GovernancePagedResult;
import com.hjo2oa.data.governance.application.GovernanceProfileApplicationService;
import com.hjo2oa.data.governance.domain.AlertRule;
import com.hjo2oa.data.governance.domain.GovernanceProfile;
import com.hjo2oa.data.governance.domain.HealthCheckRule;
import com.hjo2oa.data.governance.domain.GovernanceTypes.AlertRuleStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceProfileStatus;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.HealthCheckRuleStatus;
import com.hjo2oa.data.governance.interfaces.GovernanceRequests.UpsertAlertRuleRequest;
import com.hjo2oa.data.governance.interfaces.GovernanceRequests.UpsertGovernanceProfileRequest;
import com.hjo2oa.data.governance.interfaces.GovernanceRequests.UpsertHealthCheckRuleRequest;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.Pagination;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/data/governance/profiles")
public class GovernanceProfileController {

    private final GovernanceProfileApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public GovernanceProfileController(
            GovernanceProfileApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping
    public ApiResponse<com.hjo2oa.shared.web.PageData<GovernanceProfile>> listProfiles(
            @RequestParam String tenantId,
            @RequestParam(required = false) GovernanceScopeType scopeType,
            @RequestParam(required = false) GovernanceProfileStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        List<GovernanceProfile> items = applicationService.listProfiles(tenantId, scopeType, status);
        return ApiResponse.page(page(items, page, size), Pagination.of(page, size, items.size()), responseMetaFactory.create(request));
    }

    @PostMapping
    public ApiResponse<GovernanceProfile> upsertProfile(
            @Valid @RequestBody UpsertGovernanceProfileRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.upsertProfile(body.toCommand()), responseMetaFactory.create(request));
    }

    @GetMapping("/{profileCode}/health-rules")
    public ApiResponse<com.hjo2oa.shared.web.PageData<HealthCheckRule>> listHealthRules(
            @PathVariable String profileCode,
            @RequestParam String tenantId,
            @RequestParam(required = false) HealthCheckRuleStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        List<HealthCheckRule> items = applicationService.listHealthCheckRules(tenantId, profileCode, status);
        return ApiResponse.page(page(items, page, size), Pagination.of(page, size, items.size()), responseMetaFactory.create(request));
    }

    @PostMapping("/{profileCode}/health-rules")
    public ApiResponse<HealthCheckRule> upsertHealthRule(
            @PathVariable String profileCode,
            @Valid @RequestBody UpsertHealthCheckRuleRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.upsertHealthCheckRule(body.toCommand(profileCode)), responseMetaFactory.create(request));
    }

    @GetMapping("/{profileCode}/alert-rules")
    public ApiResponse<com.hjo2oa.shared.web.PageData<AlertRule>> listAlertRules(
            @PathVariable String profileCode,
            @RequestParam String tenantId,
            @RequestParam(required = false) AlertRuleStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        List<AlertRule> items = applicationService.listAlertRules(tenantId, profileCode, status);
        return ApiResponse.page(page(items, page, size), Pagination.of(page, size, items.size()), responseMetaFactory.create(request));
    }

    @PostMapping("/{profileCode}/alert-rules")
    public ApiResponse<AlertRule> upsertAlertRule(
            @PathVariable String profileCode,
            @Valid @RequestBody UpsertAlertRuleRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.upsertAlertRule(body.toCommand(profileCode)), responseMetaFactory.create(request));
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
