package com.hjo2oa.infra.security.interfaces;

import com.hjo2oa.infra.security.application.SecurityPolicyApplicationService;
import com.hjo2oa.infra.security.domain.SecurityPolicyStatus;
import com.hjo2oa.infra.security.domain.SecurityPolicyType;
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
@RequestMapping("/api/v1/infra/security/policies")
public class SecurityPolicyController {

    private final SecurityPolicyApplicationService applicationService;
    private final SecurityPolicyDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public SecurityPolicyController(
            SecurityPolicyApplicationService applicationService,
            SecurityPolicyDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping
    public ApiResponse<SecurityPolicyDtos.SecurityPolicyResponse> create(
            @Valid @RequestBody SecurityPolicyDtos.CreatePolicyRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.createPolicy(
                        body.policyCode(),
                        body.policyType(),
                        body.name(),
                        body.configSnapshot(),
                        body.tenantId()
                )),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{policyId}/disable")
    public ApiResponse<SecurityPolicyDtos.SecurityPolicyResponse> disable(
            @PathVariable UUID policyId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.disablePolicy(policyId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{policyId}/config")
    public ApiResponse<SecurityPolicyDtos.SecurityPolicyResponse> updateConfig(
            @PathVariable UUID policyId,
            @Valid @RequestBody SecurityPolicyDtos.UpdateConfigRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.updateConfig(policyId, body.configSnapshot())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{policyId}/secret-keys")
    public ApiResponse<SecurityPolicyDtos.SecurityPolicyResponse> addSecretKey(
            @PathVariable UUID policyId,
            @Valid @RequestBody SecurityPolicyDtos.AddSecretKeyRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.addSecretKey(policyId, body.keyRef(), body.algorithm())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{policyId}/secret-keys/{keyId}/rotate")
    public ApiResponse<SecurityPolicyDtos.SecurityPolicyResponse> rotateKey(
            @PathVariable UUID policyId,
            @PathVariable UUID keyId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.rotateKey(policyId, keyId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{policyId}/masking-rules")
    public ApiResponse<SecurityPolicyDtos.SecurityPolicyResponse> addMaskingRule(
            @PathVariable UUID policyId,
            @Valid @RequestBody SecurityPolicyDtos.AddMaskingRuleRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.addMaskingRule(policyId, body.dataType(), body.ruleExpr())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{policyId}/rate-limit-rules")
    public ApiResponse<SecurityPolicyDtos.SecurityPolicyResponse> addRateLimitRule(
            @PathVariable UUID policyId,
            @Valid @RequestBody SecurityPolicyDtos.AddRateLimitRuleRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.addRateLimitRule(
                        policyId,
                        body.subjectType(),
                        body.windowSeconds(),
                        body.maxRequests()
                )),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping
    public ApiResponse<List<SecurityPolicyDtos.SecurityPolicyResponse>> list(
            @RequestParam(required = false) SecurityPolicyType policyType,
            @RequestParam(required = false) SecurityPolicyStatus status,
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listPolicies(policyType, status, tenantId).stream()
                        .map(dtoMapper::toResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/mask")
    public ApiResponse<SecurityPolicyDtos.MaskValueResponse> mask(
            @Valid @RequestBody SecurityPolicyDtos.MaskValueRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toMaskValueResponse(applicationService.maskValue(
                        body.policyCode(),
                        body.dataType(),
                        body.value()
                )),
                responseMetaFactory.create(request)
        );
    }
}
