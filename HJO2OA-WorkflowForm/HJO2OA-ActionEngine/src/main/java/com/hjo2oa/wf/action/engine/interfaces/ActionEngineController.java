package com.hjo2oa.wf.action.engine.interfaces;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import com.hjo2oa.wf.action.engine.application.ActionEngineApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/process")
public class ActionEngineController {

    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private final ActionEngineApplicationService applicationService;
    private final ActionEngineDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public ActionEngineController(
            ActionEngineApplicationService applicationService,
            ActionEngineDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/tasks/{taskId}/actions")
    public ApiResponse<List<ActionEngineDtos.ActionDefinitionResponse>> availableActions(
            @PathVariable UUID taskId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.availableActions(taskId).stream()
                        .map(dtoMapper::toActionDefinitionResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/tasks/{taskId}/actions/execute")
    public ApiResponse<ActionEngineDtos.ExecuteActionResponse> execute(
            @PathVariable UUID taskId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody ActionEngineDtos.ExecuteActionRequest body,
            HttpServletRequest request
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "X-Idempotency-Key is required");
        }
        return ApiResponse.success(
                dtoMapper.toExecuteActionResponse(applicationService.execute(body.toCommand(taskId, idempotencyKey))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/action-records")
    public ApiResponse<List<ActionEngineDtos.TaskActionResponse>> records(
            @RequestParam UUID taskId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.records(taskId).stream()
                        .map(dtoMapper::toTaskActionResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }
}
