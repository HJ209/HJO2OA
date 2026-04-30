package com.hjo2oa.infra.attachment.application;

import java.time.Instant;
import java.util.UUID;

public record AttachmentAccessAuditRecord(
        UUID id,
        UUID attachmentId,
        Integer versionNo,
        String action,
        UUID tenantId,
        UUID operatorId,
        String clientIp,
        Instant occurredAt
) {
}
