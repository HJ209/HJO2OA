package com.hjo2oa.data.governance.domain;

import com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType;
import com.hjo2oa.data.governance.domain.GovernanceTypes.ServiceVersionStatus;
import java.time.Instant;
import java.util.Objects;

public record ServiceVersionRecord(
        String versionRecordId,
        String governanceId,
        GovernanceScopeType targetType,
        String targetCode,
        String version,
        String compatibilityNote,
        String changeSummary,
        ServiceVersionStatus status,
        Instant registeredAt,
        Instant publishedAt,
        Instant deprecatedAt,
        String operatorId,
        String approvalNote,
        String auditTraceId
) {

    public ServiceVersionRecord {
        versionRecordId = requireText(versionRecordId, "versionRecordId");
        governanceId = requireText(governanceId, "governanceId");
        Objects.requireNonNull(targetType, "targetType must not be null");
        targetCode = requireText(targetCode, "targetCode");
        version = requireText(version, "version");
        compatibilityNote = normalizeOptional(compatibilityNote);
        changeSummary = normalizeOptional(changeSummary);
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(registeredAt, "registeredAt must not be null");
        operatorId = normalizeOptional(operatorId);
        approvalNote = normalizeOptional(approvalNote);
        auditTraceId = normalizeOptional(auditTraceId);
    }

    public static ServiceVersionRecord register(
            String versionRecordId,
            String governanceId,
            GovernanceScopeType targetType,
            String targetCode,
            String version,
            String compatibilityNote,
            String changeSummary,
            String operatorId,
            String approvalNote,
            String auditTraceId,
            Instant now
    ) {
        return new ServiceVersionRecord(
                versionRecordId,
                governanceId,
                targetType,
                targetCode,
                version,
                compatibilityNote,
                changeSummary,
                ServiceVersionStatus.REGISTERED,
                now,
                null,
                null,
                operatorId,
                approvalNote,
                auditTraceId
        );
    }

    public ServiceVersionRecord publish(String operatorId, String approvalNote, String auditTraceId, Instant now) {
        return new ServiceVersionRecord(
                versionRecordId,
                governanceId,
                targetType,
                targetCode,
                version,
                compatibilityNote,
                changeSummary,
                ServiceVersionStatus.PUBLISHED,
                registeredAt,
                now,
                deprecatedAt,
                operatorId,
                approvalNote,
                auditTraceId
        );
    }

    public ServiceVersionRecord deprecate(String operatorId, String approvalNote, String auditTraceId, Instant now) {
        return new ServiceVersionRecord(
                versionRecordId,
                governanceId,
                targetType,
                targetCode,
                version,
                compatibilityNote,
                changeSummary,
                ServiceVersionStatus.DEPRECATED,
                registeredAt,
                publishedAt,
                now,
                operatorId,
                approvalNote,
                auditTraceId
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

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
