package com.hjo2oa.data.service.interfaces;

import com.hjo2oa.data.service.application.DataServiceInvocationApplicationService;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/data-services/runtime/{serviceCode}")
public class DataServiceInvocationController {

    private final DataServiceInvocationApplicationService applicationService;
    private final DataServiceDefinitionDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public DataServiceInvocationController(
            DataServiceInvocationApplicationService applicationService,
            DataServiceDefinitionDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/query")
    public ApiResponse<DataServiceInvocationDtos.ExecutionPlanResponse> query(
            @PathVariable String serviceCode,
            @RequestHeader(name = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody(required = false) DataServiceInvocationDtos.InvocationRequest body,
            HttpServletRequest request
    ) {
        DataServiceInvocationDtos.InvocationRequest payload =
                body == null ? new DataServiceInvocationDtos.InvocationRequest(null, null, null) : body;
        return ApiResponse.success(
                dtoMapper.toExecutionPlanResponse(applicationService.query(payload.toCommand(serviceCode, idempotencyKey))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/submit")
    public ApiResponse<DataServiceInvocationDtos.ExecutionPlanResponse> submit(
            @PathVariable String serviceCode,
            @RequestHeader(name = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody(required = false) DataServiceInvocationDtos.InvocationRequest body,
            HttpServletRequest request
    ) {
        DataServiceInvocationDtos.InvocationRequest payload =
                body == null ? new DataServiceInvocationDtos.InvocationRequest(null, null, null) : body;
        return ApiResponse.success(
                dtoMapper.toExecutionPlanResponse(applicationService.submit(payload.toCommand(serviceCode, idempotencyKey))),
                responseMetaFactory.create(request)
        );
    }
}
