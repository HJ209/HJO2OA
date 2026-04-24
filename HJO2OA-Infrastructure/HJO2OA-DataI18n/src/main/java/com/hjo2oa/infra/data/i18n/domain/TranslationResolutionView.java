package com.hjo2oa.infra.data.i18n.domain;

import java.util.UUID;

public record TranslationResolutionView(
        UUID entryId,
        String entityType,
        String entityId,
        String fieldName,
        String requestedLocale,
        String resolvedLocale,
        String resolvedValue,
        TranslationStatus translationStatus,
        TranslationResolveSource resolveSource,
        boolean fallbackApplied,
        UUID tenantId
) {
}
