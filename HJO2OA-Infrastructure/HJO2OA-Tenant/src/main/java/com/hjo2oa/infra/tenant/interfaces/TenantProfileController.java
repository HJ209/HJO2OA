package com.hjo2oa.infra.tenant.interfaces;

import com.hjo2oa.infra.tenant.application.TenantProfileApplicationService;
import com.hjo2oa.infra.tenant.domain.QuotaType;
import com.hjo2oa.infra.tenant.domain.TenantProfileView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/infra/tenants")
public class TenantProfileController {

    private final TenantProfileApplicationService applicationService;
    private final TenantProfileDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public TenantProfileController(
            TenantProfileApplicationService applicationService,
            TenantProfileDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping
    public ApiResponse<TenantProfileDtos.TenantProfileResponse> create(
            @Valid @RequestBody TenantProfileDtos.CreateTenantRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.createTenant(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{tenantId}/activate")
    public ApiResponse<TenantProfileDtos.TenantProfileResponse> activate(
            @PathVariable UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.activateTenant(tenantId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{tenantId}/activate")
    public ApiResponse<TenantProfileDtos.TenantProfileResponse> activateByAction(
            @PathVariable UUID tenantId,
            HttpServletRequest request
    ) {
        return activate(tenantId, request);
    }

    @PutMapping("/{tenantId}")
    public ApiResponse<TenantProfileDtos.TenantProfileResponse> update(
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantProfileDtos.UpdateTenantRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.updateTenant(body.toCommand(tenantId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{tenantId}/initialize")
    public ApiResponse<TenantProfileDtos.TenantProfileResponse> initialize(
            @PathVariable UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.initializeTenant(tenantId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{tenantId}/initialize")
    public ApiResponse<TenantProfileDtos.TenantProfileResponse> initializeByAction(
            @PathVariable UUID tenantId,
            HttpServletRequest request
    ) {
        return initialize(tenantId, request);
    }

    @PutMapping("/{tenantId}/suspend")
    public ApiResponse<TenantProfileDtos.TenantProfileResponse> suspend(
            @PathVariable UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.suspendTenant(tenantId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{tenantId}/disable")
    public ApiResponse<TenantProfileDtos.TenantProfileResponse> disable(
            @PathVariable UUID tenantId,
            HttpServletRequest request
    ) {
        return suspend(tenantId, request);
    }

    @PutMapping("/{tenantId}/archive")
    public ApiResponse<TenantProfileDtos.TenantProfileResponse> archive(
            @PathVariable UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.archiveTenant(tenantId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{tenantId}")
    public ApiResponse<TenantProfileDtos.TenantProfileDetailResponse> current(
            @PathVariable UUID tenantId,
            HttpServletRequest request
    ) {
        TenantProfileView tenantProfile = applicationService.current(tenantId).orElseThrow(() -> new BizException(
                SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                "Tenant profile not found"
        ));
        return ApiResponse.success(
                dtoMapper.toDetailResponse(tenantProfile, applicationService.listQuotas(tenantId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping
    public ApiResponse<List<TenantProfileDtos.TenantProfileResponse>> list(HttpServletRequest request) {
        return ApiResponse.success(
                applicationService.listAll().stream().map(dtoMapper::toResponse).toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{tenantId}/quotas")
    public ApiResponse<List<TenantProfileDtos.TenantQuotaResponse>> listQuotas(
            @PathVariable UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listQuotas(tenantId).stream().map(dtoMapper::toResponse).toList(),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{tenantId}/quotas/{quotaType}")
    public ApiResponse<TenantProfileDtos.TenantQuotaResponse> updateQuota(
            @PathVariable UUID tenantId,
            @PathVariable QuotaType quotaType,
            @Valid @RequestBody TenantProfileDtos.UpdateQuotaRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.updateQuota(body.toCommand(tenantId, quotaType))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{tenantId}/quota-usages/{quotaType}")
    public ApiResponse<TenantProfileDtos.TenantQuotaResponse> consumeQuota(
            @PathVariable UUID tenantId,
            @PathVariable QuotaType quotaType,
            @Valid @RequestBody TenantProfileDtos.ConsumeQuotaRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.consumeQuota(body.toCommand(tenantId, quotaType))),
                responseMetaFactory.create(request)
        );
    }
}
