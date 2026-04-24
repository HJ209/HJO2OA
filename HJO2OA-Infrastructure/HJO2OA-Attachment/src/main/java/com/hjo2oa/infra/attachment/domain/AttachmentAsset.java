package com.hjo2oa.infra.attachment.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

public record AttachmentAsset(
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
        List<AttachmentVersion> versions,
        List<AttachmentBinding> bindings
) {

    public AttachmentAsset {
        Objects.requireNonNull(id, "id must not be null");
        storageKey = requireText(storageKey, "storageKey");
        originalFilename = requireText(originalFilename, "originalFilename");
        contentType = requireText(contentType, "contentType");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
        checksum = requireText(checksum, "checksum");
        Objects.requireNonNull(storageProvider, "storageProvider must not be null");
        Objects.requireNonNull(previewStatus, "previewStatus must not be null");
        if (latestVersionNo <= 0) {
            throw new IllegalArgumentException("latestVersionNo must be greater than 0");
        }
        Objects.requireNonNull(permissionMode, "permissionMode must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        versions = List.copyOf(Objects.requireNonNull(versions, "versions must not be null"));
        bindings = List.copyOf(Objects.requireNonNullElse(bindings, List.of()));
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("versions must not be empty");
        }
        AttachmentVersion latestVersion = versions.stream()
                .max(Comparator.comparingInt(AttachmentVersion::versionNo))
                .orElseThrow();
        if (latestVersion.versionNo() != latestVersionNo) {
            throw new IllegalArgumentException("latestVersionNo must match latest version");
        }
        if (!latestVersion.storageKey().equals(storageKey)) {
            throw new IllegalArgumentException("storageKey must match latest version storage key");
        }
        if (!latestVersion.checksum().equals(checksum)) {
            throw new IllegalArgumentException("checksum must match latest version checksum");
        }
        if (latestVersion.sizeBytes() != sizeBytes) {
            throw new IllegalArgumentException("sizeBytes must match latest version size");
        }
    }

    public static AttachmentAsset create(
            String storageKey,
            String originalFilename,
            String contentType,
            long sizeBytes,
            String checksum,
            StorageProvider storageProvider,
            UUID tenantId,
            UUID createdBy,
            Instant now
    ) {
        UUID assetId = UUID.randomUUID();
        AttachmentVersion initialVersion = AttachmentVersion.initial(
                assetId,
                storageKey,
                checksum,
                sizeBytes,
                createdBy,
                now
        );
        return new AttachmentAsset(
                assetId,
                storageKey,
                originalFilename,
                contentType,
                sizeBytes,
                checksum,
                storageProvider,
                PreviewStatus.NONE,
                1,
                PermissionMode.INHERIT_BUSINESS,
                tenantId,
                createdBy,
                now,
                now,
                List.of(initialVersion),
                List.of()
        );
    }

    public AttachmentAsset addVersion(
            String storageKey,
            String checksum,
            long sizeBytes,
            UUID createdBy,
            Instant now
    ) {
        AttachmentVersion nextVersion = AttachmentVersion.create(
                id,
                latestVersionNo + 1,
                storageKey,
                checksum,
                sizeBytes,
                createdBy,
                now
        );
        return new AttachmentAsset(
                id,
                storageKey,
                originalFilename,
                contentType,
                sizeBytes,
                checksum,
                storageProvider,
                previewStatus,
                nextVersion.versionNo(),
                permissionMode,
                tenantId,
                this.createdBy,
                createdAt,
                now,
                append(versions, nextVersion),
                bindings
        );
    }

    public AttachmentAsset addBinding(
            String businessType,
            String businessId,
            BindingRole bindingRole,
            Instant now
    ) {
        AttachmentBinding existingBinding = bindings.stream()
                .filter(binding -> binding.matches(businessType, businessId, bindingRole))
                .findFirst()
                .orElse(null);
        if (existingBinding == null) {
            return new AttachmentAsset(
                    id,
                    storageKey,
                    originalFilename,
                    contentType,
                    sizeBytes,
                    checksum,
                    storageProvider,
                    previewStatus,
                    latestVersionNo,
                    permissionMode,
                    tenantId,
                    createdBy,
                    createdAt,
                    now,
                    versions,
                    append(bindings, AttachmentBinding.create(id, businessType, businessId, bindingRole))
            );
        }
        if (existingBinding.active()) {
            return this;
        }
        return replaceBinding(existingBinding.activate(), now);
    }

    public AttachmentAsset removeBinding(UUID bindingId, Instant now) {
        AttachmentBinding existingBinding = bindings.stream()
                .filter(binding -> binding.id().equals(bindingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Attachment binding not found"));
        if (!existingBinding.active()) {
            return this;
        }
        return replaceBinding(existingBinding.deactivate(), now);
    }

    public AttachmentAsset updatePreviewStatus(PreviewStatus previewStatus, Instant now) {
        Objects.requireNonNull(previewStatus, "previewStatus must not be null");
        if (this.previewStatus == previewStatus) {
            return this;
        }
        return new AttachmentAsset(
                id,
                storageKey,
                originalFilename,
                contentType,
                sizeBytes,
                checksum,
                storageProvider,
                previewStatus,
                latestVersionNo,
                permissionMode,
                tenantId,
                createdBy,
                createdAt,
                now,
                versions,
                bindings
        );
    }

    public AttachmentAssetView toView() {
        return new AttachmentAssetView(
                id,
                storageKey,
                originalFilename,
                contentType,
                sizeBytes,
                checksum,
                storageProvider,
                previewStatus,
                latestVersionNo,
                permissionMode,
                tenantId,
                createdBy,
                createdAt,
                updatedAt,
                versions.stream()
                        .sorted(Comparator.comparingInt(AttachmentVersion::versionNo))
                        .map(AttachmentVersion::toView)
                        .toList(),
                bindings.stream()
                        .map(AttachmentBinding::toView)
                        .toList()
        );
    }

    private AttachmentAsset replaceBinding(AttachmentBinding replacement, Instant now) {
        return new AttachmentAsset(
                id,
                storageKey,
                originalFilename,
                contentType,
                sizeBytes,
                checksum,
                storageProvider,
                previewStatus,
                latestVersionNo,
                permissionMode,
                tenantId,
                createdBy,
                createdAt,
                now,
                versions,
                bindings.stream()
                        .map(binding -> binding.id().equals(replacement.id()) ? replacement : binding)
                        .toList()
        );
    }

    private static <T> List<T> append(List<T> source, T value) {
        return Stream.concat(source.stream(), Stream.of(value)).toList();
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
