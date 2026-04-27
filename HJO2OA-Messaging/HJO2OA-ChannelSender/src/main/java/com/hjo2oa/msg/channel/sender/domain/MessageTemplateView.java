package com.hjo2oa.msg.channel.sender.domain;

import java.time.Instant;
import java.util.UUID;

public record MessageTemplateView(
        UUID id,
        String code,
        ChannelType channelType,
        String locale,
        int version,
        MessageCategory category,
        String titleTemplate,
        String bodyTemplate,
        String variableSchema,
        MessageTemplateStatus status,
        boolean systemLocked,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}
