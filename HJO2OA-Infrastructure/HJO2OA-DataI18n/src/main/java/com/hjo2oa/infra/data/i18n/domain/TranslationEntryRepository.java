package com.hjo2oa.infra.data.i18n.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TranslationEntryRepository {

    Optional<TranslationEntry> findById(UUID id);

    Optional<TranslationEntry> findTranslation(
            String entityType,
            String entityId,
            String fieldName,
            String locale,
            UUID tenantId
    );

    List<TranslationEntry> findTranslationsByEntity(String entityType, String entityId, UUID tenantId);

    List<TranslationEntry> findTranslationsByLocale(
            String entityType,
            String entityId,
            String locale,
            UUID tenantId
    );

    List<TranslationEntry> findAll(UUID tenantId);

    TranslationEntry save(TranslationEntry entry);

    List<TranslationEntry> batchSave(List<TranslationEntry> entries);
}
