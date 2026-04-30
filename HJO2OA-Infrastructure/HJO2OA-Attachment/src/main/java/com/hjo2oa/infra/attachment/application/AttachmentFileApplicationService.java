package com.hjo2oa.infra.attachment.application;

import com.hjo2oa.infra.attachment.domain.AttachmentAsset;
import com.hjo2oa.infra.attachment.domain.AttachmentAssetRepository;
import com.hjo2oa.infra.attachment.domain.AttachmentAssetView;
import com.hjo2oa.infra.attachment.domain.AttachmentVersion;
import com.hjo2oa.infra.attachment.domain.BindingRole;
import com.hjo2oa.infra.attachment.domain.PreviewStatus;
import com.hjo2oa.infra.attachment.domain.StorageProvider;
import com.hjo2oa.shared.kernel.BizException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AttachmentFileApplicationService {

    private static final DateTimeFormatter STORAGE_DATE =
            DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);

    private final AttachmentAssetApplicationService assetApplicationService;
    private final AttachmentAssetRepository attachmentAssetRepository;
    private final AttachmentStorageAdapterRegistry storageAdapterRegistry;
    private final AttachmentUploadPolicy uploadPolicy;
    private final AttachmentVirusScanner virusScanner;
    private final AttachmentAuthorizationService authorizationService;
    private final AttachmentAccessAuditService auditService;
    private final Clock clock;

    @Autowired
    public AttachmentFileApplicationService(
            AttachmentAssetApplicationService assetApplicationService,
            AttachmentAssetRepository attachmentAssetRepository,
            AttachmentStorageAdapterRegistry storageAdapterRegistry,
            AttachmentUploadPolicy uploadPolicy,
            AttachmentVirusScanner virusScanner,
            AttachmentAuthorizationService authorizationService,
            AttachmentAccessAuditService auditService
    ) {
        this(
                assetApplicationService,
                attachmentAssetRepository,
                storageAdapterRegistry,
                uploadPolicy,
                virusScanner,
                authorizationService,
                auditService,
                Clock.systemUTC()
        );
    }

    AttachmentFileApplicationService(
            AttachmentAssetApplicationService assetApplicationService,
            AttachmentAssetRepository attachmentAssetRepository,
            AttachmentStorageAdapterRegistry storageAdapterRegistry,
            AttachmentUploadPolicy uploadPolicy,
            AttachmentVirusScanner virusScanner,
            AttachmentAuthorizationService authorizationService,
            AttachmentAccessAuditService auditService,
            Clock clock
    ) {
        this.assetApplicationService = Objects.requireNonNull(
                assetApplicationService,
                "assetApplicationService must not be null"
        );
        this.attachmentAssetRepository = Objects.requireNonNull(
                attachmentAssetRepository,
                "attachmentAssetRepository must not be null"
        );
        this.storageAdapterRegistry = Objects.requireNonNull(storageAdapterRegistry, "storageAdapterRegistry must not be null");
        this.uploadPolicy = Objects.requireNonNull(uploadPolicy, "uploadPolicy must not be null");
        this.virusScanner = Objects.requireNonNull(virusScanner, "virusScanner must not be null");
        this.authorizationService = Objects.requireNonNull(
                authorizationService,
                "authorizationService must not be null"
        );
        this.auditService = Objects.requireNonNull(auditService, "auditService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public AttachmentAssetView upload(UploadCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        UUID tenantId = authorizationService.resolveRequiredTenant(command.tenantId());
        uploadPolicy.validate(command.contentType(), command.sizeBytes());
        String storageKey = buildStorageKey(tenantId, UUID.randomUUID(), 1, command.originalFilename());
        StoredAttachment stored = storageAdapterRegistry.require(StorageProvider.LOCAL).store(storageKey, command.content());
        scanStoredObject(stored, command.originalFilename(), command.contentType());
        AttachmentAssetView created = assetApplicationService.createAsset(new AttachmentAssetCommands.CreateAssetCommand(
                stored.storageKey(),
                command.originalFilename(),
                command.contentType(),
                stored.sizeBytes(),
                stored.checksum(),
                stored.storageProvider(),
                tenantId,
                command.createdBy()
        ));
        AttachmentAssetView current = created;
        if (command.businessType() != null && command.businessId() != null && command.bindingRole() != null) {
            current = assetApplicationService.bindToBusiness(new AttachmentAssetCommands.BindToBusinessCommand(
                    created.id(),
                    command.businessType(),
                    command.businessId(),
                    command.bindingRole()
            ));
        }
        PreviewStatus previewStatus = initialPreviewStatus(command.contentType());
        if (previewStatus != PreviewStatus.NONE) {
            current = assetApplicationService.updatePreviewStatus(new AttachmentAssetCommands.UpdatePreviewStatusCommand(
                    created.id(),
                    previewStatus
            ));
        }
        auditService.record(created.id(), 1, "UPLOAD", command.createdBy(), command.clientIp());
        return current;
    }

    public AttachmentAssetView uploadVersion(UploadVersionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        AttachmentAsset asset = requireAsset(command.assetId());
        authorizationService.assertTenantAccess(asset);
        uploadPolicy.validate(command.contentType(), command.sizeBytes());
        String storageKey = buildStorageKey(
                asset.tenantId(),
                asset.id(),
                asset.latestVersionNo() + 1,
                command.originalFilename()
        );
        StoredAttachment stored = storageAdapterRegistry.require(asset.storageProvider()).store(storageKey, command.content());
        scanStoredObject(stored, command.originalFilename(), command.contentType());
        AttachmentAssetView updated = assetApplicationService.addVersion(new AttachmentAssetCommands.AddVersionCommand(
                asset.id(),
                stored.storageKey(),
                stored.checksum(),
                stored.sizeBytes(),
                command.createdBy()
        ));
        auditService.record(asset.id(), updated.latestVersionNo(), "UPLOAD_VERSION", command.createdBy(), command.clientIp());
        return updated;
    }

    public DownloadedAttachment download(
            UUID assetId,
            Integer versionNo,
            String businessType,
            String businessId,
            UUID operatorId,
            String clientIp
    ) {
        AttachmentAsset asset = requireAsset(assetId);
        authorizationService.assertBusinessAccess(asset, businessType, businessId);
        AttachmentVersion version = resolveVersion(asset, versionNo);
        StoredAttachmentResource resource = storageAdapterRegistry.require(asset.storageProvider()).load(version.storageKey());
        auditService.record(asset.id(), version.versionNo(), "DOWNLOAD", operatorId, clientIp);
        return new DownloadedAttachment(
                asset.originalFilename(),
                asset.contentType(),
                resource.sizeBytes(),
                version.checksum(),
                version.versionNo(),
                resource.resource()
        );
    }

    public PreviewInfo preview(UUID assetId, String businessType, String businessId, UUID operatorId, String clientIp) {
        AttachmentAsset asset = requireAsset(assetId);
        authorizationService.assertBusinessAccess(asset, businessType, businessId);
        auditService.record(asset.id(), asset.latestVersionNo(), "PREVIEW", operatorId, clientIp);
        return new PreviewInfo(
                asset.id(),
                asset.previewStatus(),
                previewAvailable(asset.contentType()),
                asset.contentType(),
                "/api/v1/infra/attachments/" + asset.id() + "/download"
        );
    }

    public List<AttachmentAccessAuditRecord> audit(UUID assetId) {
        return auditService.list(assetId);
    }

    private AttachmentAsset requireAsset(UUID assetId) {
        return attachmentAssetRepository.findById(assetId)
                .orElseThrow(() -> new BizException(
                        AttachmentErrorDescriptors.ATTACHMENT_NOT_FOUND,
                        "Attachment asset not found: " + assetId
                ));
    }

    private AttachmentVersion resolveVersion(AttachmentAsset asset, Integer versionNo) {
        int resolvedVersionNo = versionNo == null ? asset.latestVersionNo() : versionNo;
        return asset.versions().stream()
                .filter(version -> version.versionNo() == resolvedVersionNo)
                .findFirst()
                .orElseThrow(() -> new BizException(
                        AttachmentErrorDescriptors.ATTACHMENT_NOT_FOUND,
                        "Attachment version not found: " + resolvedVersionNo
                ));
    }

    private void scanStoredObject(StoredAttachment stored, String originalFilename, String contentType) {
        Path localPath = stored.localPath();
        if (localPath == null) {
            return;
        }
        AttachmentVirusScanner.ScanResult scanResult = virusScanner.scan(localPath, originalFilename, contentType);
        if (!scanResult.clean()) {
            storageAdapterRegistry.require(stored.storageProvider()).delete(stored.storageKey());
            throw new BizException(
                    AttachmentErrorDescriptors.ATTACHMENT_VIRUS_SCAN_REJECTED,
                    "Attachment rejected by virus scanner: " + scanResult.details()
            );
        }
    }

    private String buildStorageKey(UUID tenantId, UUID assetId, int versionNo, String originalFilename) {
        return tenantId + "/" + STORAGE_DATE.format(clock.instant()) + "/" + assetId + "/v" + versionNo
                + "/" + sanitizeFilename(originalFilename);
    }

    private String sanitizeFilename(String originalFilename) {
        String normalized = requireText(originalFilename, "originalFilename")
                .replace("\\", "_")
                .replace("/", "_");
        return normalized.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private PreviewStatus initialPreviewStatus(String contentType) {
        return previewAvailable(contentType) ? PreviewStatus.READY : PreviewStatus.NONE;
    }

    private boolean previewAvailable(String contentType) {
        String normalized = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return normalized.startsWith("image/")
                || normalized.equals("application/pdf")
                || normalized.startsWith("text/");
    }

    private String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    public record UploadCommand(
            String originalFilename,
            String contentType,
            long sizeBytes,
            InputStream content,
            UUID tenantId,
            UUID createdBy,
            String businessType,
            String businessId,
            BindingRole bindingRole,
            String clientIp
    ) {
    }

    public record UploadVersionCommand(
            UUID assetId,
            String originalFilename,
            String contentType,
            long sizeBytes,
            InputStream content,
            UUID createdBy,
            String clientIp
    ) {
    }

    public record DownloadedAttachment(
            String originalFilename,
            String contentType,
            long sizeBytes,
            String checksum,
            int versionNo,
            org.springframework.core.io.Resource resource
    ) {
    }

    public record PreviewInfo(
            UUID assetId,
            PreviewStatus previewStatus,
            boolean previewAvailable,
            String contentType,
            String downloadUrl
    ) {
    }
}
