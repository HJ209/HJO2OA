package com.hjo2oa.infra.attachment.application;

import com.hjo2oa.infra.attachment.domain.BindingRole;
import com.hjo2oa.infra.attachment.domain.PreviewStatus;
import com.hjo2oa.infra.attachment.domain.StorageProvider;
import java.util.Objects;
import java.util.UUID;

public final class AttachmentAssetCommands {

    private AttachmentAssetCommands() {
    }

    public record CreateAssetCommand(
            String storageKey,
            String originalFilename,
            String contentType,
            long sizeBytes,
            String checksum,
            StorageProvider storageProvider,
            UUID tenantId,
            UUID createdBy
    ) {

        public CreateAssetCommand {
            if (sizeBytes < 0) {
                throw new IllegalArgumentException("sizeBytes must not be negative");
            }
            Objects.requireNonNull(storageProvider, "storageProvider must not be null");
            Objects.requireNonNull(tenantId, "tenantId must not be null");
        }
    }

    public record AddVersionCommand(
            UUID assetId,
            String storageKey,
            String checksum,
            long sizeBytes,
            UUID createdBy
    ) {

        public AddVersionCommand {
            Objects.requireNonNull(assetId, "assetId must not be null");
            if (sizeBytes < 0) {
                throw new IllegalArgumentException("sizeBytes must not be negative");
            }
        }
    }

    public record BindToBusinessCommand(
            UUID assetId,
            String businessType,
            String businessId,
            BindingRole bindingRole
    ) {

        public BindToBusinessCommand {
            Objects.requireNonNull(assetId, "assetId must not be null");
            Objects.requireNonNull(bindingRole, "bindingRole must not be null");
        }
    }

    public record UnbindFromBusinessCommand(
            UUID assetId,
            UUID bindingId
    ) {

        public UnbindFromBusinessCommand {
            Objects.requireNonNull(assetId, "assetId must not be null");
            Objects.requireNonNull(bindingId, "bindingId must not be null");
        }
    }

    public record UpdatePreviewStatusCommand(
            UUID assetId,
            PreviewStatus status
    ) {

        public UpdatePreviewStatusCommand {
            Objects.requireNonNull(assetId, "assetId must not be null");
            Objects.requireNonNull(status, "status must not be null");
        }
    }
}
