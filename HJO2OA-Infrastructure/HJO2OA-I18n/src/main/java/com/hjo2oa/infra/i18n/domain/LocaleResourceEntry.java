package com.hjo2oa.infra.i18n.domain;

import java.util.Objects;
import java.util.UUID;

public record LocaleResourceEntry(
        UUID id,
        UUID localeBundleId,
        String resourceKey,
        String resourceValue,
        int version,
        boolean active
) {

    public LocaleResourceEntry {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(localeBundleId, "localeBundleId must not be null");
        resourceKey = requireText(resourceKey, "resourceKey");
        Objects.requireNonNull(resourceValue, "resourceValue must not be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than 0");
        }
    }

    public static LocaleResourceEntry create(UUID id, UUID localeBundleId, String resourceKey, String resourceValue) {
        return new LocaleResourceEntry(id, localeBundleId, resourceKey, resourceValue, 1, true);
    }

    public LocaleResourceEntry updateValue(String resourceValue) {
        Objects.requireNonNull(resourceValue, "resourceValue must not be null");
        if (active && this.resourceValue.equals(resourceValue)) {
            return this;
        }
        return new LocaleResourceEntry(id, localeBundleId, resourceKey, resourceValue, version + 1, true);
    }

    public LocaleResourceEntry deactivate() {
        if (!active) {
            return this;
        }
        return new LocaleResourceEntry(id, localeBundleId, resourceKey, resourceValue, version + 1, false);
    }

    public LocaleResourceEntry reactivate(String resourceValue) {
        Objects.requireNonNull(resourceValue, "resourceValue must not be null");
        return new LocaleResourceEntry(id, localeBundleId, resourceKey, resourceValue, version + 1, true);
    }

    public LocaleResourceEntryView toView() {
        return new LocaleResourceEntryView(id, localeBundleId, resourceKey, resourceValue, version, active);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
