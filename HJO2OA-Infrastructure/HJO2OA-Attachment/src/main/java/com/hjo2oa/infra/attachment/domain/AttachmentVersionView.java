package com.hjo2oa.infra.attachment.domain;

import java.time.Instant;
import java.util.UUID;

public record AttachmentVersionView(
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
