package com.hjo2oa.infra.data.i18n.infrastructure;

import com.hjo2oa.infra.data.i18n.domain.TranslationEntry;
import com.hjo2oa.infra.data.i18n.domain.TranslationEntryRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryTranslationEntryRepository implements TranslationEntryRepository {

    private final Map<UUID, TranslationEntry> entriesById = new LinkedHashMap<>();
    private final Map<BusinessKey, UUID> idsByBusinessKey = new LinkedHashMap<>();

    @Override
    public synchronized Optional<TranslationEntry> findById(UUID id) {
        return Optional.ofNullable(entriesById.get(id));
    }

    @Override
    public synchronized Optional<TranslationEntry> findTranslation(
            String entityType,
            String entityId,
            String fieldName,
            String locale,
            UUID tenantId
    ) {
        UUID id = idsByBusinessKey.get(BusinessKey.of(entityType, entityId, fieldName, locale, tenantId));
        return id == null ? Optional.empty() : Optional.ofNullable(entriesById.get(id));
    }

    @Override
    public synchronized List<TranslationEntry> findTranslationsByEntity(
            String entityType,
            String entityId,
            UUID tenantId
    ) {
        return entriesById.values().stream()
                .filter(entry -> entry.entityType().equals(normalizeText(entityType)))
                .filter(entry -> entry.entityId().equals(normalizeText(entityId)))
                .filter(entry -> tenantId == null || entry.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(TranslationEntry::fieldName)
                        .thenComparing(TranslationEntry::locale))
                .toList();
    }

    @Override
    public synchronized List<TranslationEntry> findTranslationsByLocale(
            String entityType,
            String entityId,
            String locale,
            UUID tenantId
    ) {
        return entriesById.values().stream()
                .filter(entry -> entry.entityType().equals(normalizeText(entityType)))
                .filter(entry -> entry.entityId().equals(normalizeText(entityId)))
                .filter(entry -> entry.locale().equals(BusinessKey.normalizeLocale(locale)))
                .filter(entry -> tenantId == null || entry.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(TranslationEntry::fieldName)
                        .thenComparing(TranslationEntry::updatedAt, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public synchronized List<TranslationEntry> findAll(UUID tenantId) {
        return entriesById.values().stream()
                .filter(entry -> tenantId == null || entry.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(TranslationEntry::entityType)
                        .thenComparing(TranslationEntry::entityId)
                        .thenComparing(TranslationEntry::fieldName)
                        .thenComparing(TranslationEntry::locale))
                .toList();
    }

    @Override
    public synchronized TranslationEntry save(TranslationEntry entry) {
        saveInternal(entry, entriesById, idsByBusinessKey);
        return entry;
    }

    @Override
    public synchronized List<TranslationEntry> batchSave(List<TranslationEntry> entries) {
        Map<UUID, TranslationEntry> entriesCopy = new LinkedHashMap<>(entriesById);
        Map<BusinessKey, UUID> keysCopy = new LinkedHashMap<>(idsByBusinessKey);
        for (TranslationEntry entry : entries) {
            saveInternal(entry, entriesCopy, keysCopy);
        }
        entriesById.clear();
        entriesById.putAll(entriesCopy);
        idsByBusinessKey.clear();
        idsByBusinessKey.putAll(keysCopy);
        return List.copyOf(entries);
    }

    private void saveInternal(
            TranslationEntry entry,
            Map<UUID, TranslationEntry> targetEntriesById,
            Map<BusinessKey, UUID> targetIdsByBusinessKey
    ) {
        BusinessKey businessKey = BusinessKey.of(entry);
        UUID existingId = targetIdsByBusinessKey.get(businessKey);
        if (existingId != null && !existingId.equals(entry.id())) {
            throw new IllegalStateException("translation unique key conflict: " + businessKey);
        }
        TranslationEntry previous = targetEntriesById.put(entry.id(), entry);
        if (previous != null) {
            targetIdsByBusinessKey.remove(BusinessKey.of(previous));
        }
        targetIdsByBusinessKey.put(businessKey, entry.id());
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private record BusinessKey(
            String entityType,
            String entityId,
            String fieldName,
            String locale,
            UUID tenantId
    ) {

        private static BusinessKey of(TranslationEntry entry) {
            return of(entry.entityType(), entry.entityId(), entry.fieldName(), entry.locale(), entry.tenantId());
        }

        private static BusinessKey of(
                String entityType,
                String entityId,
                String fieldName,
                String locale,
                UUID tenantId
        ) {
            return new BusinessKey(
                    entityType == null ? null : entityType.trim(),
                    entityId == null ? null : entityId.trim(),
                    fieldName == null ? null : fieldName.trim(),
                    normalizeLocale(locale),
                    tenantId
            );
        }

        private static String normalizeLocale(String value) {
            if (value == null) {
                return null;
            }
            String normalized = value.trim().replace('_', '-');
            String[] segments = normalized.split("-");
            if (segments.length == 1) {
                return segments[0].toLowerCase();
            }
            StringBuilder builder = new StringBuilder(segments[0].toLowerCase());
            for (int index = 1; index < segments.length; index++) {
                builder.append('-');
                builder.append(index == 1 ? segments[index].toUpperCase() : segments[index]);
            }
            return builder.toString();
        }
    }
}
