package com.hjo2oa.infra.attachment.interfaces;

import com.hjo2oa.infra.attachment.application.AttachmentAssetApplicationService;
import com.hjo2oa.infra.attachment.application.AttachmentAssetCommands;
import com.hjo2oa.infra.attachment.application.AttachmentAuthorizationService;
import com.hjo2oa.infra.attachment.application.AttachmentFileApplicationService;
import com.hjo2oa.infra.attachment.domain.BindingRole;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.tenant.TenantContextHolder;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/infra/attachments")
public class AttachmentAssetController {

    private final AttachmentAssetApplicationService applicationService;
    private final AttachmentFileApplicationService fileApplicationService;
    private final AttachmentAuthorizationService authorizationService;
    private final AttachmentAssetDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public AttachmentAssetController(
            AttachmentAssetApplicationService applicationService,
            AttachmentAssetDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this(applicationService, null, new AttachmentAuthorizationService(), dtoMapper, responseMetaFactory);
    }

    @Autowired
    public AttachmentAssetController(
            AttachmentAssetApplicationService applicationService,
            AttachmentFileApplicationService fileApplicationService,
            AttachmentAuthorizationService authorizationService,
            AttachmentAssetDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.fileApplicationService = fileApplicationService;
        this.authorizationService = authorizationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AttachmentAssetDtos.AttachmentAssetResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessId,
            @RequestParam(required = false, defaultValue = "ATTACHMENT") BindingRole bindingRole,
            HttpServletRequest request
    ) throws java.io.IOException {
        return ApiResponse.success(
                dtoMapper.toResponse(fileApplicationService.upload(new AttachmentFileApplicationService.UploadCommand(
                        file.getOriginalFilename(),
                        resolveContentType(file),
                        file.getSize(),
                        file.getInputStream(),
                        tenantId,
                        operatorId(request),
                        businessType,
                        businessId,
                        bindingRole,
                        clientIp(request)
                ))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<AttachmentAssetDtos.AttachmentAssetResponse> create(
            @Valid @RequestBody AttachmentAssetDtos.CreateAssetRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.createAsset(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping(path = "/{assetId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AttachmentAssetDtos.AttachmentAssetResponse> uploadVersion(
            @PathVariable UUID assetId,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request
    ) throws java.io.IOException {
        return ApiResponse.success(
                dtoMapper.toResponse(fileApplicationService.uploadVersion(
                        new AttachmentFileApplicationService.UploadVersionCommand(
                                assetId,
                                file.getOriginalFilename(),
                                resolveContentType(file),
                                file.getSize(),
                                file.getInputStream(),
                                operatorId(request),
                                clientIp(request)
                        )
                )),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping(path = "/{assetId}/versions", consumes = MediaType.APPLICATION_JSON_VALUE)
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

    @GetMapping("/{assetId}/versions")
    public ApiResponse<List<AttachmentAssetDtos.AttachmentVersionResponse>> versions(
            @PathVariable UUID assetId,
            HttpServletRequest request
    ) {
        var attachment = applicationService.current(assetId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Attachment asset not found"
                ));
        return ApiResponse.success(
                attachment.versions().stream().map(dtoMapper::toResponse).toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{assetId}/preview")
    public ApiResponse<AttachmentAssetDtos.AttachmentPreviewResponse> preview(
            @PathVariable UUID assetId,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(fileApplicationService.preview(
                        assetId,
                        businessType,
                        businessId,
                        operatorId(request),
                        clientIp(request)
                )),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{assetId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            @PathVariable UUID assetId,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessId,
            HttpServletRequest request
    ) {
        return toDownloadResponse(fileApplicationService.download(
                assetId,
                null,
                businessType,
                businessId,
                operatorId(request),
                clientIp(request)
        ));
    }

    @GetMapping("/{assetId}/versions/{versionNo}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadVersion(
            @PathVariable UUID assetId,
            @PathVariable int versionNo,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessId,
            HttpServletRequest request
    ) {
        return toDownloadResponse(fileApplicationService.download(
                assetId,
                versionNo,
                businessType,
                businessId,
                operatorId(request),
                clientIp(request)
        ));
    }

    @GetMapping("/{assetId}/access-audits")
    public ApiResponse<List<AttachmentAssetDtos.AttachmentAccessAuditResponse>> accessAudits(
            @PathVariable UUID assetId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                fileApplicationService.audit(assetId).stream()
                        .map(dtoMapper::toResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping
    public ApiResponse<List<AttachmentAssetDtos.AttachmentAssetResponse>> queryByTenant(
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        UUID resolvedTenantId = authorizationService.resolveRequiredTenant(tenantId);
        return ApiResponse.success(
                applicationService.queryByTenant(resolvedTenantId).stream()
                        .map(dtoMapper::toResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType == null || contentType.isBlank() ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType;
    }

    private ResponseEntity<org.springframework.core.io.Resource> toDownloadResponse(
            AttachmentFileApplicationService.DownloadedAttachment downloadedAttachment
    ) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(downloadedAttachment.contentType()))
                .contentLength(downloadedAttachment.sizeBytes())
                .header(HttpHeaders.ETAG, "\"" + downloadedAttachment.checksum() + "\"")
                .header("X-Attachment-Version", String.valueOf(downloadedAttachment.versionNo()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(downloadedAttachment.originalFilename(), java.nio.charset.StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(downloadedAttachment.resource());
    }

    private UUID operatorId(HttpServletRequest request) {
        return parseUuid(request.getHeader("X-Identity-Assignment-Id"));
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
