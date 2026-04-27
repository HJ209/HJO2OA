package com.hjo2oa.msg.event.subscription.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionPreferenceView(
        UUID id,
        UUID personId,
        NotificationCategory category,
        List<ChannelType> allowedChannels,
        QuietWindow quietWindow,
        DigestMode digestMode,
        boolean escalationOptIn,
        boolean muteNonWorkingHours,
        boolean enabled,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}
