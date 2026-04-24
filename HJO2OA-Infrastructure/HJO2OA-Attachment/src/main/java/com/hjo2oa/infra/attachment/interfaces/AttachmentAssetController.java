package com.hjo2oa.infra.attachment.interfaces;

import com.hjo2oa.infra.attachment.application.AttachmentAssetApplicationService;
import com.hjo2oa.infra.attachment.application.AttachmentAssetCommands;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/infra/attachments")
public class AttachmentAssetController {

    private final AttachmentAssetApplicationService applicationService;
    private final AttachmentAssetDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public AttachmentAssetController(
            AttachmentAssetApplicationService applicationService,
            AttachmentAssetDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping
    public ApiResponse<AttachmentAssetDtos.AttachmentAssetResponse> create(
            @Valid @RequestBody AttachmentAssetDtos.CreateAssetRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.createAsset(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{assetId}/versions")
    public ApiResponse<AttachmentAssetDtos.AttachmentAssetResponse> addVersion(
            @PathVariable UUID assetId,
            @Valid @RequestBody AttachmentAssetDtos.AddVersionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.addVersion(body.toCommand(assetId))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{assetId}/bindings")
    public ApiResponse<AttachmentAssetDtos.AttachmentAssetResponse> bindToBusiness(
            @PathVariable UUID assetId,
            @Valid @RequestBody AttachmentAssetDtos.BindToBusinessRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.bindToBusiness(body.toCommand(assetId))),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/{assetId}/bindings/{bindingId}")
    public ApiResponse<AttachmentAssetDtos.AttachmentAssetResponse> unbindFromBusiness(
            @PathVariable UUID assetId,
            @PathVariable UUID bindingId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.unbindFromBusiness(
                        new AttachmentAssetCommands.UnbindFromBusinessCommand(assetId, bindingId)
                )),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{assetId}/preview-status")
    public ApiResponse<AttachmentAssetDtos.AttachmentAssetResponse> updatePreviewStatus(
            @PathVariable UUID assetId,
            @Valid @RequestBody AttachmentAssetDtos.UpdatePreviewStatusRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.updatePreviewStatus(body.toCommand(assetId))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/business/{businessType}/{businessId}")
    public ApiResponse<List<AttachmentAssetDtos.AttachmentAssetResponse>> queryByBusiness(
            @PathVariable String businessType,
            @PathVariable String businessId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.queryByBusiness(businessType, businessId).stream()
                        .map(dtoMapper::toResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{assetId}")
    public ApiResponse<AttachmentAssetDtos.AttachmentAssetResponse> current(
            @PathVariable UUID assetId,
            HttpServletRequest request
    ) {
        AttachmentAssetDtos.AttachmentAssetResponse attachment = applicationService.current(assetId)
                .map(dtoMapper::toResponse)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Attachment asset not found"
                ));
        return ApiResponse.success(attachment, responseMetaFactory.create(request));
    }

    @GetMapping
    public ApiResponse<List<AttachmentAssetDtos.AttachmentAssetResponse>> queryByTenant(
            @RequestParam UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.queryByTenant(tenantId).stream()
                        .map(dtoMapper::toResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }
}
