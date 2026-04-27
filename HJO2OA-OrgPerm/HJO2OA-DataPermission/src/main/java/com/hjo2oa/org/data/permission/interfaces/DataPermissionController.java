package com.hjo2oa.org.data.permission.interfaces;

import com.hjo2oa.org.data.permission.application.DataPermissionApplicationService;
import com.hjo2oa.org.data.permission.domain.DataPermissionQuery;
import com.hjo2oa.org.data.permission.domain.DataScopeType;
import com.hjo2oa.org.data.permission.domain.FieldPermissionAction;
import com.hjo2oa.org.data.permission.domain.FieldPermissionQuery;
import com.hjo2oa.org.data.permission.domain.PermissionEffect;
import com.hjo2oa.org.data.permission.domain.PermissionSubjectType;
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
@RequestMapping("/api/org-perm/data-permissions")
public class DataPermissionController {

    private final DataPermissionApplicationService applicationService;
    private final DataPermissionDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public DataPermissionController(
            DataPermissionApplicationService applicationService,
            DataPermissionDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/row-policies")
    public ApiResponse<DataPermissionDtos.RowPolicyResponse> createRowPolicy(
            @Valid @RequestBody DataPermissionDtos.SaveRowPolicyRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toRowPolicyResponse(applicationService.createRowPolicy(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/row-policies/{policyId}")
    public ApiResponse<DataPermissionDtos.RowPolicyResponse> updateRowPolicy(
            @PathVariable UUID policyId,
            @Valid @RequestBody DataPermissionDtos.SaveRowPolicyRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toRowPolicyResponse(applicationService.updateRowPolicy(policyId, body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/row-policies/{policyId}")
    public ApiResponse<Void> deleteRowPolicy(@PathVariable UUID policyId, HttpServletRequest request) {
        applicationService.deleteRowPolicy(policyId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }

    @GetMapping("/row-policies/{policyId}")
    public ApiResponse<DataPermissionDtos.RowPolicyResponse> findRowPolicy(
            @PathVariable UUID policyId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.findRowPolicy(policyId)
                        .map(dtoMapper::toRowPolicyResponse)
                        .orElseThrow(() -> notFound("Row policy not found")),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/row-policies")
    public ApiResponse<List<DataPermissionDtos.RowPolicyResponse>> queryRowPolicies(
            @RequestParam(required = false) PermissionSubjectType subjectType,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) String businessObject,
            @RequestParam(required = false) DataScopeType scopeType,
            @RequestParam(required = false) PermissionEffect effect,
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.queryRowPolicies(new DataPermissionQuery(
                                subjectType,
                                subjectId,
                                businessObject,
                                scopeType,
                                effect,
                                tenantId
                        ))
                        .stream()
                        .map(dtoMapper::toRowPolicyResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/field-policies")
    public ApiResponse<DataPermissionDtos.FieldPolicyResponse> createFieldPolicy(
            @Valid @RequestBody DataPermissionDtos.SaveFieldPolicyRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toFieldPolicyResponse(applicationService.createFieldPolicy(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/field-policies/{policyId}")
    public ApiResponse<DataPermissionDtos.FieldPolicyResponse> updateFieldPolicy(
            @PathVariable UUID policyId,
            @Valid @RequestBody DataPermissionDtos.SaveFieldPolicyRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toFieldPolicyResponse(applicationService.updateFieldPolicy(policyId, body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/field-policies/{policyId}")
    public ApiResponse<Void> deleteFieldPolicy(@PathVariable UUID policyId, HttpServletRequest request) {
        applicationService.deleteFieldPolicy(policyId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }

    @GetMapping("/field-policies/{policyId}")
    public ApiResponse<DataPermissionDtos.FieldPolicyResponse> findFieldPolicy(
            @PathVariable UUID policyId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.findFieldPolicy(policyId)
                        .map(dtoMapper::toFieldPolicyResponse)
                        .orElseThrow(() -> notFound("Field policy not found")),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/field-policies")
    public ApiResponse<List<DataPermissionDtos.FieldPolicyResponse>> queryFieldPolicies(
            @RequestParam(required = false) PermissionSubjectType subjectType,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) String businessObject,
            @RequestParam(required = false) String usageScenario,
            @RequestParam(required = false) String fieldCode,
            @RequestParam(required = false) FieldPermissionAction action,
            @RequestParam(required = false) PermissionEffect effect,
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.queryFieldPolicies(new FieldPermissionQuery(
                                subjectType,
                                subjectId,
                                businessObject,
                                usageScenario,
                                fieldCode,
                                action,
                                effect,
                                tenantId
                        ))
                        .stream()
                        .map(dtoMapper::toFieldPolicyResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/decisions/row")
    public ApiResponse<DataPermissionDtos.RowDecisionResponse> decideRow(
            @Valid @RequestBody DataPermissionDtos.RowDecisionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toRowDecisionResponse(applicationService.decideRow(body.toQuery())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/decisions/field")
    public ApiResponse<DataPermissionDtos.FieldDecisionResponse> decideField(
            @Valid @RequestBody DataPermissionDtos.FieldDecisionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toFieldDecisionResponse(applicationService.decideField(body.toQuery())),
                responseMetaFactory.create(request)
        );
    }

    private BizException notFound(String message) {
        return new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, message);
    }
}
