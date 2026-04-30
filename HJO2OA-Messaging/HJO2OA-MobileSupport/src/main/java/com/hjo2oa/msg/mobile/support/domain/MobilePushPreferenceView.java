package com.hjo2oa.msg.mobile.support.domain;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record MobilePushPreferenceView(
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
}
