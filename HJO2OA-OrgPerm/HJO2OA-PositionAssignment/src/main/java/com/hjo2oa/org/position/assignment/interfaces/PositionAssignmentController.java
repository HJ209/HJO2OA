package com.hjo2oa.org.position.assignment.interfaces;

import com.hjo2oa.org.position.assignment.application.PositionAssignmentApplicationService;
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
        return ApiResponse.success(
                dtoMapper.toPositionResponse(applicationService.createPosition(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/positions/{positionId}")
    public ApiResponse<PositionAssignmentDtos.PositionResponse> updatePosition(
            @PathVariable UUID positionId,
            @Valid @RequestBody PositionAssignmentDtos.UpdatePositionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toPositionResponse(applicationService.updatePosition(body.toCommand(positionId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/positions/{positionId}/disable")
    public ApiResponse<PositionAssignmentDtos.PositionResponse> disablePosition(
            @PathVariable UUID positionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toPositionResponse(applicationService.disablePosition(positionId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/positions/{positionId}/activate")
    public ApiResponse<PositionAssignmentDtos.PositionResponse> activatePosition(
            @PathVariable UUID positionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toPositionResponse(applicationService.activatePosition(positionId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/positions/{positionId}")
    public ApiResponse<PositionAssignmentDtos.PositionResponse> getPosition(
            @PathVariable UUID positionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toPositionResponse(applicationService.getPosition(positionId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/positions")
    public ApiResponse<List<PositionAssignmentDtos.PositionResponse>> listPositions(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID departmentId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listPositions(tenantId, organizationId, departmentId).stream()
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
        return ApiResponse.success(
                dtoMapper.toAssignmentResponse(applicationService.createAssignment(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/assignments/{assignmentId}/type")
    public ApiResponse<PositionAssignmentDtos.AssignmentResponse> changeAssignmentType(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody PositionAssignmentDtos.ChangeAssignmentTypeRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toAssignmentResponse(applicationService.changeAssignmentType(assignmentId, body.type())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/persons/{personId}/primary-assignment/{assignmentId}")
    public ApiResponse<PositionAssignmentDtos.AssignmentResponse> changePrimaryAssignment(
            @PathVariable UUID personId,
            @PathVariable UUID assignmentId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toAssignmentResponse(applicationService.changePrimaryAssignment(personId, assignmentId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/assignments/{assignmentId}/deactivate")
    public ApiResponse<PositionAssignmentDtos.AssignmentResponse> deactivateAssignment(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody PositionAssignmentDtos.DeactivateAssignmentRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toAssignmentResponse(applicationService.deactivateAssignment(assignmentId, body.endDate())),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/assignments/{assignmentId}")
    public ApiResponse<PositionAssignmentDtos.AssignmentResponse> getAssignment(
            @PathVariable UUID assignmentId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toAssignmentResponse(applicationService.getAssignment(assignmentId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/persons/{personId}/assignments")
    public ApiResponse<List<PositionAssignmentDtos.AssignmentResponse>> listAssignmentsByPerson(
            @PathVariable UUID personId,
            @RequestParam UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listAssignmentsByPerson(tenantId, personId).stream()
                        .map(dtoMapper::toAssignmentResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/positions/{positionId}/assignments")
    public ApiResponse<List<PositionAssignmentDtos.AssignmentResponse>> listAssignmentsByPosition(
            @PathVariable UUID positionId,
            @RequestParam UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listAssignmentsByPosition(tenantId, positionId).stream()
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
        return ApiResponse.success(
                dtoMapper.toPositionRoleResponse(applicationService.addPositionRole(body.toCommand(positionId))),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/positions/{positionId}/roles/{roleId}")
    public ApiResponse<Void> removePositionRole(
            @PathVariable UUID positionId,
            @PathVariable UUID roleId,
            @RequestParam UUID tenantId,
            HttpServletRequest request
    ) {
        applicationService.removePositionRole(tenantId, positionId, roleId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }

    @GetMapping("/positions/{positionId}/roles")
    public ApiResponse<List<PositionAssignmentDtos.PositionRoleResponse>> listPositionRoles(
            @PathVariable UUID positionId,
            @RequestParam UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listPositionRoles(tenantId, positionId).stream()
                        .map(dtoMapper::toPositionRoleResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

}
