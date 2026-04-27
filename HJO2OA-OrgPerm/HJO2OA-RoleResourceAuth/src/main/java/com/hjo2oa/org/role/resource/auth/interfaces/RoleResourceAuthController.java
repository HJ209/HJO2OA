package com.hjo2oa.org.role.resource.auth.interfaces;

import com.hjo2oa.org.role.resource.auth.application.RoleResourceAuthApplicationService;
import com.hjo2oa.org.role.resource.auth.domain.RoleCategory;
import com.hjo2oa.org.role.resource.auth.domain.RoleScope;
import com.hjo2oa.org.role.resource.auth.domain.RoleStatus;
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
@RequestMapping("/api/v1/org-perm")
public class RoleResourceAuthController {

    private final RoleResourceAuthApplicationService applicationService;
    private final RoleResourceAuthDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public RoleResourceAuthController(
            RoleResourceAuthApplicationService applicationService,
            RoleResourceAuthDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/roles")
    public ApiResponse<RoleResourceAuthDtos.RoleResponse> createRole(
            @Valid @RequestBody RoleResourceAuthDtos.CreateRoleRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toRoleResponse(applicationService.createRole(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/roles/{roleId}")
    public ApiResponse<RoleResourceAuthDtos.RoleResponse> updateRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody RoleResourceAuthDtos.UpdateRoleRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toRoleResponse(applicationService.updateRole(body.toCommand(roleId))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/roles/{roleId}/status")
    public ApiResponse<RoleResourceAuthDtos.RoleResponse> changeRoleStatus(
            @PathVariable UUID roleId,
            @Valid @RequestBody RoleResourceAuthDtos.ChangeRoleStatusRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toRoleResponse(applicationService.changeRoleStatus(body.toCommand(roleId))),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/roles/{roleId}")
    public ApiResponse<Void> deleteRole(@PathVariable UUID roleId, HttpServletRequest request) {
        applicationService.deleteRole(roleId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }

    @GetMapping("/roles/{roleId}")
    public ApiResponse<RoleResourceAuthDtos.RoleResponse> getRole(
            @PathVariable UUID roleId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toRoleResponse(applicationService.getRole(roleId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleResourceAuthDtos.RoleResponse>> queryRoles(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) RoleCategory category,
            @RequestParam(required = false) RoleScope scope,
            @RequestParam(required = false) RoleStatus status,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.queryRoles(tenantId, category, scope, status).stream()
                        .map(dtoMapper::toRoleResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/roles/{roleId}/resource-permissions")
    public ApiResponse<List<RoleResourceAuthDtos.ResourcePermissionResponse>> replaceResourcePermissions(
            @PathVariable UUID roleId,
            @Valid @RequestBody RoleResourceAuthDtos.ReplaceResourcePermissionsRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.replaceResourcePermissions(body.toCommand(roleId)).stream()
                        .map(dtoMapper::toResourcePermissionResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/roles/{roleId}/resource-permissions")
    public ApiResponse<List<RoleResourceAuthDtos.ResourcePermissionResponse>> queryResourcePermissions(
            @PathVariable UUID roleId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.queryResourcePermissions(roleId).stream()
                        .map(dtoMapper::toResourcePermissionResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/persons/{personId}/roles/direct")
    public ApiResponse<RoleResourceAuthDtos.PersonRoleResponse> grantPersonRole(
            @PathVariable UUID personId,
            @Valid @RequestBody RoleResourceAuthDtos.GrantPersonRoleRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toPersonRoleResponse(applicationService.grantPersonRole(body.toCommand(personId))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/persons/{personId}/roles/direct")
    public ApiResponse<List<RoleResourceAuthDtos.PersonRoleResponse>> queryDirectPersonRoles(
            @PathVariable UUID personId,
            @RequestParam(defaultValue = "false") boolean includeExpired,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.queryDirectPersonRoles(personId, includeExpired).stream()
                        .map(dtoMapper::toPersonRoleResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/persons/{personId}/roles/{roleId}/direct")
    public ApiResponse<Void> revokePersonRole(
            @PathVariable UUID personId,
            @PathVariable UUID roleId,
            HttpServletRequest request
    ) {
        applicationService.revokePersonRole(personId, roleId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }
}
