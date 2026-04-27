package com.hjo2oa.msg.event.subscription.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record SubscriptionPreference(
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

    public SubscriptionPreference {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(personId, "personId must not be null");
        Objects.requireNonNull(category, "category must not be null");
        allowedChannels = immutableChannels(allowedChannels);
        digestMode = digestMode == null ? DigestMode.IMMEDIATE : digestMode;
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (forcedCategory(category) && (!enabled || allowedChannels.isEmpty())) {
            throw new IllegalArgumentException("forced notification category cannot be fully muted");
        }
    }

    public static SubscriptionPreference create(
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
            Instant now
    ) {
        return new SubscriptionPreference(
                id,
                personId,
                category,
                allowedChannels,
                quietWindow,
                digestMode,
                escalationOptIn,
                muteNonWorkingHours,
                enabled,
                tenantId,
                now,
                now
        );
    }

    public SubscriptionPreference update(
            List<ChannelType> newAllowedChannels,
            QuietWindow newQuietWindow,
            DigestMode newDigestMode,
            boolean newEscalationOptIn,
            boolean newMuteNonWorkingHours,
            boolean newEnabled,
            Instant now
    ) {
        return new SubscriptionPreference(
                id,
                personId,
                category,
                newAllowedChannels,
                newQuietWindow,
                newDigestMode,
                newEscalationOptIn,
                newMuteNonWorkingHours,
                newEnabled,
                tenantId,
                createdAt,
                now
        );
    }

    public SubscriptionPreferenceView toView() {
        return new SubscriptionPreferenceView(
                id,
                personId,
                category,
                allowedChannels,
                quietWindow,
                digestMode,
                escalationOptIn,
                muteNonWorkingHours,
                enabled,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    public static boolean forcedCategory(NotificationCategory category) {
        return category == NotificationCategory.ORG_ACCOUNT_LOCKED
                || category == NotificationCategory.SYSTEM_SECURITY;
    }

    private static List<ChannelType> immutableChannels(List<ChannelType> channels) {
        if (channels == null || channels.isEmpty()) {
            return List.of();
        }
        Set<ChannelType> normalized = new LinkedHashSet<>();
        for (ChannelType channel : channels) {
            if (channel != null) {
                normalized.add(channel);
            }
        }
        return List.copyOf(normalized);
    }
}
