package com.hjo2oa.infra.i18n.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.infra.i18n.domain.LocaleBundle;
import com.hjo2oa.infra.i18n.domain.LocaleBundleRepository;
import com.hjo2oa.infra.i18n.domain.LocaleBundleStatus;
import com.hjo2oa.infra.i18n.domain.LocaleResourceEntry;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisLocaleBundleRepository implements LocaleBundleRepository {

    private static final Comparator<LocaleBundle> BUNDLE_ORDER = Comparator
            .comparing(LocaleBundle::bundleCode)
            .thenComparing(LocaleBundle::locale)
            .thenComparing(bundle -> bundle.tenantId() == null ? "" : bundle.tenantId().toString());

    private final LocaleBundleMapper localeBundleMapper;
    private final LocaleResourceEntryMapper localeResourceEntryMapper;

    public MybatisLocaleBundleRepository(
            LocaleBundleMapper localeBundleMapper,
            LocaleResourceEntryMapper localeResourceEntryMapper
    ) {
        this.localeBundleMapper = Objects.requireNonNull(localeBundleMapper, "localeBundleMapper must not be null");
        this.localeResourceEntryMapper = Objects.requireNonNull(
                localeResourceEntryMapper,
                "localeResourceEntryMapper must not be null"
        );
    }

    @Override
    public Optional<LocaleBundle> findById(UUID bundleId) {
        LocaleBundleEntity entity = localeBundleMapper.selectById(bundleId);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(loadAggregate(entity.getId()));
    }

    @Override
    public List<LocaleBundle> findByCode(String bundleCode) {
        LambdaQueryWrapper<LocaleBundleEntity> wrapper = Wrappers.<LocaleBundleEntity>lambdaQuery()
                .eq(LocaleBundleEntity::getBundleCode, bundleCode)
                .orderByAsc(LocaleBundleEntity::getLocale)
                .orderByAsc(LocaleBundleEntity::getTenantId);
        return localeBundleMapper.selectList(wrapper).stream()
                .map(entity -> loadAggregate(entity.getId()))
                .sorted(BUNDLE_ORDER)
                .toList();
    }

    @Override
    public List<LocaleBundle> findByModuleAndLocale(String moduleCode, String locale) {
        LambdaQueryWrapper<LocaleBundleEntity> wrapper = Wrappers.<LocaleBundleEntity>lambdaQuery()
                .eq(LocaleBundleEntity::getModuleCode, moduleCode)
                .eq(LocaleBundleEntity::getLocale, locale)
                .orderByAsc(LocaleBundleEntity::getBundleCode)
                .orderByAsc(LocaleBundleEntity::getTenantId);
        return localeBundleMapper.selectList(wrapper).stream()
                .map(entity -> loadAggregate(entity.getId()))
                .sorted(BUNDLE_ORDER)
                .toList();
    }

    @Override
    public List<LocaleBundle> findActiveByLocale(String locale, UUID tenantId) {
        LambdaQueryWrapper<LocaleBundleEntity> wrapper = Wrappers.<LocaleBundleEntity>lambdaQuery()
                .eq(LocaleBundleEntity::getLocale, locale)
                .eq(LocaleBundleEntity::getStatus, LocaleBundleStatus.ACTIVE.name())
                .orderByAsc(LocaleBundleEntity::getBundleCode)
                .orderByAsc(LocaleBundleEntity::getTenantId);
        if (tenantId == null) {
            wrapper.isNull(LocaleBundleEntity::getTenantId);
        } else {
            wrapper.and(criteria -> criteria.eq(LocaleBundleEntity::getTenantId, tenantId)
                    .or()
                    .isNull(LocaleBundleEntity::getTenantId));
        }
        return localeBundleMapper.selectList(wrapper).stream()
                .map(entity -> loadAggregate(entity.getId()))
                .sorted(BUNDLE_ORDER)
                .toList();
    }

    @Override
    public LocaleBundle save(LocaleBundle bundle) {
        LocaleBundleEntity existing = localeBundleMapper.selectById(bundle.id());
        LocaleBundleEntity entity = toBundleEntity(bundle, existing);
        if (existing == null) {
            localeBundleMapper.insert(entity);
        } else {
            localeBundleMapper.updateById(entity);
        }
        syncEntries(bundle);
        return loadAggregate(bundle.id());
    }

    private LocaleBundle loadAggregate(UUID bundleId) {
        LocaleBundleEntity bundleEntity = localeBundleMapper.selectById(bundleId);
        if (bundleEntity == null) {
            throw new IllegalStateException("LocaleBundleEntity not found: " + bundleId);
        }
        List<LocaleResourceEntryEntity> entryEntities = localeResourceEntryMapper.selectList(
                Wrappers.<LocaleResourceEntryEntity>lambdaQuery()
                        .eq(LocaleResourceEntryEntity::getLocaleBundleId, bundleId)
                        .orderByAsc(LocaleResourceEntryEntity::getResourceKey)
        );
        return toAggregate(bundleEntity, entryEntities);
    }

    private void syncEntries(LocaleBundle bundle) {
        List<LocaleResourceEntryEntity> existing = localeResourceEntryMapper.selectList(
                Wrappers.<LocaleResourceEntryEntity>lambdaQuery()
                        .eq(LocaleResourceEntryEntity::getLocaleBundleId, bundle.id())
        );
        Map<UUID, LocaleResourceEntryEntity> existingById = existing.stream()
                .collect(Collectors.toMap(LocaleResourceEntryEntity::getId, Function.identity()));
        List<UUID> retainedIds = bundle.entries().stream()
                .map(LocaleResourceEntry::id)
                .toList();
        for (LocaleResourceEntry entry : bundle.entries()) {
            LocaleResourceEntryEntity entity = toEntryEntity(entry, existingById.get(entry.id()));
            if (existingById.containsKey(entry.id())) {
                localeResourceEntryMapper.updateById(entity);
            } else {
                localeResourceEntryMapper.insert(entity);
            }
        }
        for (LocaleResourceEntryEntity existingEntity : existing) {
            if (!retainedIds.contains(existingEntity.getId())) {
                localeResourceEntryMapper.deleteById(existingEntity.getId());
            }
        }
    }

    private LocaleBundle toAggregate(
            LocaleBundleEntity bundleEntity,
            List<LocaleResourceEntryEntity> entryEntities
    ) {
        return new LocaleBundle(
                bundleEntity.getId(),
                bundleEntity.getBundleCode(),
                bundleEntity.getModuleCode(),
                bundleEntity.getLocale(),
                bundleEntity.getFallbackLocale(),
                LocaleBundleStatus.valueOf(bundleEntity.getStatus()),
                bundleEntity.getTenantId(),
                bundleEntity.getCreatedAt(),
                bundleEntity.getUpdatedAt(),
                entryEntities.stream().map(this::toDomainEntry).toList()
        );
    }

    private LocaleBundleEntity toBundleEntity(
            LocaleBundle bundle,
            LocaleBundleEntity existing
    ) {
        LocaleBundleEntity entity = existing == null ? new LocaleBundleEntity() : existing;
        entity.setId(bundle.id());
        entity.setBundleCode(bundle.bundleCode());
        entity.setModuleCode(bundle.moduleCode());
        entity.setLocale(bundle.locale());
        entity.setFallbackLocale(bundle.fallbackLocale());
        entity.setStatus(bundle.status().name());
        entity.setTenantId(bundle.tenantId());
        entity.setCreatedAt(bundle.createdAt());
        entity.setUpdatedAt(bundle.updatedAt());
        return entity;
    }

    private LocaleResourceEntryEntity toEntryEntity(
            LocaleResourceEntry entry,
            LocaleResourceEntryEntity existing
    ) {
        LocaleResourceEntryEntity entity = existing == null ? new LocaleResourceEntryEntity() : existing;
        entity.setId(entry.id());
        entity.setLocaleBundleId(entry.localeBundleId());
        entity.setResourceKey(entry.resourceKey());
        entity.setResourceValue(entry.resourceValue());
        entity.setVersion(entry.version());
        entity.setActive(entry.active());
        return entity;
    }

    private LocaleResourceEntry toDomainEntry(LocaleResourceEntryEntity entity) {
        return new LocaleResourceEntry(
                entity.getId(),
                entity.getLocaleBundleId(),
                entity.getResourceKey(),
                entity.getResourceValue(),
                entity.getVersion() == null ? 1 : entity.getVersion(),
                Boolean.TRUE.equals(entity.getActive())
        );
    }
}
