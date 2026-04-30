package com.hjo2oa.infra.scheduler.interfaces;

import com.hjo2oa.infra.scheduler.application.ScheduledJobApplicationService;
import com.hjo2oa.infra.scheduler.application.SchedulerOperationContext;
import com.hjo2oa.infra.scheduler.domain.ExecutionStatus;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/infra/scheduler/executions")
public class SchedulerExecutionController {

    private final ScheduledJobApplicationService applicationService;
    private final ScheduledJobDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public SchedulerExecutionController(
            ScheduledJobApplicationService applicationService,
            ScheduledJobDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping
    public ApiResponse<List<ScheduledJobDtos.JobExecutionRecordResponse>> list(
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) ExecutionStatus executionStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.queryExecutions(jobId, executionStatus, from, to).stream()
                        .map(dtoMapper::toResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{executionId}")
    public ApiResponse<ScheduledJobDtos.JobExecutionRecordResponse> detail(
            @PathVariable UUID executionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.getExecution(executionId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{executionId}/retry")
    public ApiResponse<ScheduledJobDtos.JobExecutionRecordResponse> retry(
            @PathVariable UUID executionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.retryExecution(executionId, toOperationContext(request))),
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
