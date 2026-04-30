package com.hjo2oa.infra.errorcode.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ErrorCodeDefinition(
        UUID id,
        String code,
        String moduleCode,
        String category,
        ErrorSeverity severity,
        int httpStatus,
        String messageKey,
        boolean retryable,
        boolean deprecated,
        Instant createdAt,
        Instant updatedAt
) {

    public ErrorCodeDefinition {
        Objects.requireNonNull(id, "id must not be null");
        code = requireText(code, "code");
        moduleCode = requireText(moduleCode, "moduleCode");
        category = normalizeNullable(category);
        Objects.requireNonNull(severity, "severity must not be null");
        validateHttpStatus(httpStatus);
        messageKey = requireText(messageKey, "messageKey");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ErrorCodeDefinition create(
            String code,
            String moduleCode,
            String category,
            ErrorSeverity severity,
            int httpStatus,
            String messageKey,
            boolean retryable,
            Instant now
    ) {
        Objects.requireNonNull(now, "now must not be null");
        return new ErrorCodeDefinition(
                UUID.randomUUID(),
                code,
                moduleCode,
                category,
                severity,
                httpStatus,
                messageKey,
                retryable,
                false,
                now,
                now
        );
    }

    public ErrorCodeDefinition deprecate(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (deprecated) {
            return this;
        }
        return new ErrorCodeDefinition(
                id,
                code,
                moduleCode,
                category,
                severity,
                httpStatus,
                messageKey,
                retryable,
                true,
                createdAt,
                now
        );
    }

    public ErrorCodeDefinition updateSeverity(ErrorSeverity newSeverity, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(newSeverity, "newSeverity must not be null");
        if (severity == newSeverity) {
            return this;
        }
        return new ErrorCodeDefinition(
                id,
                code,
                moduleCode,
                category,
                newSeverity,
                httpStatus,
                messageKey,
                retryable,
                deprecated,
                createdAt,
                now
        );
    }

    public ErrorCodeDefinition updateHttpStatus(int newHttpStatus, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        validateHttpStatus(newHttpStatus);
        if (httpStatus == newHttpStatus) {
            return this;
        }
        return new ErrorCodeDefinition(
                id,
                code,
                moduleCode,
                category,
                severity,
                newHttpStatus,
                messageKey,
                retryable,
                deprecated,
                createdAt,
                now
        );
    }

    public ErrorCodeDefinition updateMessageKey(String newMessageKey, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        String normalizedMessageKey = requireText(newMessageKey, "messageKey");
        if (messageKey.equals(normalizedMessageKey)) {
            return this;
        }
        return new ErrorCodeDefinition(
                id,
                code,
                moduleCode,
                category,
                severity,
                httpStatus,
                normalizedMessageKey,
                retryable,
                deprecated,
                createdAt,
                now
        );
    }

    public ErrorCodeDefinition updateMetadata(
            String newCategory,
            ErrorSeverity newSeverity,
            int newHttpStatus,
            String newMessageKey,
            boolean newRetryable,
            Instant now
    ) {
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(newSeverity, "severity must not be null");
        validateHttpStatus(newHttpStatus);
        return new ErrorCodeDefinition(
                id,
                code,
                moduleCode,
                newCategory,
                newSeverity,
                newHttpStatus,
                newMessageKey,
                newRetryable,
                deprecated,
                createdAt,
                now
        );
    }

    public ErrorCodeDefinition markRetryable(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (retryable) {
            return this;
        }
        return new ErrorCodeDefinition(
                id,
                code,
                moduleCode,
                category,
                severity,
                httpStatus,
                messageKey,
                true,
                deprecated,
                createdAt,
                now
        );
    }

    public ErrorCodeDefinitionView toView() {
        return new ErrorCodeDefinitionView(
                id,
                code,
                moduleCode,
                category,
                severity,
                httpStatus,
                messageKey,
                retryable,
                deprecated,
                createdAt,
                updatedAt
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static void validateHttpStatus(int httpStatus) {
        if (httpStatus < 100 || httpStatus > 599) {
            throw new IllegalArgumentException("httpStatus must be between 100 and 599");
        }
    }
}
