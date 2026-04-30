package com.hjo2oa.org.position.assignment.interfaces;

import com.hjo2oa.org.position.assignment.application.PositionAssignmentApplicationService;
import com.hjo2oa.org.position.assignment.domain.AssignmentView;
import com.hjo2oa.org.position.assignment.domain.PositionRoleView;
import com.hjo2oa.org.position.assignment.domain.PositionView;
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
@RequestMapping("/api/v1/org/position-assignments")
public class PositionAssignmentController {

    private final PositionAssignmentApplicationService applicationService;
    private final PositionAssignmentDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public PositionAssignmentController(
            PositionAssignmentApplicationService applicationService,
            PositionAssignmentDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/positions")
    public ApiResponse<PositionAssignmentDtos.PositionResponse> createPosition(
            @Valid @RequestBody PositionAssignmentDtos.CreatePositionRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(body.tenantId());
        return ApiResponse.success(
                dtoMapper.toPositionResponse(applicationService.createPosition(body.toCommand(tenantId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/positions/{positionId}")
    public ApiResponse<PositionAssignmentDtos.PositionResponse> updatePosition(
            @PathVariable UUID positionId,
            @Valid @RequestBody PositionAssignmentDtos.UpdatePositionRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toPositionResponse(applicationService.updatePosition(body.toCommand(positionId, tenantId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/positions/{positionId}/disable")
    public ApiResponse<PositionAssignmentDtos.PositionResponse> disablePosition(
            @PathVariable UUID positionId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toPositionResponse(applicationService.disablePosition(tenantId, positionId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/positions/{positionId}/activate")
    public ApiResponse<PositionAssignmentDtos.PositionResponse> activatePosition(
            @PathVariable UUID positionId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toPositionResponse(applicationService.activatePosition(tenantId, positionId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/positions/{positionId}")
    public ApiResponse<PositionAssignmentDtos.PositionResponse> getPosition(
            @PathVariable UUID positionId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toPositionResponse(applicationService.getPosition(tenantId, positionId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/positions")
    public ApiResponse<List<PositionAssignmentDtos.PositionResponse>> listPositions(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID departmentId,
            HttpServletRequest request
    ) {
        UUID requestTenantId = requestTenantId(tenantId);
        return ApiResponse.success(
                applicationService.listPositions(requestTenantId, organizationId, departmentId).stream()
                        .map(dtoMapper::toPositionResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/assignments")
    public ApiResponse<PositionAssignmentDtos.AssignmentResponse> createAssignment(
            @Valid @RequestBody PositionAssignmentDtos.CreateAssignmentRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(body.tenantId());
        return ApiResponse.success(
                dtoMapper.toAssignmentResponse(applicationService.createAssignment(body.toCommand(tenantId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/assignments/{assignmentId}/type")
    public ApiResponse<PositionAssignmentDtos.AssignmentResponse> changeAssignmentType(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody PositionAssignmentDtos.ChangeAssignmentTypeRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toAssignmentResponse(applicationService.changeAssignmentType(tenantId, assignmentId, body.type())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/persons/{personId}/primary-assignment/{assignmentId}")
    public ApiResponse<PositionAssignmentDtos.AssignmentResponse> changePrimaryAssignment(
            @PathVariable UUID personId,
            @PathVariable UUID assignmentId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toAssignmentResponse(applicationService.changePrimaryAssignment(tenantId, personId, assignmentId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/assignments/{assignmentId}/deactivate")
    public ApiResponse<PositionAssignmentDtos.AssignmentResponse> deactivateAssignment(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody PositionAssignmentDtos.DeactivateAssignmentRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toAssignmentResponse(applicationService.deactivateAssignment(tenantId, assignmentId, body.endDate())),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/assignments/{assignmentId}")
    public ApiResponse<PositionAssignmentDtos.AssignmentResponse> getAssignment(
            @PathVariable UUID assignmentId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toAssignmentResponse(applicationService.getAssignment(tenantId, assignmentId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/persons/{personId}/assignments")
    public ApiResponse<List<PositionAssignmentDtos.AssignmentResponse>> listAssignmentsByPerson(
            @PathVariable UUID personId,
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        UUID requestTenantId = requestTenantId(tenantId);
        return ApiResponse.success(
                applicationService.listAssignmentsByPerson(requestTenantId, personId).stream()
                        .map(dtoMapper::toAssignmentResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/positions/{positionId}/assignments")
    public ApiResponse<List<PositionAssignmentDtos.AssignmentResponse>> listAssignmentsByPosition(
            @PathVariable UUID positionId,
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        UUID requestTenantId = requestTenantId(tenantId);
        return ApiResponse.success(
                applicationService.listAssignmentsByPosition(requestTenantId, positionId).stream()
                        .map(dtoMapper::toAssignmentResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/positions/{positionId}/roles")
    public ApiResponse<PositionAssignmentDtos.PositionRoleResponse> addPositionRole(
            @PathVariable UUID positionId,
            @Valid @RequestBody PositionAssignmentDtos.AddPositionRoleRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(body.tenantId());
        return ApiResponse.success(
                dtoMapper.toPositionRoleResponse(applicationService.addPositionRole(body.toCommand(positionId, tenantId))),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/positions/{positionId}/roles/{roleId}")
    public ApiResponse<Void> removePositionRole(
            @PathVariable UUID positionId,
            @PathVariable UUID roleId,
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        applicationService.removePositionRole(requestTenantId(tenantId), positionId, roleId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }

    @GetMapping("/positions/{positionId}/roles")
    public ApiResponse<List<PositionAssignmentDtos.PositionRoleResponse>> listPositionRoles(
            @PathVariable UUID positionId,
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        UUID requestTenantId = requestTenantId(tenantId);
        return ApiResponse.success(
                applicationService.listPositionRoles(requestTenantId, positionId).stream()
                        .map(dtoMapper::toPositionRoleResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/export")
    public ApiResponse<PositionAssignmentDtos.PositionAssignmentExportResponse> exportPositionAssignments(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID departmentId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        List<PositionAssignmentDtos.PositionResponse> positions = applicationService
                .listPositions(tenantId, organizationId, departmentId)
                .stream()
                .map(dtoMapper::toPositionResponse)
                .toList();
        List<PositionAssignmentDtos.AssignmentResponse> assignments = positions.stream()
                .flatMap(position -> applicationService.listAssignmentsByPosition(tenantId, position.id()).stream())
                .map(dtoMapper::toAssignmentResponse)
                .toList();
        List<PositionAssignmentDtos.PositionRoleResponse> roles = positions.stream()
                .flatMap(position -> applicationService.listPositionRoles(tenantId, position.id()).stream())
                .map(dtoMapper::toPositionRoleResponse)
                .toList();
        return ApiResponse.success(
                new PositionAssignmentDtos.PositionAssignmentExportResponse(positions, assignments, roles),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/import")
    public ApiResponse<PositionAssignmentDtos.PositionAssignmentImportResponse> importPositionAssignments(
            @Valid @RequestBody PositionAssignmentDtos.PositionAssignmentImportRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        List<UUID> positionIds = new ArrayList<>();
        List<UUID> assignmentIds = new ArrayList<>();
        if (body.positions() != null) {
            for (PositionAssignmentDtos.CreatePositionRequest item : body.positions()) {
                PositionView view = applicationService.createPosition(item.toCommand(requestTenantId(item.tenantId())));
                if (!view.tenantId().equals(tenantId)) {
                    throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "Import position tenant mismatch");
                }
                positionIds.add(view.id());
            }
        }
        if (body.assignments() != null) {
            for (PositionAssignmentDtos.CreateAssignmentRequest item : body.assignments()) {
                AssignmentView view = applicationService.createAssignment(item.toCommand(requestTenantId(item.tenantId())));
                if (!view.tenantId().equals(tenantId)) {
                    throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "Import assignment tenant mismatch");
                }
                assignmentIds.add(view.id());
            }
        }
        return ApiResponse.success(
                new PositionAssignmentDtos.PositionAssignmentImportResponse(
                        positionIds.size(),
                        assignmentIds.size(),
                        positionIds,
                        assignmentIds
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
