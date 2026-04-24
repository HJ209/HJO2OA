package com.hjo2oa.infra.errorcode.interfaces;

import com.hjo2oa.infra.errorcode.application.ErrorCodeDefinitionApplicationService;
import com.hjo2oa.infra.errorcode.domain.ErrorCodeDefinitionView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/infra/error-codes")
public class ErrorCodeDefinitionController {

    private final ErrorCodeDefinitionApplicationService applicationService;
    private final ErrorCodeDefinitionDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public ErrorCodeDefinitionController(
            ErrorCodeDefinitionApplicationService applicationService,
            ErrorCodeDefinitionDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping
    public ApiResponse<ErrorCodeDefinitionDtos.DetailResponse> defineCode(
            @Valid @RequestBody ErrorCodeDefinitionDtos.DefineRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.defineCode(dtoMapper.toDefineCommand(body))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{codeId}/deprecate")
    public ApiResponse<ErrorCodeDefinitionDtos.DetailResponse> deprecateCode(
            @PathVariable UUID codeId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.deprecateCode(codeId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{codeId}/severity")
    public ApiResponse<ErrorCodeDefinitionDtos.DetailResponse> updateSeverity(
            @PathVariable UUID codeId,
            @Valid @RequestBody ErrorCodeDefinitionDtos.UpdateSeverityRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.updateSeverity(codeId, body.severity())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{codeId}/http-status")
    public ApiResponse<ErrorCodeDefinitionDtos.DetailResponse> updateHttpStatus(
            @PathVariable UUID codeId,
            @Valid @RequestBody ErrorCodeDefinitionDtos.UpdateHttpStatusRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.updateHttpStatus(codeId, body.httpStatus())),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/code/{code}")
    public ApiResponse<ErrorCodeDefinitionDtos.DetailResponse> queryByCode(
            @PathVariable String code,
            HttpServletRequest request
    ) {
        ErrorCodeDefinitionView definition = applicationService.queryByCode(code)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "错误码不存在"));
        return ApiResponse.success(dtoMapper.toDetailResponse(definition), responseMetaFactory.create(request));
    }

    @GetMapping("/module/{moduleCode}")
    public ApiResponse<List<ErrorCodeDefinitionDtos.DetailResponse>> queryByModule(
            @PathVariable String moduleCode,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.queryByModule(moduleCode).stream().map(dtoMapper::toDetailResponse).toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping
    public ApiResponse<List<ErrorCodeDefinitionDtos.DetailResponse>> listAll(HttpServletRequest request) {
        return ApiResponse.success(
                applicationService.listAll().stream().map(dtoMapper::toDetailResponse).toList(),
                responseMetaFactory.create(request)
        );
    }
}
