package com.hjo2oa.wf.process.instance.interfaces;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import com.hjo2oa.wf.process.instance.application.ProcessInstanceApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping({"/api/v1/process/instances", "/api/v1/wf/process/instances"})
public class ProcessInstanceController {

    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private final ProcessInstanceApplicationService applicationService;
    private final ProcessInstanceDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public ProcessInstanceController(
            ProcessInstanceApplicationService applicationService,
            ProcessInstanceDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping
    public ApiResponse<ProcessInstanceDtos.InstanceDetailResponse> start(
            @Valid @RequestBody ProcessInstanceDtos.StartProcessRequest body,
            @RequestHeader(value = TENANT_ID_HEADER, required = false) UUID tenantId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.start(body.toCommand(
                        resolveTenantId(tenantId, body.tenantId()),
                        requireIdempotencyKey(idempotencyKey),
                        requestId(request)
                ))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{instanceId}")
    public ApiResponse<ProcessInstanceDtos.InstanceDetailResponse> detail(
            @PathVariable UUID instanceId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.detail(instanceId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{instanceId}/timeline")
    public ApiResponse<ProcessInstanceDtos.InstanceDetailResponse> timeline(
            @PathVariable UUID instanceId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.timeline(instanceId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{instanceId}/terminate")
    @PutMapping("/{instanceId}/terminate")
    public ApiResponse<ProcessInstanceDtos.InstanceDetailResponse> terminate(
            @PathVariable UUID instanceId,
            @Valid @RequestBody ProcessInstanceDtos.ControlProcessRequest body,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.terminate(body.toTerminateCommand(
                        instanceId,
                        requireIdempotencyKey(idempotencyKey),
                        requestId(request)
                ))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{instanceId}/suspend")
    public ApiResponse<ProcessInstanceDtos.InstanceDetailResponse> suspend(
            @PathVariable UUID instanceId,
            @Valid @RequestBody ProcessInstanceDtos.ControlProcessRequest body,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.suspend(body.toSuspendCommand(
                        instanceId,
                        requireIdempotencyKey(idempotencyKey),
                        requestId(request)
                ))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{instanceId}/resume")
    public ApiResponse<ProcessInstanceDtos.InstanceDetailResponse> resume(
            @PathVariable UUID instanceId,
            @Valid @RequestBody ProcessInstanceDtos.ControlProcessRequest body,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.resume(body.toResumeCommand(
                        instanceId,
                        requireIdempotencyKey(idempotencyKey),
                        requestId(request)
                ))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/tasks/{taskId}/claim")
    @PutMapping("/tasks/{taskId}/claim")
    public ApiResponse<ProcessInstanceDtos.TaskInstanceResponse> claim(
            @PathVariable UUID taskId,
            @Valid @RequestBody ProcessInstanceDtos.ClaimTaskRequest body,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toTaskResponse(applicationService.claim(body.toCommand(
                        taskId,
                        requireIdempotencyKey(idempotencyKey),
                        requestId(request)
                ))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/tasks/{taskId}/transfer")
    @PutMapping("/tasks/{taskId}/transfer")
    public ApiResponse<ProcessInstanceDtos.TaskInstanceResponse> transfer(
            @PathVariable UUID taskId,
            @Valid @RequestBody ProcessInstanceDtos.TransferTaskRequest body,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toTaskResponse(applicationService.transfer(body.toCommand(
                        taskId,
                        requireIdempotencyKey(idempotencyKey),
                        requestId(request)
                ))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/tasks/{taskId}/add-sign")
    public ApiResponse<ProcessInstanceDtos.InstanceDetailResponse> addSign(
            @PathVariable UUID taskId,
            @Valid @RequestBody ProcessInstanceDtos.AddSignRequest body,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.addSign(body.toCommand(
                        taskId,
                        requireIdempotencyKey(idempotencyKey),
                        requestId(request)
                ))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ApiResponse<ProcessInstanceDtos.InstanceDetailResponse> complete(
            @PathVariable UUID taskId,
            @Valid @RequestBody ProcessInstanceDtos.CompleteTaskRequest body,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.completeTask(body.toCommand(
                        taskId,
                        requireIdempotencyKey(idempotencyKey),
                        requestId(request)
                ))),
                responseMetaFactory.create(request)
        );
    }

    private UUID resolveTenantId(UUID headerTenantId, UUID bodyTenantId) {
        UUID tenantId = headerTenantId == null ? bodyTenantId : headerTenantId;
        if (tenantId == null) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "X-Tenant-Id is required");
        }
        return tenantId;
    }

    private String requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "X-Idempotency-Key is required");
        }
        return idempotencyKey;
    }

    private String requestId(HttpServletRequest request) {
        return request == null ? null : request.getHeader(ResponseMetaFactory.REQUEST_ID_HEADER);
    }
}
