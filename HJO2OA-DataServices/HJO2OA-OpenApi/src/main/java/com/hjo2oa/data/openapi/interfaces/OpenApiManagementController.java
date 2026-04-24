package com.hjo2oa.data.openapi.interfaces;

import com.hjo2oa.data.openapi.application.OpenApiManagementApplicationService;
import com.hjo2oa.data.openapi.domain.ApiCredentialGrant;
import com.hjo2oa.data.openapi.domain.ApiCredentialStatus;
import com.hjo2oa.data.openapi.domain.ApiInvocationAuditLog;
import com.hjo2oa.data.openapi.domain.ApiRateLimitPolicyView;
import com.hjo2oa.data.openapi.domain.OpenApiEndpointListItemView;
import com.hjo2oa.data.openapi.domain.OpenApiEndpointView;
import com.hjo2oa.data.openapi.domain.OpenApiHttpMethod;
import com.hjo2oa.data.openapi.domain.OpenApiStatus;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.PageData;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
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
@RequestMapping("/api/v1/data/open-api")
public class OpenApiManagementController {

    private final OpenApiManagementApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public OpenApiManagementController(
            OpenApiManagementApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/endpoints")
    public ApiResponse<PageData<OpenApiEndpointListItemView>> pageEndpoints(
            @RequestParam(required = false) String path,
            @RequestParam(required = false) OpenApiHttpMethod httpMethod,
            @RequestParam(required = false) String version,
            @RequestParam(required = false) OpenApiStatus status,
            @RequestParam(required = false) String dataServiceCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.pageEndpoints(path, httpMethod, version, status, dataServiceCode, page, size),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/endpoints/{code}/versions/{version}")
    public ApiResponse<OpenApiEndpointView> endpointDetail(
            @PathVariable String code,
            @PathVariable String version,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.endpointDetail(code, version), responseMetaFactory.create(request));
    }

    @PutMapping("/endpoints/{code}/versions/{version}")
    public ApiResponse<OpenApiEndpointView> upsertEndpoint(
            @PathVariable String code,
            @PathVariable String version,
            @Valid @RequestBody UpsertOpenApiEndpointRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.upsertEndpoint(
                        code,
                        version,
                        body.name(),
                        body.dataServiceCode(),
                        body.path(),
                        body.httpMethod(),
                        body.authType(),
                        body.compatibilityNotes()
                ),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/endpoints/{code}/versions/{version}/publish")
    public ApiResponse<OpenApiEndpointView> publishEndpoint(
            @PathVariable String code,
            @PathVariable String version,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.publishEndpoint(code, version), responseMetaFactory.create(request));
    }

    @PostMapping("/endpoints/{code}/versions/{version}/deprecate")
    public ApiResponse<OpenApiEndpointView> deprecateEndpoint(
            @PathVariable String code,
            @PathVariable String version,
            @RequestBody(required = false) DeprecateOpenApiEndpointRequest body,
            HttpServletRequest request
    ) {
        Instant sunsetAt = body == null ? null : body.sunsetAt();
        return ApiResponse.success(
                applicationService.deprecateEndpoint(code, version, sunsetAt),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/endpoints/{code}/versions/{version}/offline")
    public ApiResponse<OpenApiEndpointView> offlineEndpoint(
            @PathVariable String code,
            @PathVariable String version,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.offlineEndpoint(code, version), responseMetaFactory.create(request));
    }

    @DeleteMapping("/endpoints/{code}/versions/{version}")
    public ApiResponse<Void> deleteEndpoint(
            @PathVariable String code,
            @PathVariable String version,
            HttpServletRequest request
    ) {
        applicationService.deleteEndpoint(code, version);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }

    @GetMapping("/credentials")
    public ApiResponse<PageData<ApiCredentialGrant>> pageCredentials(
            @RequestParam(required = false) String clientCode,
            @RequestParam(required = false) ApiCredentialStatus status,
            @RequestParam(required = false) Instant expiresBefore,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.pageCredentials(clientCode, status, expiresBefore, page, size),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/endpoints/{code}/versions/{version}/credentials/{clientCode}")
    public ApiResponse<OpenApiEndpointView> upsertCredential(
            @PathVariable String code,
            @PathVariable String version,
            @PathVariable String clientCode,
            @Valid @RequestBody UpsertApiCredentialGrantRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.upsertCredential(
                        code,
                        version,
                        clientCode,
                        body.secretRef(),
                        body.scopes(),
                        body.expiresAt()
                ),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/endpoints/{code}/versions/{version}/credentials/{clientCode}/revoke")
    public ApiResponse<OpenApiEndpointView> revokeCredential(
            @PathVariable String code,
            @PathVariable String version,
            @PathVariable String clientCode,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.revokeCredential(code, version, clientCode),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/policies")
    public ApiResponse<PageData<ApiRateLimitPolicyView>> pagePolicies(
            @RequestParam(required = false) String endpointCode,
            @RequestParam(required = false) String clientCode,
            @RequestParam(required = false) String policyCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.pagePolicies(endpointCode, clientCode, policyCode, page, size),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/endpoints/{code}/versions/{version}/policies/{policyCode}")
    public ApiResponse<OpenApiEndpointView> upsertPolicy(
            @PathVariable String code,
            @PathVariable String version,
            @PathVariable String policyCode,
            @Valid @RequestBody UpsertApiRateLimitPolicyRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.upsertPolicy(
                        code,
                        version,
                        policyCode,
                        body.clientCode(),
                        body.policyType(),
                        body.windowValue(),
                        body.windowUnit(),
                        body.threshold(),
                        body.description()
                ),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/endpoints/{code}/versions/{version}/policies/{policyCode}/disable")
    public ApiResponse<OpenApiEndpointView> disablePolicy(
            @PathVariable String code,
            @PathVariable String version,
            @PathVariable String policyCode,
            @RequestParam(required = false) String clientCode,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.disablePolicy(code, version, policyCode, clientCode),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageData<ApiInvocationAuditLog>> pageAuditLogs(
            @RequestParam(required = false) String endpointCode,
            @RequestParam(required = false) String clientCode,
            @RequestParam(required = false) Integer responseStatus,
            @RequestParam(required = false) Instant occurredFrom,
            @RequestParam(required = false) Instant occurredTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.pageAuditLogs(endpointCode, clientCode, responseStatus, occurredFrom, occurredTo, page, size),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/audit-logs/{logId}")
    public ApiResponse<ApiInvocationAuditLog> auditLogDetail(
            @PathVariable String logId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.auditLogDetail(logId), responseMetaFactory.create(request));
    }

    @PostMapping("/audit-logs/{logId}/review")
    public ApiResponse<ApiInvocationAuditLog> reviewAuditLog(
            @PathVariable String logId,
            @RequestBody ReviewApiInvocationAuditLogRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.reviewAuditLog(
                        logId,
                        body.abnormalFlag(),
                        body.reviewConclusion(),
                        body.note()
                ),
                responseMetaFactory.create(request)
        );
    }
}
