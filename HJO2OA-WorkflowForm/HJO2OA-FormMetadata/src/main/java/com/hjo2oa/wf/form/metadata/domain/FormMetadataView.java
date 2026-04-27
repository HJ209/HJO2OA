package com.hjo2oa.wf.form.metadata.domain;

import java.time.Instant;
import java.util.UUID;

public record FormMetadataView(
        UUID id,
        String code,
        String name,
        String nameI18nKey,
        int version,
        FormMetadataStatus status,
        UUID tenantId,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
