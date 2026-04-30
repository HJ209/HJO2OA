package com.hjo2oa.process.monitor.interfaces;

import com.hjo2oa.process.monitor.application.ProcessMonitorQueryApplicationService;
import com.hjo2oa.process.monitor.domain.MonitorQueryFilter;
import com.hjo2oa.process.monitor.domain.ProcessInterventionCommand;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping({"/api/v1/process/monitor/admin", "/api/v1/admin/wf/process-monitor"})
public class ProcessMonitorAdminController {

    private final ProcessMonitorQueryApplicationService applicationService;
    private final ProcessMonitorDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public ProcessMonitorAdminController(
            ProcessMonitorQueryApplicationService applicationService,
            ProcessMonitorDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/overview")
    public ApiResponse<ProcessMonitorDtos.OverviewResponse> overview(
            @RequestParam(name = "filter[tenantId]", required = false) UUID tenantId,
            @RequestParam(name = "filter[definitionId]", required = false) UUID definitionId,
            @RequestParam(name = "filter[definitionCode]", required = false) String definitionCode,
            @RequestParam(name = "filter[category]", required = false) String category,
            @RequestParam(name = "filter[startedFrom]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedFrom,
            @RequestParam(name = "filter[startedTo]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedTo,
            @RequestParam(name = "page[limit]", required = false) Integer limit,
            @RequestParam(name = "filter[stalledThresholdMinutes]", required = false) Long stalledThresholdMinutes,
            HttpServletRequest request
    ) {
        MonitorQueryFilter filter = toFilter(
                tenantId,
                definitionId,
                definitionCode,
                category,
                startedFrom,
                startedTo,
                limit,
                stalledThresholdMinutes
        );
        return ApiResponse.success(
                new ProcessMonitorDtos.OverviewResponse(
                        dtoMapper.toDurationResponses(applicationService.processDurations(filter)),
                        dtoMapper.toStalledNodeResponses(applicationService.stalledNodes(filter)),
                        dtoMapper.toCongestionResponses(applicationService.approvalCongestion(filter)),
                        dtoMapper.toOverdueTaskResponses(applicationService.overdueTasks(filter))
                ),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/process-durations")
    public ApiResponse<List<ProcessMonitorDtos.ProcessDurationResponse>> processDurations(
            @RequestParam(name = "filter[tenantId]", required = false) UUID tenantId,
            @RequestParam(name = "filter[definitionId]", required = false) UUID definitionId,
            @RequestParam(name = "filter[definitionCode]", required = false) String definitionCode,
            @RequestParam(name = "filter[category]", required = false) String category,
            @RequestParam(name = "filter[startedFrom]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedFrom,
            @RequestParam(name = "filter[startedTo]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedTo,
            @RequestParam(name = "page[limit]", required = false) Integer limit,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDurationResponses(applicationService.processDurations(toFilter(
                        tenantId,
                        definitionId,
                        definitionCode,
                        category,
                        startedFrom,
                        startedTo,
                        limit,
                        null
                ))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/stalled-nodes")
    public ApiResponse<List<ProcessMonitorDtos.StalledNodeResponse>> stalledNodes(
            @RequestParam(name = "filter[tenantId]", required = false) UUID tenantId,
            @RequestParam(name = "filter[definitionId]", required = false) UUID definitionId,
            @RequestParam(name = "filter[definitionCode]", required = false) String definitionCode,
            @RequestParam(name = "filter[category]", required = false) String category,
            @RequestParam(name = "filter[startedFrom]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedFrom,
            @RequestParam(name = "filter[startedTo]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedTo,
            @RequestParam(name = "page[limit]", required = false) Integer limit,
            @RequestParam(name = "filter[stalledThresholdMinutes]", required = false) Long stalledThresholdMinutes,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toStalledNodeResponses(applicationService.stalledNodes(toFilter(
                        tenantId,
                        definitionId,
                        definitionCode,
                        category,
                        startedFrom,
                        startedTo,
                        limit,
                        stalledThresholdMinutes
                ))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/approval-congestion")
    public ApiResponse<List<ProcessMonitorDtos.ApprovalCongestionResponse>> approvalCongestion(
            @RequestParam(name = "filter[tenantId]", required = false) UUID tenantId,
            @RequestParam(name = "filter[definitionId]", required = false) UUID definitionId,
            @RequestParam(name = "filter[definitionCode]", required = false) String definitionCode,
            @RequestParam(name = "filter[category]", required = false) String category,
            @RequestParam(name = "filter[startedFrom]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedFrom,
            @RequestParam(name = "filter[startedTo]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedTo,
            @RequestParam(name = "page[limit]", required = false) Integer limit,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toCongestionResponses(applicationService.approvalCongestion(toFilter(
                        tenantId,
                        definitionId,
                        definitionCode,
                        category,
                        startedFrom,
                        startedTo,
                        limit,
                        null
                ))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/overdue-tasks")
    public ApiResponse<List<ProcessMonitorDtos.OverdueTaskResponse>> overdueTasks(
            @RequestParam(name = "filter[tenantId]", required = false) UUID tenantId,
            @RequestParam(name = "filter[definitionId]", required = false) UUID definitionId,
            @RequestParam(name = "filter[definitionCode]", required = false) String definitionCode,
            @RequestParam(name = "filter[category]", required = false) String category,
            @RequestParam(name = "filter[startedFrom]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedFrom,
            @RequestParam(name = "filter[startedTo]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedTo,
            @RequestParam(name = "page[limit]", required = false) Integer limit,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toOverdueTaskResponses(applicationService.overdueTasks(toFilter(
                        tenantId,
                        definitionId,
                        definitionCode,
                        category,
                        startedFrom,
                        startedTo,
                        limit,
                        null
                ))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/instances")
    public ApiResponse<List<ProcessMonitorDtos.MonitoredInstanceResponse>> instances(
            @RequestParam(name = "filter[tenantId]", required = false) UUID tenantId,
            @RequestParam(name = "filter[definitionId]", required = false) UUID definitionId,
            @RequestParam(name = "filter[definitionCode]", required = false) String definitionCode,
            @RequestParam(name = "filter[category]", required = false) String category,
            @RequestParam(name = "filter[status]", required = false) String status,
            @RequestParam(name = "filter[startedFrom]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedFrom,
            @RequestParam(name = "filter[startedTo]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedTo,
            @RequestParam(name = "page[limit]", required = false) Integer limit,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toInstanceResponses(applicationService.instances(toFilter(
                        tenantId,
                        definitionId,
                        definitionCode,
                        category,
                        startedFrom,
                        startedTo,
                        limit,
                        null
                ), status)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/exceptions")
    public ApiResponse<List<ProcessMonitorDtos.ExceptionInstanceResponse>> exceptions(
            @RequestParam(name = "filter[tenantId]", required = false) UUID tenantId,
            @RequestParam(name = "filter[definitionId]", required = false) UUID definitionId,
            @RequestParam(name = "filter[definitionCode]", required = false) String definitionCode,
            @RequestParam(name = "filter[category]", required = false) String category,
            @RequestParam(name = "page[limit]", required = false) Integer limit,
            @RequestParam(name = "filter[stalledThresholdMinutes]", required = false) Long stalledThresholdMinutes,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toExceptionResponses(applicationService.exceptionInstances(toFilter(
                        tenantId,
                        definitionId,
                        definitionCode,
                        category,
                        null,
                        null,
                        limit,
                        stalledThresholdMinutes
                ))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/instances/{instanceId}/trail")
    public ApiResponse<List<ProcessMonitorDtos.NodeTrailResponse>> nodeTrail(
            @RequestParam(name = "filter[tenantId]") UUID tenantId,
            @PathVariable UUID instanceId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toNodeTrailResponses(applicationService.nodeTrail(tenantId, instanceId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/instances/{instanceId}/interventions")
    public ApiResponse<List<ProcessMonitorDtos.InterventionResponse>> interventions(
            @RequestParam(name = "filter[tenantId]") UUID tenantId,
            @PathVariable UUID instanceId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toInterventionResponses(applicationService.interventions(tenantId, instanceId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/instances/{instanceId}/interventions")
    public ApiResponse<ProcessMonitorDtos.InterventionResponse> intervene(
            @PathVariable UUID instanceId,
            @RequestBody ProcessMonitorDtos.InterventionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toInterventionResponse(applicationService.intervene(new ProcessInterventionCommand(
                        body.tenantId(),
                        instanceId,
                        body.taskId(),
                        body.actionType(),
                        body.operatorId(),
                        body.targetAssigneeId(),
                        body.reason()
                ))),
                responseMetaFactory.create(request)
        );
    }

    private MonitorQueryFilter toFilter(
            UUID tenantId,
            UUID definitionId,
            String definitionCode,
            String category,
            Instant startedFrom,
            Instant startedTo,
            Integer limit,
            Long stalledThresholdMinutes
    ) {
        return MonitorQueryFilter.of(
                tenantId,
                definitionId,
                definitionCode,
                category,
                startedFrom,
                startedTo,
                limit,
                stalledThresholdMinutes
        );
    }
}
