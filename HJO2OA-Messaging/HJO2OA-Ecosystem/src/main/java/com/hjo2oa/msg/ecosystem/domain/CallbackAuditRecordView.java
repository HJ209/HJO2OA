package com.hjo2oa.msg.ecosystem.domain;

import java.time.Instant;
import java.util.UUID;

public record CallbackAuditRecordView(
        UUID id,
        UUID integrationId,
        String callbackType,
        VerifyResult verifyResult,
        String payloadSummary,
        String errorMessage,
        String idempotencyKey,
        String payloadDigest,
        Instant occurredAt
) {
}
