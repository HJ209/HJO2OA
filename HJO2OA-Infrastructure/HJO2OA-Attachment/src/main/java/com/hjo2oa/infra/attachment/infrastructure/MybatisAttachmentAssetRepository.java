package com.hjo2oa.infra.attachment.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hjo2oa.infra.attachment.domain.AttachmentAsset;
import com.hjo2oa.infra.attachment.domain.AttachmentAssetRepository;
import com.hjo2oa.infra.attachment.domain.AttachmentBinding;
import com.hjo2oa.infra.attachment.domain.AttachmentVersion;
import com.hjo2oa.infra.attachment.domain.BindingRole;
import com.hjo2oa.infra.attachment.domain.PermissionMode;
import com.hjo2oa.infra.attachment.domain.PreviewStatus;
import com.hjo2oa.infra.attachment.domain.StorageProvider;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisAttachmentAssetRepository implements AttachmentAssetRepository {

    private final AttachmentAssetMapper attachmentAssetMapper;
    private final AttachmentVersionMapper attachmentVersionMapper;
    private final AttachmentBindingMapper attachmentBindingMapper;

    public MybatisAttachmentAssetRepository(
            AttachmentAssetMapper attachmentAssetMapper,
            AttachmentVersionMapper attachmentVersionMapper,
            AttachmentBindingMapper attachmentBindingMapper
    ) {
        this.attachmentAssetMapper = Objects.requireNonNull(
                attachmentAssetMapper,
                "attachmentAssetMapper must not be null"
        );
        this.attachmentVersionMapper = Objects.requireNonNull(
                attachmentVersionMapper,
                "attachmentVersionMapper must not be null"
        );
        this.attachmentBindingMapper = Objects.requireNonNull(
                attachmentBindingMapper,
                "attachmentBindingMapper must not be null"
        );
    }

    @Override
    public Optional<AttachmentAsset> findById(UUID id) {
        return Optional.ofNullable(attachmentAssetMapper.selectById(id.toString())).map(this::toDomain);
    }

    @Override
    public Optional<AttachmentAsset> findByStorageKey(String storageKey) {
        return Optional.ofNullable(attachmentAssetMapper.selectOne(
                new LambdaQueryWrapper<AttachmentAssetEntity>()
                        .eq(AttachmentAssetEntity::getStorageKey, storageKey)
        )).map(this::toDomain);
    }

    @Override
    public List<AttachmentAsset> findAllByBusiness(String businessType, String businessId) {
        List<String> assetIds = attachmentBindingMapper.selectList(
                        new LambdaQueryWrapper<AttachmentBindingEntity>()
                                .eq(AttachmentBindingEntity::getBusinessType, businessType)
                                .eq(AttachmentBindingEntity::getBusinessId, businessId)
                                .eq(AttachmentBindingEntity::getActive, true)
                ).stream()
                .map(AttachmentBindingEntity::getAttachmentAssetId)
                .distinct()
                .toList();
        if (assetIds.isEmpty()) {
            return List.of();
        }
        return attachmentAssetMapper.selectBatchIds(assetIds).stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(AttachmentAsset::updatedAt).reversed())
                .toList();
    }

    @Override
    public List<AttachmentAsset> findAllByTenant(UUID tenantId) {
        return attachmentAssetMapper.selectList(
                        new LambdaQueryWrapper<AttachmentAssetEntity>()
                                .eq(AttachmentAssetEntity::getTenantId, tenantId.toString())
                                .orderByDesc(AttachmentAssetEntity::getUpdatedAt)
                ).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long sumSizeBytesByTenant(UUID tenantId) {
        return attachmentAssetMapper.selectList(
                        new LambdaQueryWrapper<AttachmentAssetEntity>()
                                .eq(AttachmentAssetEntity::getTenantId, tenantId.toString())
                ).stream()
                .map(AttachmentAssetEntity::getSizeBytes)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
    }

    @Override
    @Transactional
    public AttachmentAsset save(AttachmentAsset attachmentAsset) {
        AttachmentAssetEntity assetEntity = toAssetEntity(attachmentAsset);
        if (attachmentAssetMapper.selectById(assetEntity.getId()) == null) {
            attachmentAssetMapper.insert(assetEntity);
        } else {
            attachmentAssetMapper.updateById(assetEntity);
        }

        attachmentVersionMapper.delete(new LambdaQueryWrapper<AttachmentVersionEntity>()
                .eq(AttachmentVersionEntity::getAttachmentAssetId, assetEntity.getId()));
        for (AttachmentVersion version : attachmentAsset.versions()) {
            attachmentVersionMapper.insert(toVersionEntity(version));
        }

        attachmentBindingMapper.delete(new LambdaQueryWrapper<AttachmentBindingEntity>()
                .eq(AttachmentBindingEntity::getAttachmentAssetId, assetEntity.getId()));
        for (AttachmentBinding binding : attachmentAsset.bindings()) {
            attachmentBindingMapper.insert(toBindingEntity(binding));
        }
        return attachmentAsset;
    }

    private AttachmentAsset toDomain(AttachmentAssetEntity assetEntity) {
        List<AttachmentVersion> versions = attachmentVersionMapper.selectList(
                        new LambdaQueryWrapper<AttachmentVersionEntity>()
                                .eq(AttachmentVersionEntity::getAttachmentAssetId, assetEntity.getId())
                                .orderByAsc(AttachmentVersionEntity::getVersionNo)
                ).stream()
                .map(this::toVersion)
                .toList();
        List<AttachmentBinding> bindings = attachmentBindingMapper.selectList(
                        new LambdaQueryWrapper<AttachmentBindingEntity>()
                                .eq(AttachmentBindingEntity::getAttachmentAssetId, assetEntity.getId())
                ).stream()
                .map(this::toBinding)
                .toList();
        return new AttachmentAsset(
                UUID.fromString(assetEntity.getId()),
                assetEntity.getStorageKey(),
                assetEntity.getOriginalFilename(),
                assetEntity.getContentType(),
                assetEntity.getSizeBytes() == null ? 0L : assetEntity.getSizeBytes(),
                assetEntity.getChecksum(),
                StorageProvider.valueOf(assetEntity.getStorageProvider()),
                PreviewStatus.valueOf(assetEntity.getPreviewStatus()),
                assetEntity.getLatestVersionNo(),
                PermissionMode.valueOf(assetEntity.getPermissionMode()),
                UUID.fromString(assetEntity.getTenantId()),
                toUuidNullable(assetEntity.getCreatedBy()),
                toInstant(assetEntity.getCreatedAt()),
                toInstant(assetEntity.getUpdatedAt()),
                versions,
                bindings
        );
    }

    private AttachmentVersion toVersion(AttachmentVersionEntity versionEntity) {
        return new AttachmentVersion(
                UUID.fromString(versionEntity.getId()),
                UUID.fromString(versionEntity.getAttachmentAssetId()),
                versionEntity.getVersionNo(),
                versionEntity.getStorageKey(),
                versionEntity.getChecksum(),
                versionEntity.getSizeBytes() == null ? 0L : versionEntity.getSizeBytes(),
                toUuidNullable(versionEntity.getCreatedBy()),
                toInstant(versionEntity.getCreatedAt())
        );
    }

    private AttachmentBinding toBinding(AttachmentBindingEntity bindingEntity) {
        return new AttachmentBinding(
                UUID.fromString(bindingEntity.getId()),
                UUID.fromString(bindingEntity.getAttachmentAssetId()),
                bindingEntity.getBusinessType(),
                bindingEntity.getBusinessId(),
                BindingRole.valueOf(bindingEntity.getBindingRole()),
                Boolean.TRUE.equals(bindingEntity.getActive())
        );
    }

    private AttachmentAssetEntity toAssetEntity(AttachmentAsset attachmentAsset) {
        AttachmentAssetEntity entity = new AttachmentAssetEntity();
        entity.setId(attachmentAsset.id().toString());
        entity.setStorageKey(attachmentAsset.storageKey());
        entity.setOriginalFilename(attachmentAsset.originalFilename());
        entity.setContentType(attachmentAsset.contentType());
        entity.setSizeBytes(attachmentAsset.sizeBytes());
        entity.setChecksum(attachmentAsset.checksum());
        entity.setStorageProvider(attachmentAsset.storageProvider().name());
        entity.setPreviewStatus(attachmentAsset.previewStatus().name());
        entity.setLatestVersionNo(attachmentAsset.latestVersionNo());
        entity.setPermissionMode(attachmentAsset.permissionMode().name());
        entity.setTenantId(attachmentAsset.tenantId().toString());
        entity.setCreatedBy(toStringNullable(attachmentAsset.createdBy()));
        entity.setCreatedAt(toLocalDateTime(attachmentAsset.createdAt()));
        entity.setUpdatedAt(toLocalDateTime(attachmentAsset.updatedAt()));
        return entity;
    }

    private AttachmentVersionEntity toVersionEntity(AttachmentVersion version) {
        AttachmentVersionEntity entity = new AttachmentVersionEntity();
        entity.setId(version.id().toString());
        entity.setAttachmentAssetId(version.attachmentAssetId().toString());
        entity.setVersionNo(version.versionNo());
        entity.setStorageKey(version.storageKey());
        entity.setChecksum(version.checksum());
        entity.setSizeBytes(version.sizeBytes());
        entity.setCreatedBy(toStringNullable(version.createdBy()));
        entity.setCreatedAt(toLocalDateTime(version.createdAt()));
        return entity;
    }

    private AttachmentBindingEntity toBindingEntity(AttachmentBinding binding) {
        AttachmentBindingEntity entity = new AttachmentBindingEntity();
        entity.setId(binding.id().toString());
        entity.setAttachmentAssetId(binding.attachmentAssetId().toString());
        entity.setBusinessType(binding.businessType());
        entity.setBusinessId(binding.businessId());
        entity.setBindingRole(binding.bindingRole().name());
        entity.setActive(binding.active());
        return entity;
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    private UUID toUuidNullable(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    private String toStringNullable(UUID value) {
        return value == null ? null : value.toString();
    }
}
