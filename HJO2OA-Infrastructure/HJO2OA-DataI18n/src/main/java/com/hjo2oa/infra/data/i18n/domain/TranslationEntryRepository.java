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
            String locale
    );

    List<TranslationEntry> findTranslationsByEntity(String entityType, String entityId);

    List<TranslationEntry> findTranslationsByLocale(String entityType, String entityId, String locale);

    TranslationEntry save(TranslationEntry entry);

    List<TranslationEntry> batchSave(List<TranslationEntry> entries);
}
