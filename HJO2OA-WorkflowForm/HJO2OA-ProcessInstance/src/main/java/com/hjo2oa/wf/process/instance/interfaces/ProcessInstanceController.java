package com.hjo2oa.wf.process.instance.interfaces;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/wf/process/instances")
public class ProcessInstanceController {

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
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.start(body.toCommand())),
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

    @PutMapping("/{instanceId}/terminate")
    public ApiResponse<ProcessInstanceDtos.InstanceDetailResponse> terminate(
            @PathVariable UUID instanceId,
            @Valid @RequestBody ProcessInstanceDtos.TerminateProcessRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.terminate(body.toCommand(instanceId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/tasks/{taskId}/claim")
    public ApiResponse<ProcessInstanceDtos.TaskInstanceResponse> claim(
            @PathVariable UUID taskId,
            @Valid @RequestBody ProcessInstanceDtos.ClaimTaskRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toTaskResponse(applicationService.claim(body.toCommand(taskId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/tasks/{taskId}/transfer")
    public ApiResponse<ProcessInstanceDtos.TaskInstanceResponse> transfer(
            @PathVariable UUID taskId,
            @Valid @RequestBody ProcessInstanceDtos.TransferTaskRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toTaskResponse(applicationService.transfer(body.toCommand(taskId))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ApiResponse<ProcessInstanceDtos.InstanceDetailResponse> complete(
            @PathVariable UUID taskId,
            @Valid @RequestBody ProcessInstanceDtos.CompleteTaskRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.completeTask(body.toCommand(taskId))),
                responseMetaFactory.create(request)
        );
    }
}
