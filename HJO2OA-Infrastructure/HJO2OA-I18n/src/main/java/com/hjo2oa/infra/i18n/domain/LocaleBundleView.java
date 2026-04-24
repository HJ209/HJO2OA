package com.hjo2oa.infra.i18n.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LocaleBundleView(
        UUID id,
        String bundleCode,
        String moduleCode,
        String locale,
        String fallbackLocale,
        LocaleBundleStatus status,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        List<LocaleResourceEntryView> entries
) {

    public LocaleBundleView {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
