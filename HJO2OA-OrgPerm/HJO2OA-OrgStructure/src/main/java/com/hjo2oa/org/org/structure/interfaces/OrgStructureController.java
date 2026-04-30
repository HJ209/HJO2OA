package com.hjo2oa.org.org.structure.interfaces;

import com.hjo2oa.org.org.structure.application.OrgStructureApplicationService;
import com.hjo2oa.org.org.structure.domain.DepartmentView;
import com.hjo2oa.org.org.structure.domain.OrganizationView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.tenant.TenantContextHolder;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
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
@RequestMapping("/api/v1/org/structure")
public class OrgStructureController {

    private final OrgStructureApplicationService applicationService;
    private final OrgStructureDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public OrgStructureController(
            OrgStructureApplicationService applicationService,
            OrgStructureDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/organizations")
    public ApiResponse<OrgStructureDtos.OrganizationResponse> createOrganization(
            @Valid @RequestBody OrgStructureDtos.CreateOrganizationRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(body.tenantId());
        return ApiResponse.success(
                dtoMapper.toOrganizationResponse(applicationService.createOrganization(body.toCommand(tenantId))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/organizations/{organizationId}")
    public ApiResponse<OrgStructureDtos.OrganizationResponse> getOrganization(
            @PathVariable UUID organizationId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toOrganizationResponse(applicationService.getOrganization(tenantId, organizationId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/organizations")
    public ApiResponse<List<OrgStructureDtos.OrganizationResponse>> listOrganizations(
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        UUID requestTenantId = requestTenantId(tenantId);
        return ApiResponse.success(
                applicationService.listOrganizations(requestTenantId).stream()
                        .map(dtoMapper::toOrganizationResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/organizations/children")
    public ApiResponse<List<OrgStructureDtos.OrganizationResponse>> listChildOrganizations(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) UUID parentId,
            HttpServletRequest request
    ) {
        UUID requestTenantId = requestTenantId(tenantId);
        return ApiResponse.success(
                applicationService.listChildOrganizations(requestTenantId, parentId).stream()
                        .map(dtoMapper::toOrganizationResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/organizations/{organizationId}")
    public ApiResponse<OrgStructureDtos.OrganizationResponse> updateOrganization(
            @PathVariable UUID organizationId,
            @Valid @RequestBody OrgStructureDtos.UpdateOrganizationRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toOrganizationResponse(applicationService.updateOrganization(body.toCommand(organizationId, tenantId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/organizations/{organizationId}/move")
    public ApiResponse<OrgStructureDtos.OrganizationResponse> moveOrganization(
            @PathVariable UUID organizationId,
            @RequestBody OrgStructureDtos.MoveNodeRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toOrganizationResponse(
                        applicationService.moveOrganization(body.toOrganizationCommand(organizationId, tenantId))
                ),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/organizations/{organizationId}/activate")
    public ApiResponse<OrgStructureDtos.OrganizationResponse> activateOrganization(
            @PathVariable UUID organizationId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toOrganizationResponse(applicationService.activateOrganization(tenantId, organizationId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/organizations/{organizationId}/disable")
    public ApiResponse<OrgStructureDtos.OrganizationResponse> disableOrganization(
            @PathVariable UUID organizationId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toOrganizationResponse(applicationService.disableOrganization(tenantId, organizationId)),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/organizations/{organizationId}")
    public ApiResponse<Void> deleteOrganization(
            @PathVariable UUID organizationId,
            HttpServletRequest request
    ) {
        applicationService.deleteOrganization(requestTenantId(null), organizationId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }

    @PostMapping("/departments")
    public ApiResponse<OrgStructureDtos.DepartmentResponse> createDepartment(
            @Valid @RequestBody OrgStructureDtos.CreateDepartmentRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(body.tenantId());
        return ApiResponse.success(
                dtoMapper.toDepartmentResponse(applicationService.createDepartment(body.toCommand(tenantId))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/departments/{departmentId}")
    public ApiResponse<OrgStructureDtos.DepartmentResponse> getDepartment(
            @PathVariable UUID departmentId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toDepartmentResponse(applicationService.getDepartment(tenantId, departmentId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/departments")
    public ApiResponse<List<OrgStructureDtos.DepartmentResponse>> listDepartments(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam UUID organizationId,
            HttpServletRequest request
    ) {
        UUID requestTenantId = requestTenantId(tenantId);
        return ApiResponse.success(
                applicationService.listDepartments(requestTenantId, organizationId).stream()
                        .map(dtoMapper::toDepartmentResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/departments/children")
    public ApiResponse<List<OrgStructureDtos.DepartmentResponse>> listChildDepartments(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID parentId,
            HttpServletRequest request
    ) {
        UUID requestTenantId = requestTenantId(tenantId);
        return ApiResponse.success(
                applicationService.listChildDepartments(requestTenantId, organizationId, parentId).stream()
                        .map(dtoMapper::toDepartmentResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/departments/{departmentId}")
    public ApiResponse<OrgStructureDtos.DepartmentResponse> updateDepartment(
            @PathVariable UUID departmentId,
            @Valid @RequestBody OrgStructureDtos.UpdateDepartmentRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toDepartmentResponse(applicationService.updateDepartment(body.toCommand(departmentId, tenantId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/departments/{departmentId}/move")
    public ApiResponse<OrgStructureDtos.DepartmentResponse> moveDepartment(
            @PathVariable UUID departmentId,
            @RequestBody OrgStructureDtos.MoveNodeRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toDepartmentResponse(applicationService.moveDepartment(body.toDepartmentCommand(departmentId, tenantId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/departments/{departmentId}/activate")
    public ApiResponse<OrgStructureDtos.DepartmentResponse> activateDepartment(
            @PathVariable UUID departmentId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toDepartmentResponse(applicationService.activateDepartment(tenantId, departmentId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/departments/{departmentId}/disable")
    public ApiResponse<OrgStructureDtos.DepartmentResponse> disableDepartment(
            @PathVariable UUID departmentId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toDepartmentResponse(applicationService.disableDepartment(tenantId, departmentId)),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/departments/{departmentId}")
    public ApiResponse<Void> deleteDepartment(
            @PathVariable UUID departmentId,
            HttpServletRequest request
    ) {
        applicationService.deleteDepartment(requestTenantId(null), departmentId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }

    @GetMapping("/export")
    public ApiResponse<OrgStructureDtos.OrgStructureExportResponse> exportStructure(HttpServletRequest request) {
        UUID tenantId = requestTenantId(null);
        List<OrgStructureDtos.OrganizationResponse> organizations = applicationService.listOrganizations(tenantId)
                .stream()
                .map(dtoMapper::toOrganizationResponse)
                .toList();
        List<OrgStructureDtos.DepartmentResponse> departments = organizations.stream()
                .flatMap(organization -> applicationService.listDepartments(tenantId, organization.id()).stream())
                .map(dtoMapper::toDepartmentResponse)
                .toList();
        return ApiResponse.success(
                new OrgStructureDtos.OrgStructureExportResponse(organizations, departments),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/import")
    public ApiResponse<OrgStructureDtos.OrgStructureImportResponse> importStructure(
            @Valid @RequestBody OrgStructureDtos.OrgStructureImportRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        List<UUID> organizationIds = new ArrayList<>();
        List<UUID> departmentIds = new ArrayList<>();
        if (body.organizations() != null) {
            for (OrgStructureDtos.CreateOrganizationRequest item : body.organizations()) {
                OrganizationView view = applicationService.createOrganization(item.toCommand(requestTenantId(item.tenantId())));
                if (!view.tenantId().equals(tenantId)) {
                    throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "Import organization tenant mismatch");
                }
                organizationIds.add(view.id());
            }
        }
        if (body.departments() != null) {
            for (OrgStructureDtos.CreateDepartmentRequest item : body.departments()) {
                DepartmentView view = applicationService.createDepartment(item.toCommand(requestTenantId(item.tenantId())));
                if (!view.tenantId().equals(tenantId)) {
                    throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "Import department tenant mismatch");
                }
                departmentIds.add(view.id());
            }
        }
        return ApiResponse.success(
                new OrgStructureDtos.OrgStructureImportResponse(
                        organizationIds.size(),
                        departmentIds.size(),
                        organizationIds,
                        departmentIds
                ),
                responseMetaFactory.create(request)
        );
    }

    private UUID requestTenantId(UUID requestValue) {
        UUID headerTenantId = TenantContextHolder.currentTenantId()
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.BAD_REQUEST, "X-Tenant-Id is required"));
        if (requestValue != null && !requestValue.equals(headerTenantId)) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "Tenant id does not match X-Tenant-Id");
        }
        return headerTenantId;
    }
}
