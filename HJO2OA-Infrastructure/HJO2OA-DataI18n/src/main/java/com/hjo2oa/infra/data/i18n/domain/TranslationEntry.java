package com.hjo2oa.infra.data.i18n.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record TranslationEntry(
        UUID id,
        String entityType,
        String entityId,
        String fieldName,
        String locale,
        String translatedValue,
        TranslationStatus translationStatus,
        UUID tenantId,
        UUID updatedBy,
        Instant updatedAt
) {

    public TranslationEntry {
        Objects.requireNonNull(id, "id must not be null");
        entityType = requireText(entityType, "entityType");
        entityId = requireText(entityId, "entityId");
        fieldName = requireText(fieldName, "fieldName");
        locale = normalizeLocale(locale);
        translatedValue = normalizeTranslatedValue(translatedValue);
        Objects.requireNonNull(translationStatus, "translationStatus must not be null");
        validateStatusAndValue(translationStatus, translatedValue);
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static TranslationEntry create(
            String entityType,
            String entityId,
            String fieldName,
            String locale,
            String translatedValue,
            UUID tenantId,
            UUID updatedBy,
            Instant updatedAt
    ) {
        String normalizedValue = normalizeTranslatedValue(translatedValue);
        TranslationStatus status = normalizedValue.isBlank()
                ? TranslationStatus.DRAFT
                : TranslationStatus.TRANSLATED;
        return new TranslationEntry(
                UUID.randomUUID(),
                entityType,
                entityId,
                fieldName,
                locale,
                normalizedValue,
                status,
                tenantId,
                updatedBy,
                updatedAt
        );
    }

    public TranslationEntry translate(String newValue, UUID updatedBy, Instant updatedAt) {
        String normalizedValue = requireTranslatedValue(newValue);
        return new TranslationEntry(
                id,
                entityType,
                entityId,
                fieldName,
                locale,
                normalizedValue,
                TranslationStatus.TRANSLATED,
                tenantId,
                updatedBy,
                updatedAt
        );
    }

    public TranslationEntry review(UUID updatedBy, Instant updatedAt) {
        if (translationStatus == TranslationStatus.REVIEWED) {
            return this;
        }
        if (translationStatus != TranslationStatus.TRANSLATED) {
            throw new IllegalArgumentException("only translated entry can be reviewed");
        }
        return new TranslationEntry(
                id,
                entityType,
                entityId,
                fieldName,
                locale,
                translatedValue,
                TranslationStatus.REVIEWED,
                tenantId,
                updatedBy,
                updatedAt
        );
    }

    public TranslationEntryView toView() {
        return new TranslationEntryView(
                id,
                entityType,
                entityId,
                fieldName,
                locale,
                translatedValue,
                translationStatus,
                tenantId,
                updatedBy,
                updatedAt
        );
    }

    public boolean hasTranslatedValue() {
        return !translatedValue.isBlank();
    }

    private static void validateStatusAndValue(
            TranslationStatus translationStatus,
            String translatedValue
    ) {
        if (translationStatus == TranslationStatus.DRAFT && !translatedValue.isBlank()) {
            throw new IllegalArgumentException("draft entry must not contain translatedValue");
        }
        if (translationStatus != TranslationStatus.DRAFT && translatedValue.isBlank()) {
            throw new IllegalArgumentException("translatedValue must not be blank when status is not DRAFT");
        }
    }

    private static String requireTranslatedValue(String value) {
        String normalized = normalizeTranslatedValue(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("translatedValue must not be blank");
        }
        return normalized;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeTranslatedValue(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String normalizeLocale(String value) {
        String normalized = requireText(value, "locale").replace('_', '-');
        String[] segments = normalized.split("-");
        if (segments.length == 1) {
            return segments[0].toLowerCase(Locale.ROOT);
        }
        StringBuilder builder = new StringBuilder(segments[0].toLowerCase(Locale.ROOT));
        for (int index = 1; index < segments.length; index++) {
            builder.append('-');
            builder.append(index == 1
                    ? segments[index].toUpperCase(Locale.ROOT)
                    : segments[index]);
        }
        return builder.toString();
    }
}
