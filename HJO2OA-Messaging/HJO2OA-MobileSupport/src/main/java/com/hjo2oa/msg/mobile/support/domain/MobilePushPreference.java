package com.hjo2oa.msg.mobile.support.domain;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record MobilePushPreference(
        UUID id,
        UUID tenantId,
        UUID personId,
        boolean pushEnabled,
        LocalTime quietStartsAt,
        LocalTime quietEndsAt,
        List<String> mutedCategories,
        Instant createdAt,
        Instant updatedAt
) {

    public MobilePushPreference {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(personId, "personId must not be null");
        mutedCategories = mutedCategories == null ? List.of() : List.copyOf(mutedCategories);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static MobilePushPreference createDefault(UUID tenantId, UUID personId, Instant now) {
        return new MobilePushPreference(UUID.randomUUID(), tenantId, personId, true, null, null, List.of(), now, now);
    }

    public MobilePushPreference update(
            boolean enabled,
            LocalTime startsAt,
            LocalTime endsAt,
            List<String> categories,
            Instant now
    ) {
        return new MobilePushPreference(id, tenantId, personId, enabled, startsAt, endsAt, categories, createdAt, now);
    }

    public MobilePushPreferenceView toView() {
        return new MobilePushPreferenceView(
                id,
                tenantId,
                personId,
                pushEnabled,
                quietStartsAt,
                quietEndsAt,
                mutedCategories,
                createdAt,
                updatedAt
        );
    }
}
