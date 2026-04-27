package com.hjo2oa.org.org.sync.audit.interfaces;

import com.hjo2oa.org.org.sync.audit.application.OrgSyncAuditApplicationService;
import com.hjo2oa.org.org.sync.audit.application.OrgSyncAuditCommands;
import com.hjo2oa.org.org.sync.audit.domain.AuditCategory;
import com.hjo2oa.org.org.sync.audit.domain.DiffStatus;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
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
@RequestMapping("/api/v1/org/sync-audit")
public class OrgSyncAuditController {

    private final OrgSyncAuditApplicationService applicationService;
    private final OrgSyncAuditDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public OrgSyncAuditController(
            OrgSyncAuditApplicationService applicationService,
            OrgSyncAuditDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/sources")
    public ApiResponse<OrgSyncAuditDtos.SourceResponse> createSource(
            @Valid @RequestBody OrgSyncAuditDtos.CreateSourceRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.createSource(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/sources/{sourceId}")
    public ApiResponse<OrgSyncAuditDtos.SourceResponse> updateSource(
            @PathVariable UUID sourceId,
            @Valid @RequestBody OrgSyncAuditDtos.UpdateSourceRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.updateSource(body.toCommand(sourceId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/sources/{sourceId}/enable")
    public ApiResponse<OrgSyncAuditDtos.SourceResponse> enableSource(
            @PathVariable UUID sourceId,
            @RequestBody(required = false) OrgSyncAuditDtos.OperatorRequest body,
            HttpServletRequest request
    ) {
        UUID operatorId = body == null ? null : body.operatorId();
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.enableSource(sourceId, operatorId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/sources/{sourceId}/disable")
    public ApiResponse<OrgSyncAuditDtos.SourceResponse> disableSource(
            @PathVariable UUID sourceId,
            @RequestBody(required = false) OrgSyncAuditDtos.OperatorRequest body,
            HttpServletRequest request
    ) {
        UUID operatorId = body == null ? null : body.operatorId();
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.disableSource(sourceId, operatorId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/sources")
    public ApiResponse<List<OrgSyncAuditDtos.SourceResponse>> listSources(
            @RequestParam UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listSources(tenantId).stream().map(dtoMapper::toResponse).toList(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/tasks/full")
    public ApiResponse<OrgSyncAuditDtos.TaskResponse> startFullTask(
            @Valid @RequestBody OrgSyncAuditDtos.StartTaskRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.startFullTask(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/tasks/incremental")
    public ApiResponse<OrgSyncAuditDtos.TaskResponse> startIncrementalTask(
            @Valid @RequestBody OrgSyncAuditDtos.StartTaskRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.startIncrementalTask(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/tasks/{taskId}/retry")
    public ApiResponse<OrgSyncAuditDtos.TaskResponse> retryTask(
            @PathVariable UUID taskId,
            @RequestBody(required = false) OrgSyncAuditDtos.OperatorRequest body,
            HttpServletRequest request
    ) {
        UUID operatorId = body == null ? null : body.operatorId();
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.retryTask(taskId, operatorId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/tasks")
    public ApiResponse<List<OrgSyncAuditDtos.TaskResponse>> listTasks(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) UUID sourceId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listTasks(tenantId, sourceId).stream().map(dtoMapper::toResponse).toList(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/diffs")
    public ApiResponse<OrgSyncAuditDtos.DiffResponse> createDiff(
            @Valid @RequestBody OrgSyncAuditDtos.CreateDiffRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.createDiff(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/diffs/{diffRecordId}/resolve")
    public ApiResponse<OrgSyncAuditDtos.CompensationResponse> resolveDiff(
            @PathVariable UUID diffRecordId,
            @Valid @RequestBody OrgSyncAuditDtos.ResolveDiffRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.resolveDiff(body.toCommand(diffRecordId))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/diffs")
    public ApiResponse<List<OrgSyncAuditDtos.DiffResponse>> queryDiffs(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) UUID taskId,
            @RequestParam(required = false) DiffStatus status,
            HttpServletRequest request
    ) {
        OrgSyncAuditCommands.DiffQuery query = new OrgSyncAuditCommands.DiffQuery(tenantId, taskId, status);
        return ApiResponse.success(
                applicationService.queryDiffs(query).stream().map(dtoMapper::toResponse).toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/audits")
    public ApiResponse<List<OrgSyncAuditDtos.AuditResponse>> queryAudits(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) AuditCategory category,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) UUID taskId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            HttpServletRequest request
    ) {
        OrgSyncAuditCommands.AuditQuery query = new OrgSyncAuditCommands.AuditQuery(
                tenantId,
                category,
                entityType,
                entityId,
                taskId,
                from,
                to
        );
        return ApiResponse.success(
                applicationService.queryAudits(query).stream().map(dtoMapper::toResponse).toList(),
                responseMetaFactory.create(request)
        );
    }
}
