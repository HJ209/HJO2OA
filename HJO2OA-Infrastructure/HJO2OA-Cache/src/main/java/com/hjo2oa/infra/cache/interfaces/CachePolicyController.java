package com.hjo2oa.infra.cache.interfaces;

import com.hjo2oa.infra.cache.application.CacheErrorDescriptors;
import com.hjo2oa.infra.cache.application.CachePolicyApplicationService;
import com.hjo2oa.shared.kernel.BizException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/infra/cache/policies")
public class CachePolicyController {

    private final CachePolicyApplicationService applicationService;
    private final CachePolicyDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public CachePolicyController(
            CachePolicyApplicationService applicationService,
            CachePolicyDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping
    public ApiResponse<CachePolicyDtos.PolicyResponse> create(
            @Valid @RequestBody CachePolicyDtos.CreateRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toPolicyResponse(applicationService.createPolicy(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{policyId}")
    public ApiResponse<CachePolicyDtos.PolicyResponse> update(
            @PathVariable UUID policyId,
            @Valid @RequestBody CachePolicyDtos.UpdateRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toPolicyResponse(applicationService.updatePolicy(body.toCommand(policyId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{policyId}/deactivate")
    public ApiResponse<CachePolicyDtos.PolicyResponse> deactivate(
            @PathVariable UUID policyId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toPolicyResponse(applicationService.deactivatePolicy(policyId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{policyId}/refresh")
    public ApiResponse<CachePolicyDtos.InvalidationResponse> refreshPolicy(
            @PathVariable UUID policyId,
            @RequestParam(required = false) String reasonRef,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toInvalidationResponse(applicationService.refreshPolicy(policyId, reasonRef)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/invalidate")
    public ApiResponse<CachePolicyDtos.InvalidationResponse> invalidate(
            @Valid @RequestBody CachePolicyDtos.InvalidateRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toInvalidationResponse(applicationService.invalidateKey(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/namespace/{namespace}")
    public ApiResponse<CachePolicyDtos.PolicyResponse> queryByNamespace(
            @PathVariable String namespace,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toPolicyResponse(applicationService.queryByNamespace(namespace)
                        .orElseThrow(() -> new BizException(
                                CacheErrorDescriptors.POLICY_NOT_FOUND,
                                "Cache policy not found"
                        ))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping
    public ApiResponse<List<CachePolicyDtos.PolicyResponse>> list(HttpServletRequest request) {
        return ApiResponse.success(
                dtoMapper.toPolicyResponses(applicationService.list()),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/keys")
    public ApiResponse<List<CachePolicyDtos.RuntimeKeyResponse>> queryKeys(
            @RequestParam(required = false) String namespace,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.queryKeys(namespace, tenantId, keyword).stream()
                        .map(dtoMapper::toRuntimeKeyResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/metrics")
    public ApiResponse<List<CachePolicyDtos.RuntimeMetricsResponse>> metrics(HttpServletRequest request) {
        return ApiResponse.success(
                applicationService.metrics().stream()
                        .map(dtoMapper::toRuntimeMetricsResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/metrics/{namespace}")
    public ApiResponse<CachePolicyDtos.RuntimeMetricsResponse> metricsByNamespace(
            @PathVariable String namespace,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toRuntimeMetricsResponse(applicationService.metrics(namespace)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/namespaces/{namespace}/clear")
    public ApiResponse<CachePolicyDtos.InvalidationResponse> clearNamespace(
            @PathVariable String namespace,
            @RequestBody(required = false) CachePolicyDtos.ClearNamespaceRequest body,
            HttpServletRequest request
    ) {
        String reasonRef = body == null ? null : body.reasonRef();
        return ApiResponse.success(
                dtoMapper.toInvalidationResponse(applicationService.clearNamespace(namespace, reasonRef)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/invalidations")
    public ApiResponse<List<CachePolicyDtos.InvalidationResponse>> listInvalidations(
            @RequestParam(required = false) String namespace,
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listInvalidations(namespace, limit).stream()
                        .map(dtoMapper::toInvalidationResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }
}
