package com.hjo2oa.infra.config.interfaces;

import com.hjo2oa.infra.config.application.ConfigEntryApplicationService;
import com.hjo2oa.infra.config.application.ConfigEntryCommands;
import com.hjo2oa.infra.config.domain.ConfigEntryView;
import com.hjo2oa.infra.config.domain.ConfigStatus;
import com.hjo2oa.infra.config.domain.ConfigType;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/infra")
public class ConfigEntryController {

    private final ConfigEntryApplicationService applicationService;
    private final ConfigEntryDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public ConfigEntryController(
            ConfigEntryApplicationService applicationService,
            ConfigEntryDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping({"/configs", "/config-entries", "/feature-flags"})
    public ApiResponse<ConfigEntryDtos.ConfigEntryResponse> create(
            @Valid @RequestBody ConfigEntryDtos.CreateRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.createEntry(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping({"/configs/{entryId}/disable", "/config-entries/{entryId}/disable"})
    public ApiResponse<ConfigEntryDtos.ConfigEntryResponse> disable(
            @PathVariable UUID entryId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.disableEntry(entryId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping({"/configs/{entryId}/default", "/config-entries/{entryId}"})
    public ApiResponse<ConfigEntryDtos.ConfigEntryResponse> updateDefault(
            @PathVariable UUID entryId,
            @Valid @RequestBody ConfigEntryDtos.UpdateDefaultRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.updateDefault(entryId, body.defaultValue())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping({"/configs/{entryId}/overrides", "/config-entries/{entryId}/overrides"})
    public ApiResponse<ConfigEntryDtos.ConfigEntryResponse> addOverride(
            @PathVariable UUID entryId,
            @Valid @RequestBody ConfigEntryDtos.AddOverrideRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.addOverride(body.toCommand(entryId))),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping({"/configs/{entryId}/overrides/{overrideId}", "/config-entries/{entryId}/overrides/{overrideId}"})
    public ApiResponse<ConfigEntryDtos.ConfigEntryResponse> removeOverride(
            @PathVariable UUID entryId,
            @PathVariable UUID overrideId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.removeOverride(entryId, overrideId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/config-overrides/{overrideId}/disable")
    public ApiResponse<ConfigEntryDtos.ConfigEntryResponse> disableOverride(
            @PathVariable UUID overrideId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.disableOverride(overrideId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping({"/configs/{entryId}/feature-rules", "/feature-flags/{entryId}/rules"})
    public ApiResponse<ConfigEntryDtos.ConfigEntryResponse> addFeatureRule(
            @PathVariable UUID entryId,
            @Valid @RequestBody ConfigEntryDtos.AddFeatureRuleRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.addFeatureRule(body.toCommand(entryId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/feature-rules/{ruleId}")
    public ApiResponse<ConfigEntryDtos.ConfigEntryResponse> updateFeatureRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody ConfigEntryDtos.UpdateFeatureRuleRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.updateFeatureRule(
                        ruleId,
                        body.ruleType(),
                        body.ruleValue(),
                        body.sortOrder(),
                        body.active()
                )),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping({"/configs/key/{key}", "/config-entries/key/{key}"})
    public ApiResponse<ConfigEntryDtos.ConfigEntryResponse> queryByKey(
            @PathVariable String key,
            HttpServletRequest request
    ) {
        ConfigEntryView entryView = applicationService.queryByKey(key)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Config entry not found"
                ));
        return ApiResponse.success(dtoMapper.toResponse(entryView), responseMetaFactory.create(request));
    }

    @PostMapping({"/configs/resolve", "/config-resolution/preview"})
    public ApiResponse<ConfigEntryDtos.ResolvedConfigValueResponse> resolve(
            @Valid @RequestBody ConfigEntryDtos.ResolveValueRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.resolveValue(body.toQuery())),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/config-resolution")
    public ApiResponse<ConfigEntryDtos.ResolvedConfigValueResponse> resolveByQuery(
            @RequestParam String key,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false, name = "organizationId") UUID organizationId,
            @RequestParam(required = false, name = "orgId") UUID orgId,
            @RequestParam(required = false) UUID roleId,
            @RequestParam(required = false) UUID userId,
            HttpServletRequest request
    ) {
        UUID effectiveOrgId = organizationId == null ? orgId : organizationId;
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.resolveValue(key, tenantId, effectiveOrgId, roleId, userId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping({"/configs", "/config-entries"})
    public ApiResponse<List<ConfigEntryDtos.ConfigEntryResponse>> list(
            @RequestParam(required = false) String configKey,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ConfigType configType,
            @RequestParam(required = false) ConfigStatus status,
            @RequestParam(required = false) Boolean tenantAware,
            @RequestParam(required = false) Boolean mutableAtRuntime,
            HttpServletRequest request
    ) {
        List<ConfigEntryDtos.ConfigEntryResponse> data = applicationService.list(new ConfigEntryCommands.ListQuery(
                        configKey,
                        keyword,
                        configType,
                        status,
                        tenantAware,
                        mutableAtRuntime
                )).stream()
                .map(dtoMapper::toResponse)
                .toList();
        return ApiResponse.success(data, responseMetaFactory.create(request));
    }

    @GetMapping("/feature-flags")
    public ApiResponse<List<ConfigEntryDtos.ConfigEntryResponse>> listFeatureFlags(HttpServletRequest request) {
        List<ConfigEntryDtos.ConfigEntryResponse> data = applicationService.list(new ConfigEntryCommands.ListQuery(
                        null,
                        null,
                        ConfigType.FEATURE_FLAG,
                        null,
                        null,
                        null
                )).stream()
                .map(dtoMapper::toResponse)
                .toList();
        return ApiResponse.success(data, responseMetaFactory.create(request));
    }
}
