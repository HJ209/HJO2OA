package com.hjo2oa.infra.data.i18n.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.infra.data.i18n.domain.TranslationEntry;
import com.hjo2oa.infra.data.i18n.domain.TranslationEntryRepository;
import com.hjo2oa.infra.data.i18n.domain.TranslationStatus;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Primary
@Repository
public class MybatisTranslationEntryRepository implements TranslationEntryRepository {

    private final TranslationEntryMapper mapper;

    public MybatisTranslationEntryRepository(TranslationEntryMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Optional<TranslationEntry> findById(UUID id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<TranslationEntry> findTranslation(
            String entityType,
            String entityId,
            String fieldName,
            String locale
    ) {
        LambdaQueryWrapper<TranslationEntryEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TranslationEntryEntity::getEntityType, normalizeText(entityType))
                .eq(TranslationEntryEntity::getEntityId, normalizeText(entityId))
                .eq(TranslationEntryEntity::getFieldName, normalizeText(fieldName))
                .eq(TranslationEntryEntity::getLocale, normalizeLocale(locale));
        return Optional.ofNullable(mapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public List<TranslationEntry> findTranslationsByEntity(String entityType, String entityId) {
        LambdaQueryWrapper<TranslationEntryEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TranslationEntryEntity::getEntityType, normalizeText(entityType))
                .eq(TranslationEntryEntity::getEntityId, normalizeText(entityId))
                .orderByAsc(TranslationEntryEntity::getFieldName)
                .orderByAsc(TranslationEntryEntity::getLocale);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public List<TranslationEntry> findTranslationsByLocale(
            String entityType,
            String entityId,
            String locale
    ) {
        LambdaQueryWrapper<TranslationEntryEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TranslationEntryEntity::getEntityType, normalizeText(entityType))
                .eq(TranslationEntryEntity::getEntityId, normalizeText(entityId))
                .eq(TranslationEntryEntity::getLocale, normalizeLocale(locale))
                .orderByAsc(TranslationEntryEntity::getFieldName)
                .orderByDesc(TranslationEntryEntity::getUpdatedAt);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public TranslationEntry save(TranslationEntry entry) {
        TranslationEntryEntity entity = toEntity(entry);
        if (mapper.selectById(entry.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return entry;
    }

    @Override
    @Transactional
    public List<TranslationEntry> batchSave(List<TranslationEntry> entries) {
        for (TranslationEntry entry : entries) {
            save(entry);
        }
        return List.copyOf(entries);
    }

    private TranslationEntryEntity toEntity(TranslationEntry entry) {
        return new TranslationEntryEntity()
                .setId(entry.id())
                .setEntityType(entry.entityType())
                .setEntityId(entry.entityId())
                .setFieldName(entry.fieldName())
                .setLocale(entry.locale())
                .setTranslatedValue(entry.translatedValue())
                .setTranslationStatus(entry.translationStatus().name())
                .setTenantId(entry.tenantId())
                .setUpdatedBy(entry.updatedBy())
                .setUpdatedAt(entry.updatedAt());
    }

    private TranslationEntry toDomain(TranslationEntryEntity entity) {
        return new TranslationEntry(
                entity.getId(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getFieldName(),
                entity.getLocale(),
                entity.getTranslatedValue(),
                TranslationStatus.valueOf(entity.getTranslationStatus()),
                entity.getTenantId(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt()
        );
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeLocale(String value) {
        String normalized = value == null ? null : value.trim().replace('_', '-');
        if (normalized == null || normalized.isEmpty()) {
            return normalized;
        }
        String[] segments = normalized.split("-");
        if (segments.length == 1) {
            return segments[0].toLowerCase(Locale.ROOT);
        }
        StringBuilder builder = new StringBuilder(segments[0].toLowerCase(Locale.ROOT));
        for (int index = 1; index < segments.length; index++) {
            builder.append('-');
            builder.append(index == 1
                    ? segments[index].toUpperCase(Locale.ROOT)
                    : segments[index]);
        }
        return builder.toString();
    }
}
