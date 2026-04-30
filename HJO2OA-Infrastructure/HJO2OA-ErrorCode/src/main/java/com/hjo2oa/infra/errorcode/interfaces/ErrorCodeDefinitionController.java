package com.hjo2oa.infra.errorcode.interfaces;

import com.hjo2oa.infra.errorcode.application.ErrorCodeDefinitionApplicationService;
import com.hjo2oa.infra.errorcode.application.ErrorCodeMessageLocalizer;
import com.hjo2oa.infra.errorcode.domain.ErrorCodeDefinitionView;
import com.hjo2oa.infra.errorcode.domain.ErrorSeverity;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/v1/infra/error-codes")
public class ErrorCodeDefinitionController {

    private final ErrorCodeDefinitionApplicationService applicationService;
    private final ErrorCodeDefinitionDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;
    private final ErrorCodeMessageLocalizer messageLocalizer;

    public ErrorCodeDefinitionController(
            ErrorCodeDefinitionApplicationService applicationService,
            ErrorCodeDefinitionDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this(applicationService, dtoMapper, responseMetaFactory, ErrorCodeMessageLocalizer.noop());
    }

    @Autowired
    public ErrorCodeDefinitionController(
            ErrorCodeDefinitionApplicationService applicationService,
            ErrorCodeDefinitionDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory,
            ObjectProvider<ErrorCodeMessageLocalizer> messageLocalizerProvider
    ) {
        this(
                applicationService,
                dtoMapper,
                responseMetaFactory,
                messageLocalizerProvider.getIfAvailable(ErrorCodeMessageLocalizer::noop)
        );
    }

    private ErrorCodeDefinitionController(
            ErrorCodeDefinitionApplicationService applicationService,
            ErrorCodeDefinitionDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory,
            ErrorCodeMessageLocalizer messageLocalizer
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
        this.messageLocalizer = messageLocalizer;
    }

    @PostMapping
    public ApiResponse<ErrorCodeDefinitionDtos.DetailResponse> defineCode(
            @Valid @RequestBody ErrorCodeDefinitionDtos.DefineRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                toDetailResponse(applicationService.defineCode(dtoMapper.toDefineCommand(body)), request),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{codeId}")
    public ApiResponse<ErrorCodeDefinitionDtos.DetailResponse> updateCode(
            @PathVariable UUID codeId,
            @Valid @RequestBody ErrorCodeDefinitionDtos.UpdateDefinitionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                toDetailResponse(
                        applicationService.updateDefinition(dtoMapper.toUpdateDefinitionCommand(codeId, body)),
                        request
                ),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{codeId}/deprecate")
    public ApiResponse<ErrorCodeDefinitionDtos.DetailResponse> deprecateCode(
            @PathVariable UUID codeId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                toDetailResponse(applicationService.deprecateCode(codeId), request),
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
                toDetailResponse(applicationService.updateSeverity(codeId, body.severity()), request),
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
                toDetailResponse(applicationService.updateHttpStatus(codeId, body.httpStatus()), request),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/code/{code}")
    public ApiResponse<ErrorCodeDefinitionDtos.DetailResponse> queryByCode(
            @PathVariable String code,
            HttpServletRequest request
    ) {
        ErrorCodeDefinitionView definition = applicationService.queryByCode(code)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Error code definition not found"
                ));
        return ApiResponse.success(toDetailResponse(definition, request), responseMetaFactory.create(request));
    }

    @GetMapping("/module/{moduleCode}")
    public ApiResponse<List<ErrorCodeDefinitionDtos.DetailResponse>> queryByModule(
            @PathVariable String moduleCode,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.queryByModule(moduleCode).stream()
                        .map(view -> toDetailResponse(view, request))
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping
    public ApiResponse<List<ErrorCodeDefinitionDtos.DetailResponse>> listAll(
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) ErrorSeverity severity,
            @RequestParam(required = false) Boolean deprecated,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listAll().stream()
                        .filter(view -> moduleCode == null || view.moduleCode().equals(moduleCode))
                        .filter(view -> severity == null || view.severity() == severity)
                        .filter(view -> deprecated == null || view.deprecated() == deprecated)
                        .map(view -> toDetailResponse(view, request))
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    private ErrorCodeDefinitionDtos.DetailResponse toDetailResponse(
            ErrorCodeDefinitionView view,
            HttpServletRequest request
    ) {
        String message = messageLocalizer.localize(view, resolveLocale(request), view.messageKey());
        return dtoMapper.toDetailResponse(view, message);
    }

    private String resolveLocale(HttpServletRequest request) {
        String locale = request == null ? null : request.getHeader("Accept-Language");
        if (locale == null || locale.isBlank()) {
            return "zh-CN";
        }
        return locale.split(",", 2)[0].trim();
    }
}
