package com.hjo2oa.data.data.sync.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record SyncDifferenceItem(
        String differenceCode,
        DifferenceType differenceType,
        DifferenceStatus status,
        String recordKey,
        String fieldName,
        Map<String, Object> expectedPayload,
        Map<String, Object> actualPayload,
        String message,
        Instant detectedAt,
        String resolvedBy,
        String resolutionReason,
        Instant resolvedAt
) {

    public SyncDifferenceItem {
        differenceCode = SyncDomainSupport.requireText(differenceCode, "differenceCode");
        Objects.requireNonNull(differenceType, "differenceType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        recordKey = SyncDomainSupport.requireText(recordKey, "recordKey");
        fieldName = SyncDomainSupport.normalize(fieldName);
        expectedPayload = expectedPayload == null ? Map.of() : Map.copyOf(expectedPayload);
        actualPayload = actualPayload == null ? Map.of() : Map.copyOf(actualPayload);
        message = SyncDomainSupport.normalize(message);
        Objects.requireNonNull(detectedAt, "detectedAt must not be null");
        resolvedBy = SyncDomainSupport.normalize(resolvedBy);
        resolutionReason = SyncDomainSupport.normalize(resolutionReason);
    }

    public SyncDifferenceItem ignore(String operatorId, String reason, Instant resolvedAt) {
        return resolve(DifferenceStatus.IGNORED, operatorId, reason, resolvedAt);
    }

    public SyncDifferenceItem confirmManually(String operatorId, String reason, Instant resolvedAt) {
        return resolve(DifferenceStatus.MANUAL_CONFIRMED, operatorId, reason, resolvedAt);
    }

    public SyncDifferenceItem compensate(String operatorId, String reason, Instant resolvedAt) {
        return resolve(DifferenceStatus.COMPENSATED, operatorId, reason, resolvedAt);
    }

    public boolean resolved() {
        return status != DifferenceStatus.DETECTED;
    }

    private SyncDifferenceItem resolve(
            DifferenceStatus resolvedStatus,
            String operatorId,
            String reason,
            Instant resolvedAt
    ) {
        return new SyncDifferenceItem(
                differenceCode,
                differenceType,
                resolvedStatus,
                recordKey,
                fieldName,
                expectedPayload,
                actualPayload,
                message,
                detectedAt,
                SyncDomainSupport.requireText(operatorId, "operatorId"),
                SyncDomainSupport.requireText(reason, "reason"),
                Objects.requireNonNull(resolvedAt, "resolvedAt must not be null")
        );
    }
}
