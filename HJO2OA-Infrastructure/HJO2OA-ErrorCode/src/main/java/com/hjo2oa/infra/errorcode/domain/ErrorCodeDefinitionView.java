package com.hjo2oa.infra.errorcode.domain;

import java.time.Instant;
import java.util.UUID;

public record ErrorCodeDefinitionView(
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
}
