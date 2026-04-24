package com.hjo2oa.infra.attachment.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AttachmentAssetView(
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
        List<AttachmentVersionView> versions,
        List<AttachmentBindingView> bindings
) {
}
