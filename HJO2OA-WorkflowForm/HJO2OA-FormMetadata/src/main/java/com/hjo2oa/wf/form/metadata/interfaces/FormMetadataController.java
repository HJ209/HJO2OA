package com.hjo2oa.wf.form.metadata.interfaces;

import com.hjo2oa.wf.form.metadata.application.FormMetadataApplicationService;
import com.hjo2oa.wf.form.metadata.application.FormMetadataCommands;
import com.hjo2oa.wf.form.metadata.domain.FormFieldDefinition;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/form")
public class FormMetadataController {

    private final FormMetadataApplicationService applicationService;
    private final FormMetadataDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public FormMetadataController(
            FormMetadataApplicationService applicationService,
            FormMetadataDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/metadata")
    public ApiResponse<FormMetadataDtos.FormMetadataDetailResponse> create(
            @Valid @RequestBody FormMetadataDtos.CreateRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.create(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/metadata")
    public ApiResponse<List<FormMetadataDtos.FormMetadataResponse>> query(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) FormMetadataStatus status,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.query(new FormMetadataCommands.FormMetadataQuery(tenantId, code, status)).stream()
                        .map(dtoMapper::toResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/metadata/{metadataId}")
    public ApiResponse<FormMetadataDtos.FormMetadataDetailResponse> get(
            @PathVariable UUID metadataId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.get(metadataId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/metadata/{metadataId}")
    public ApiResponse<FormMetadataDtos.FormMetadataDetailResponse> update(
            @PathVariable UUID metadataId,
            @Valid @RequestBody FormMetadataDtos.UpdateRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.update(body.toCommand(metadataId))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/metadata/{metadataId}/validate")
    public ApiResponse<List<FormFieldDefinition>> validate(
            @PathVariable UUID metadataId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.fields(metadataId),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/metadata/{metadataId}/publish")
    public ApiResponse<FormMetadataDtos.FormMetadataDetailResponse> publish(
            @PathVariable UUID metadataId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.publish(metadataId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/metadata/{metadataId}/deprecate")
    public ApiResponse<FormMetadataDtos.FormMetadataDetailResponse> deprecate(
            @PathVariable UUID metadataId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.deprecate(metadataId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/metadata/{metadataId}/versions")
    public ApiResponse<FormMetadataDtos.FormMetadataDetailResponse> deriveNewVersion(
            @PathVariable UUID metadataId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.deriveNewVersion(metadataId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/metadata/{code}/versions")
    public ApiResponse<List<FormMetadataDtos.FormMetadataResponse>> versions(
            @PathVariable String code,
            @RequestParam UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.versions(tenantId, code).stream()
                        .map(dtoMapper::toResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/render-schemas/{code}")
    public ApiResponse<FormMetadataDtos.RenderSchemaResponse> latestRenderSchema(
            @PathVariable String code,
            @RequestParam UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toRenderSchemaResponse(applicationService.latestRenderSchema(tenantId, code)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/render-schemas/{code}/versions/{version}")
    public ApiResponse<FormMetadataDtos.RenderSchemaResponse> renderSchema(
            @PathVariable String code,
            @PathVariable int version,
            @RequestParam UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toRenderSchemaResponse(applicationService.renderSchema(tenantId, code, version)),
                responseMetaFactory.create(request)
        );
    }
}
