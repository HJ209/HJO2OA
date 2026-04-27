package com.hjo2oa.msg.ecosystem.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CallbackAuditRecord(
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

    public CallbackAuditRecord {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(integrationId, "integrationId must not be null");
        callbackType = EcosystemIntegration.requireText(callbackType, "callbackType");
        Objects.requireNonNull(verifyResult, "verifyResult must not be null");
        idempotencyKey = EcosystemIntegration.requireText(idempotencyKey, "idempotencyKey");
        payloadDigest = EcosystemIntegration.requireText(payloadDigest, "payloadDigest");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static CallbackAuditRecord create(
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
        return new CallbackAuditRecord(
                id,
                integrationId,
                callbackType,
                verifyResult,
                payloadSummary,
                errorMessage,
                idempotencyKey,
                payloadDigest,
                occurredAt
        );
    }

    public CallbackAuditRecordView toView() {
        return new CallbackAuditRecordView(
                id,
                integrationId,
                callbackType,
                verifyResult,
                payloadSummary,
                errorMessage,
                idempotencyKey,
                payloadDigest,
                occurredAt
        );
    }
}
