package com.hjo2oa.infra.attachment.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentAssetRepository {

    Optional<AttachmentAsset> findById(UUID id);

    Optional<AttachmentAsset> findByStorageKey(String storageKey);

    List<AttachmentAsset> findAllByBusiness(String businessType, String businessId);

    List<AttachmentAsset> findAllByTenant(UUID tenantId);

    long sumSizeBytesByTenant(UUID tenantId);

    AttachmentAsset save(AttachmentAsset attachmentAsset);
}
