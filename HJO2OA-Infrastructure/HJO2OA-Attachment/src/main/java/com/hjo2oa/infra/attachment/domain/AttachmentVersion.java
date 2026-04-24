package com.hjo2oa.infra.attachment.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AttachmentVersion(
        UUID id,
        UUID attachmentAssetId,
        int versionNo,
        String storageKey,
        String checksum,
        long sizeBytes,
        UUID createdBy,
        Instant createdAt
) {

    public AttachmentVersion {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(attachmentAssetId, "attachmentAssetId must not be null");
        if (versionNo <= 0) {
            throw new IllegalArgumentException("versionNo must be greater than 0");
        }
        storageKey = requireText(storageKey, "storageKey");
        checksum = requireText(checksum, "checksum");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static AttachmentVersion initial(
            UUID attachmentAssetId,
            String storageKey,
            String checksum,
            long sizeBytes,
            UUID createdBy,
            Instant createdAt
    ) {
        return create(attachmentAssetId, 1, storageKey, checksum, sizeBytes, createdBy, createdAt);
    }

    public static AttachmentVersion create(
            UUID attachmentAssetId,
            int versionNo,
            String storageKey,
            String checksum,
            long sizeBytes,
            UUID createdBy,
            Instant createdAt
    ) {
        return new AttachmentVersion(
                UUID.randomUUID(),
                attachmentAssetId,
                versionNo,
                storageKey,
                checksum,
                sizeBytes,
                createdBy,
                createdAt
        );
    }

    public AttachmentVersionView toView() {
        return new AttachmentVersionView(
                id,
                attachmentAssetId,
                versionNo,
                storageKey,
                checksum,
                sizeBytes,
                createdBy,
                createdAt
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
