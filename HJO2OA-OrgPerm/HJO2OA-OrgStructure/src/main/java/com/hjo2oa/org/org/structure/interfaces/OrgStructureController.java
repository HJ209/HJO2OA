package com.hjo2oa.org.org.structure.interfaces;

import com.hjo2oa.org.org.structure.application.OrgStructureApplicationService;
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
        return ApiResponse.success(
                dtoMapper.toOrganizationResponse(applicationService.createOrganization(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/organizations/{organizationId}")
    public ApiResponse<OrgStructureDtos.OrganizationResponse> getOrganization(
            @PathVariable UUID organizationId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toOrganizationResponse(applicationService.getOrganization(organizationId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/organizations")
    public ApiResponse<List<OrgStructureDtos.OrganizationResponse>> listOrganizations(
            @RequestParam UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listOrganizations(tenantId).stream()
                        .map(dtoMapper::toOrganizationResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/organizations/children")
    public ApiResponse<List<OrgStructureDtos.OrganizationResponse>> listChildOrganizations(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) UUID parentId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listChildOrganizations(tenantId, parentId).stream()
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
        return ApiResponse.success(
                dtoMapper.toOrganizationResponse(applicationService.updateOrganization(body.toCommand(organizationId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/organizations/{organizationId}/move")
    public ApiResponse<OrgStructureDtos.OrganizationResponse> moveOrganization(
            @PathVariable UUID organizationId,
            @RequestBody OrgStructureDtos.MoveNodeRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toOrganizationResponse(
                        applicationService.moveOrganization(body.toOrganizationCommand(organizationId))
                ),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/organizations/{organizationId}/activate")
    public ApiResponse<OrgStructureDtos.OrganizationResponse> activateOrganization(
            @PathVariable UUID organizationId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toOrganizationResponse(applicationService.activateOrganization(organizationId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/organizations/{organizationId}/disable")
    public ApiResponse<OrgStructureDtos.OrganizationResponse> disableOrganization(
            @PathVariable UUID organizationId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toOrganizationResponse(applicationService.disableOrganization(organizationId)),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/organizations/{organizationId}")
    public ApiResponse<Void> deleteOrganization(
            @PathVariable UUID organizationId,
            HttpServletRequest request
    ) {
        applicationService.deleteOrganization(organizationId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }

    @PostMapping("/departments")
    public ApiResponse<OrgStructureDtos.DepartmentResponse> createDepartment(
            @Valid @RequestBody OrgStructureDtos.CreateDepartmentRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDepartmentResponse(applicationService.createDepartment(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/departments/{departmentId}")
    public ApiResponse<OrgStructureDtos.DepartmentResponse> getDepartment(
            @PathVariable UUID departmentId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDepartmentResponse(applicationService.getDepartment(departmentId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/departments")
    public ApiResponse<List<OrgStructureDtos.DepartmentResponse>> listDepartments(
            @RequestParam UUID tenantId,
            @RequestParam UUID organizationId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listDepartments(tenantId, organizationId).stream()
                        .map(dtoMapper::toDepartmentResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/departments/children")
    public ApiResponse<List<OrgStructureDtos.DepartmentResponse>> listChildDepartments(
            @RequestParam UUID tenantId,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID parentId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listChildDepartments(tenantId, organizationId, parentId).stream()
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
        return ApiResponse.success(
                dtoMapper.toDepartmentResponse(applicationService.updateDepartment(body.toCommand(departmentId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/departments/{departmentId}/move")
    public ApiResponse<OrgStructureDtos.DepartmentResponse> moveDepartment(
            @PathVariable UUID departmentId,
            @RequestBody OrgStructureDtos.MoveNodeRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDepartmentResponse(applicationService.moveDepartment(body.toDepartmentCommand(departmentId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/departments/{departmentId}/activate")
    public ApiResponse<OrgStructureDtos.DepartmentResponse> activateDepartment(
            @PathVariable UUID departmentId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDepartmentResponse(applicationService.activateDepartment(departmentId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/departments/{departmentId}/disable")
    public ApiResponse<OrgStructureDtos.DepartmentResponse> disableDepartment(
            @PathVariable UUID departmentId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDepartmentResponse(applicationService.disableDepartment(departmentId)),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/departments/{departmentId}")
    public ApiResponse<Void> deleteDepartment(
            @PathVariable UUID departmentId,
            HttpServletRequest request
    ) {
        applicationService.deleteDepartment(departmentId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }
}
