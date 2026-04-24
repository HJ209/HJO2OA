package com.hjo2oa.infra.audit.domain;

import java.util.UUID;

public record AuditFieldChangeView(
        UUID id,
        UUID auditRecordId,
        String fieldName,
        String oldValue,
        String newValue,
        SensitivityLevel sensitivityLevel
) {
}
