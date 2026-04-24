package com.hjo2oa.infra.i18n.domain;

import java.util.UUID;

public record ResolvedLocaleMessageView(
        String bundleCode,
        String resourceKey,
        String requestedLocale,
        String resolvedLocale,
        String resourceValue,
        UUID tenantId,
        boolean usedFallback
) {
}
