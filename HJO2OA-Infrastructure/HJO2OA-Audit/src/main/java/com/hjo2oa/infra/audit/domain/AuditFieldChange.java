package com.hjo2oa.infra.audit.domain;

import java.util.Objects;
import java.util.UUID;

public record AuditFieldChange(
        UUID id,
        UUID auditRecordId,
        String fieldName,
        String oldValue,
        String newValue,
        SensitivityLevel sensitivityLevel
) {

    public AuditFieldChange {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(auditRecordId, "auditRecordId must not be null");
        fieldName = requireText(fieldName, "fieldName");
        oldValue = normalizeNullable(oldValue);
        newValue = normalizeNullable(newValue);
    }

    public static AuditFieldChange create(
            UUID auditRecordId,
            String fieldName,
            String oldValue,
            String newValue,
            SensitivityLevel sensitivityLevel
    ) {
        return new AuditFieldChange(UUID.randomUUID(), auditRecordId, fieldName, oldValue, newValue, sensitivityLevel);
    }

    public AuditFieldChangeView toView() {
        return new AuditFieldChangeView(id, auditRecordId, fieldName, oldValue, newValue, sensitivityLevel);
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
}
