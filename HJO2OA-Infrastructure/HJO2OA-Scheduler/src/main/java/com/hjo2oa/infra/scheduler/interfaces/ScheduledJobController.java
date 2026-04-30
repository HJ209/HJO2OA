package com.hjo2oa.infra.scheduler.interfaces;

import com.hjo2oa.infra.scheduler.application.ScheduledJobApplicationService;
import com.hjo2oa.infra.scheduler.application.ScheduledJobCommands;
import com.hjo2oa.infra.scheduler.application.SchedulerOperationContext;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/infra/scheduler/jobs")
public class ScheduledJobController {

    private final ScheduledJobApplicationService applicationService;
    private final ScheduledJobDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public ScheduledJobController(
            ScheduledJobApplicationService applicationService,
            ScheduledJobDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping
    public ApiResponse<ScheduledJobDtos.ScheduledJobResponse> registerJob(
            @Valid @RequestBody ScheduledJobDtos.RegisterJobRequest body,
            HttpServletRequest request
    ) {
        ScheduledJobCommands.RegisterJobCommand command = dtoMapper.toRegisterCommand(body);
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.registerJob(
                        command.jobCode(),
                        command.handlerName(),
                        command.name(),
                        command.triggerType(),
                        command.cronExpr(),
                        command.timezoneId(),
                        command.concurrencyPolicy(),
                        command.timeoutSeconds(),
                        command.retryPolicy(),
                        command.tenantId()
                )),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{jobId}/enable")
    public ApiResponse<ScheduledJobDtos.ScheduledJobResponse> enableJob(
            @PathVariable @NotNull UUID jobId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.enableJob(jobId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{jobId}/pause")
    public ApiResponse<ScheduledJobDtos.ScheduledJobResponse> pauseJob(
            @PathVariable @NotNull UUID jobId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.pauseJob(jobId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{jobId}/resume")
    public ApiResponse<ScheduledJobDtos.ScheduledJobResponse> resumeJob(
            @PathVariable @NotNull UUID jobId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.resumeJob(jobId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{jobId}/disable")
    public ApiResponse<ScheduledJobDtos.ScheduledJobResponse> disableJob(
            @PathVariable @NotNull UUID jobId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.disableJob(jobId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/trigger/{jobCode}")
    public ApiResponse<ScheduledJobDtos.JobExecutionRecordResponse> triggerJobByCode(
            @PathVariable String jobCode,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.triggerJob(jobCode, toOperationContext(request))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{jobId}/trigger")
    public ApiResponse<ScheduledJobDtos.JobExecutionRecordResponse> triggerJob(
            @PathVariable @NotNull UUID jobId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.triggerJob(jobId, toOperationContext(request))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping
    public ApiResponse<List<ScheduledJobDtos.ScheduledJobResponse>> queryJobs(
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.queryJobs(tenantId).stream().map(dtoMapper::toResponse).toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{jobId}/executions")
    public ApiResponse<List<ScheduledJobDtos.JobExecutionRecordResponse>> queryExecutions(
            @PathVariable UUID jobId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.queryExecutions(jobId, from, to).stream().map(dtoMapper::toResponse).toList(),
                responseMetaFactory.create(request)
        );
    }

    private SchedulerOperationContext toOperationContext(HttpServletRequest request) {
        return new SchedulerOperationContext(
                parseUuidHeader(request, "X-Tenant-Id"),
                parseUuidHeader(request, "X-Operator-Account-Id"),
                parseUuidHeader(request, "X-Operator-Person-Id"),
                request.getHeader(ResponseMetaFactory.REQUEST_ID_HEADER),
                request.getHeader("X-Idempotency-Key"),
                request.getHeader("Accept-Language"),
                request.getHeader("X-Timezone"),
                "http:" + request.getMethod() + " " + request.getRequestURI()
        );
    }

    private UUID parseUuidHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, headerName + " must be a UUID", ex);
        }
    }
}
