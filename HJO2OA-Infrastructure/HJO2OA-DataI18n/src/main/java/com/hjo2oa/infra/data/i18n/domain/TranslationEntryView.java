package com.hjo2oa.infra.data.i18n.domain;

import java.time.Instant;
import java.util.UUID;

public record TranslationEntryView(
        UUID id,
        String entityType,
        String entityId,
        String fieldName,
        String locale,
        String translatedValue,
        TranslationStatus translationStatus,
        UUID tenantId,
        UUID updatedBy,
        Instant updatedAt
) {
}
