package com.hjo2oa.infra.attachment.interfaces;

import com.hjo2oa.infra.attachment.application.AttachmentAssetCommands;
import com.hjo2oa.infra.attachment.domain.BindingRole;
import com.hjo2oa.infra.attachment.domain.PermissionMode;
import com.hjo2oa.infra.attachment.domain.PreviewStatus;
import com.hjo2oa.infra.attachment.domain.StorageProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AttachmentAssetDtos {

    private AttachmentAssetDtos() {
    }

    public record CreateAssetRequest(
            @NotBlank @Size(max = 256) String storageKey,
            @NotBlank @Size(max = 256) String originalFilename,
            @NotBlank @Size(max = 128) String contentType,
            @PositiveOrZero long sizeBytes,
            @NotBlank @Size(max = 128) String checksum,
            @NotNull StorageProvider storageProvider,
            @NotNull UUID tenantId,
            UUID createdBy
    ) {

        public AttachmentAssetCommands.CreateAssetCommand toCommand() {
            return new AttachmentAssetCommands.CreateAssetCommand(
                    storageKey,
                    originalFilename,
                    contentType,
                    sizeBytes,
                    checksum,
                    storageProvider,
                    tenantId,
                    createdBy
            );
        }
    }

    public record AddVersionRequest(
            @NotBlank @Size(max = 256) String storageKey,
            @NotBlank @Size(max = 128) String checksum,
            @PositiveOrZero long sizeBytes,
            UUID createdBy
    ) {

        public AttachmentAssetCommands.AddVersionCommand toCommand(UUID assetId) {
            return new AttachmentAssetCommands.AddVersionCommand(assetId, storageKey, checksum, sizeBytes, createdBy);
        }
    }

    public record BindToBusinessRequest(
            @NotBlank @Size(max = 64) String businessType,
            @NotBlank @Size(max = 64) String businessId,
            @NotNull BindingRole bindingRole
    ) {

        public AttachmentAssetCommands.BindToBusinessCommand toCommand(UUID assetId) {
            return new AttachmentAssetCommands.BindToBusinessCommand(assetId, businessType, businessId, bindingRole);
        }
    }

    public record UpdatePreviewStatusRequest(
            @NotNull PreviewStatus status
    ) {

        public AttachmentAssetCommands.UpdatePreviewStatusCommand toCommand(UUID assetId) {
            return new AttachmentAssetCommands.UpdatePreviewStatusCommand(assetId, status);
        }
    }

    public record AttachmentVersionResponse(
            UUID id,
            UUID attachmentAssetId,
            int versionNo,
            String storageKey,
            String checksum,
            long sizeBytes,
            UUID createdBy,
            Instant createdAt
    ) {
    }

    public record AttachmentBindingResponse(
            UUID id,
            UUID attachmentAssetId,
            String businessType,
            String businessId,
            BindingRole bindingRole,
            boolean active
    ) {
    }

    public record AttachmentAssetResponse(
            UUID id,
            String storageKey,
            String originalFilename,
            String contentType,
            long sizeBytes,
            String checksum,
            StorageProvider storageProvider,
            PreviewStatus previewStatus,
            int latestVersionNo,
            PermissionMode permissionMode,
            UUID tenantId,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt,
            List<AttachmentVersionResponse> versions,
            List<AttachmentBindingResponse> bindings
    ) {
    }
}
