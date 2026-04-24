package com.hjo2oa.infra.i18n.domain;

import java.util.UUID;

public record LocaleResourceEntryView(
        UUID id,
        UUID localeBundleId,
        String resourceKey,
        String resourceValue,
        int version,
        boolean active
) {
}
