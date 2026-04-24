package com.hjo2oa.data.governance.interfaces;

import com.hjo2oa.data.governance.application.GovernanceCommands.VersionListQuery;
import com.hjo2oa.data.governance.application.GovernanceProfileApplicationService;
import com.hjo2oa.data.governance.domain.ServiceVersionRecord;
import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.ServiceVersionStatus;
import com.hjo2oa.data.governance.interfaces.GovernanceRequests.DeprecateServiceVersionRequest;
import com.hjo2oa.data.governance.interfaces.GovernanceRequests.PublishServiceVersionRequest;
import com.hjo2oa.data.governance.interfaces.GovernanceRequests.RegisterServiceVersionRequest;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.Pagination;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/data/governance/versions")
public class GovernanceVersionController {

    private final GovernanceProfileApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public GovernanceVersionController(
            GovernanceProfileApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping
    public ApiResponse<com.hjo2oa.shared.web.PageData<ServiceVersionRecord>> list(
            @RequestParam String tenantId,
            @RequestParam(required = false) GovernanceScopeType targetType,
            @RequestParam(required = false) String targetCode,
            @RequestParam(required = false) ServiceVersionStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        List<ServiceVersionRecord> items = applicationService.listVersionRecords(new VersionListQuery(
                tenantId,
                targetType,
                targetCode,
                status
        )).items();
        return ApiResponse.page(page(items, page, size), Pagination.of(page, size, items.size()), responseMetaFactory.create(request));
    }

    @PostMapping("/register")
    public ApiResponse<ServiceVersionRecord> register(
            @Valid @RequestBody RegisterServiceVersionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.registerVersion(body.toCommand()), responseMetaFactory.create(request));
    }

    @PostMapping("/publish")
    public ApiResponse<ServiceVersionRecord> publish(
            @Valid @RequestBody PublishServiceVersionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.publishVersion(body.toCommand()), responseMetaFactory.create(request));
    }

    @PostMapping("/deprecate")
    public ApiResponse<ServiceVersionRecord> deprecate(
            @Valid @RequestBody DeprecateServiceVersionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.deprecateVersion(body.toCommand()), responseMetaFactory.create(request));
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
