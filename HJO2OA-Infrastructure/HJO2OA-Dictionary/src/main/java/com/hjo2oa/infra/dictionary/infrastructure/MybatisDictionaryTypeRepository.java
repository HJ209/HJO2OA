package com.hjo2oa.infra.dictionary.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.infra.dictionary.domain.DictionaryItem;
import com.hjo2oa.infra.dictionary.domain.DictionaryStatus;
import com.hjo2oa.infra.dictionary.domain.DictionaryType;
import com.hjo2oa.infra.dictionary.domain.DictionaryTypeRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class MybatisDictionaryTypeRepository implements DictionaryTypeRepository {

    private final DictionaryTypeMapper dictionaryTypeMapper;
    private final DictionaryItemMapper dictionaryItemMapper;

    public MybatisDictionaryTypeRepository(
            DictionaryTypeMapper dictionaryTypeMapper,
            DictionaryItemMapper dictionaryItemMapper
    ) {
        this.dictionaryTypeMapper = Objects.requireNonNull(dictionaryTypeMapper, "dictionaryTypeMapper must not be null");
        this.dictionaryItemMapper = Objects.requireNonNull(dictionaryItemMapper, "dictionaryItemMapper must not be null");
    }

    @Override
    public Optional<DictionaryType> findById(UUID typeId) {
        DictionaryTypeEntity entity = dictionaryTypeMapper.selectById(typeId);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(loadAggregate(entity.getId()));
    }

    @Override
    public Optional<DictionaryType> findByCode(UUID tenantId, String code) {
        LambdaQueryWrapper<DictionaryTypeEntity> wrapper = Wrappers.lambdaQuery();
        applyTenantScope(wrapper, tenantId);
        wrapper.eq(DictionaryTypeEntity::getCode, code);
        DictionaryTypeEntity entity = dictionaryTypeMapper.selectOne(wrapper);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(loadAggregate(entity.getId()));
    }

    @Override
    public List<DictionaryType> findByTenant(UUID tenantId) {
        LambdaQueryWrapper<DictionaryTypeEntity> wrapper = Wrappers.lambdaQuery();
        applyTenantScope(wrapper, tenantId);
        wrapper.orderByAsc(DictionaryTypeEntity::getSortOrder)
                .orderByAsc(DictionaryTypeEntity::getCode)
                .orderByAsc(DictionaryTypeEntity::getUpdatedAt);
        return dictionaryTypeMapper.selectList(wrapper).stream()
                .map(entity -> loadAggregate(entity.getId()))
                .toList();
    }

    @Override
    public DictionaryType save(DictionaryType type) {
        DictionaryTypeEntity existingType = dictionaryTypeMapper.selectById(type.id());
        DictionaryTypeEntity entity = toTypeEntity(type, existingType);
        if (existingType == null) {
            dictionaryTypeMapper.insert(entity);
        } else {
            dictionaryTypeMapper.updateById(entity);
        }
        syncItems(type);
        return loadAggregate(type.id());
    }

    @Override
    public List<DictionaryType> findAllActive() {
        LambdaQueryWrapper<DictionaryTypeEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DictionaryTypeEntity::getStatus, DictionaryStatus.ACTIVE.name())
                .orderByAsc(DictionaryTypeEntity::getSortOrder)
                .orderByAsc(DictionaryTypeEntity::getCode)
                .orderByAsc(DictionaryTypeEntity::getUpdatedAt);
        return dictionaryTypeMapper.selectList(wrapper).stream()
                .map(entity -> loadAggregate(entity.getId()))
                .toList();
    }

    private DictionaryType loadAggregate(UUID typeId) {
        DictionaryTypeEntity typeEntity = dictionaryTypeMapper.selectById(typeId);
        if (typeEntity == null) {
            throw new IllegalStateException("Dictionary type not found: " + typeId);
        }
        List<DictionaryItemEntity> itemEntities = dictionaryItemMapper.selectList(
                Wrappers.<DictionaryItemEntity>lambdaQuery()
                        .eq(DictionaryItemEntity::getDictionaryTypeId, typeId)
                        .orderByAsc(DictionaryItemEntity::getSortOrder)
                        .orderByAsc(DictionaryItemEntity::getItemCode)
        );
        return toAggregate(typeEntity, itemEntities);
    }

    private void syncItems(DictionaryType type) {
        List<DictionaryItemEntity> existingItems = dictionaryItemMapper.selectList(
                Wrappers.<DictionaryItemEntity>lambdaQuery()
                        .eq(DictionaryItemEntity::getDictionaryTypeId, type.id())
        );
        Map<UUID, DictionaryItemEntity> existingById = existingItems.stream()
                .collect(Collectors.toMap(DictionaryItemEntity::getId, Function.identity()));
        List<UUID> retainedIds = new ArrayList<>();
        for (DictionaryItem item : type.items()) {
            DictionaryItemEntity existingItem = existingById.get(item.id());
            DictionaryItemEntity entity = toItemEntity(item, existingItem);
            retainedIds.add(item.id());
            if (existingItem == null) {
                dictionaryItemMapper.insert(entity);
            } else {
                dictionaryItemMapper.updateById(entity);
            }
        }
        for (DictionaryItemEntity existingItem : existingItems) {
            if (!retainedIds.contains(existingItem.getId())) {
                dictionaryItemMapper.deleteById(existingItem.getId());
            }
        }
    }

    private DictionaryType toAggregate(
            DictionaryTypeEntity typeEntity,
            List<DictionaryItemEntity> itemEntities
    ) {
        return new DictionaryType(
                typeEntity.getId(),
                typeEntity.getCode(),
                typeEntity.getName(),
                typeEntity.getCategory(),
                Boolean.TRUE.equals(typeEntity.getHierarchical()),
                Boolean.TRUE.equals(typeEntity.getCacheable()),
                typeEntity.getSortOrder() == null ? 0 : typeEntity.getSortOrder(),
                Boolean.TRUE.equals(typeEntity.getSystemManaged()),
                DictionaryStatus.valueOf(typeEntity.getStatus()),
                typeEntity.getTenantId(),
                typeEntity.getCreatedAt(),
                typeEntity.getUpdatedAt(),
                itemEntities.stream()
                        .map(this::toDomainItem)
                        .sorted(Comparator.comparingInt(DictionaryItem::sortOrder)
                                .thenComparing(DictionaryItem::itemCode))
                        .toList()
        );
    }

    private DictionaryItem toDomainItem(DictionaryItemEntity entity) {
        return new DictionaryItem(
                entity.getId(),
                entity.getDictionaryTypeId(),
                entity.getItemCode(),
                entity.getDisplayName(),
                entity.getParentItemId(),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder(),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getMultiLangValue(),
                Boolean.TRUE.equals(entity.getDefaultItem()),
                entity.getExtensionJson()
        );
    }

    private DictionaryTypeEntity toTypeEntity(DictionaryType type, DictionaryTypeEntity existingEntity) {
        DictionaryTypeEntity entity = existingEntity == null ? new DictionaryTypeEntity() : existingEntity;
        entity.setId(type.id());
        entity.setCode(type.code());
        entity.setName(type.name());
        entity.setCategory(type.category());
        entity.setHierarchical(type.hierarchical());
        entity.setCacheable(type.cacheable());
        entity.setSortOrder(type.sortOrder());
        entity.setSystemManaged(type.systemManaged());
        entity.setStatus(type.status().name());
        entity.setTenantId(type.tenantId());
        entity.setCreatedAt(type.createdAt());
        entity.setUpdatedAt(type.updatedAt());
        return entity;
    }

    private DictionaryItemEntity toItemEntity(DictionaryItem item, DictionaryItemEntity existingEntity) {
        DictionaryItemEntity entity = existingEntity == null ? new DictionaryItemEntity() : existingEntity;
        entity.setId(item.id());
        entity.setDictionaryTypeId(item.dictionaryTypeId());
        entity.setItemCode(item.itemCode());
        entity.setDisplayName(item.displayName());
        entity.setParentItemId(item.parentItemId());
        entity.setSortOrder(item.sortOrder());
        entity.setEnabled(item.enabled());
        entity.setMultiLangValue(item.multiLangValue());
        entity.setDefaultItem(item.defaultItem());
        entity.setExtensionJson(item.extensionJson());
        return entity;
    }

    private void applyTenantScope(LambdaQueryWrapper<DictionaryTypeEntity> wrapper, UUID tenantId) {
        if (tenantId == null) {
            wrapper.isNull(DictionaryTypeEntity::getTenantId);
        } else {
            wrapper.eq(DictionaryTypeEntity::getTenantId, tenantId);
        }
    }
}
