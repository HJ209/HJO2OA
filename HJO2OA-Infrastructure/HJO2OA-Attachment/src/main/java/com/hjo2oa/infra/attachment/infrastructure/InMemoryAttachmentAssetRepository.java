package com.hjo2oa.infra.attachment.infrastructure;

import com.hjo2oa.infra.attachment.domain.AttachmentAsset;
import com.hjo2oa.infra.attachment.domain.AttachmentAssetRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryAttachmentAssetRepository implements AttachmentAssetRepository {

    private final Map<UUID, AttachmentAsset> assets = new ConcurrentHashMap<>();

    @Override
    public Optional<AttachmentAsset> findById(UUID id) {
        return Optional.ofNullable(assets.get(id));
    }

    @Override
    public Optional<AttachmentAsset> findByStorageKey(String storageKey) {
        return assets.values().stream()
                .filter(asset -> asset.storageKey().equals(storageKey))
                .findFirst();
    }

    @Override
    public List<AttachmentAsset> findAllByBusiness(String businessType, String businessId) {
        return assets.values().stream()
                .filter(asset -> asset.bindings().stream()
                        .anyMatch(binding -> binding.active()
                                && binding.businessType().equals(businessType)
                                && binding.businessId().equals(businessId)))
                .sorted(Comparator.comparing(AttachmentAsset::updatedAt).reversed())
                .toList();
    }

    @Override
    public List<AttachmentAsset> findAllByTenant(UUID tenantId) {
        return assets.values().stream()
                .filter(asset -> asset.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(AttachmentAsset::updatedAt).reversed())
                .toList();
    }

    @Override
    public long sumSizeBytesByTenant(UUID tenantId) {
        return assets.values().stream()
                .filter(asset -> asset.tenantId().equals(tenantId))
                .mapToLong(AttachmentAsset::sizeBytes)
                .sum();
    }

    @Override
    public AttachmentAsset save(AttachmentAsset attachmentAsset) {
        assets.put(attachmentAsset.id(), attachmentAsset);
        return attachmentAsset;
    }
}
