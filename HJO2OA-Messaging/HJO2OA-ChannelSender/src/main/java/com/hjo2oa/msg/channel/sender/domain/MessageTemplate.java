package com.hjo2oa.msg.channel.sender.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MessageTemplate(
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

    public MessageTemplate {
        Objects.requireNonNull(id, "id must not be null");
        code = requireText(code, "code");
        Objects.requireNonNull(channelType, "channelType must not be null");
        locale = requireText(locale, "locale").replace('_', '-').toLowerCase(java.util.Locale.ROOT);
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
        Objects.requireNonNull(category, "category must not be null");
        titleTemplate = requireText(titleTemplate, "titleTemplate");
        bodyTemplate = requireText(bodyTemplate, "bodyTemplate");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static MessageTemplate create(
            UUID id,
            String code,
            ChannelType channelType,
            String locale,
            int version,
            MessageCategory category,
            String titleTemplate,
            String bodyTemplate,
            String variableSchema,
            boolean systemLocked,
            UUID tenantId,
            Instant now
    ) {
        return new MessageTemplate(
                id,
                code,
                channelType,
                locale,
                version,
                category,
                titleTemplate,
                bodyTemplate,
                variableSchema,
                MessageTemplateStatus.DRAFT,
                systemLocked,
                tenantId,
                now,
                now
        );
    }

    public MessageTemplate publish(Instant now) {
        return withStatus(MessageTemplateStatus.PUBLISHED, now);
    }

    public MessageTemplate disable(Instant now) {
        if (systemLocked) {
            throw new IllegalStateException("system locked template cannot be disabled");
        }
        return withStatus(MessageTemplateStatus.DISABLED, now);
    }

    public MessageTemplateView toView() {
        return new MessageTemplateView(
                id,
                code,
                channelType,
                locale,
                version,
                category,
                titleTemplate,
                bodyTemplate,
                variableSchema,
                status,
                systemLocked,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    private MessageTemplate withStatus(MessageTemplateStatus targetStatus, Instant now) {
        return new MessageTemplate(
                id,
                code,
                channelType,
                locale,
                version,
                category,
                titleTemplate,
                bodyTemplate,
                variableSchema,
                targetStatus,
                systemLocked,
                tenantId,
                createdAt,
                now
        );
    }

    static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
