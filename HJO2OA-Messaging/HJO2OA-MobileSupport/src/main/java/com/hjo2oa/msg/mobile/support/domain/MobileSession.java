package com.hjo2oa.msg.mobile.support.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MobileSession(
        UUID id,
        UUID deviceBindingId,
        UUID personId,
        UUID accountId,
        UUID currentAssignmentId,
        UUID currentPositionId,
        MobileSessionStatus sessionStatus,
        MobileRiskLevel riskLevelSnapshot,
        Instant riskFrozenAt,
        String riskReason,
        Instant issuedAt,
        Instant expiresAt,
        Instant lastHeartbeatAt,
        int refreshVersion,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {

    public MobileSession {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(deviceBindingId, "deviceBindingId must not be null");
        Objects.requireNonNull(personId, "personId must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(sessionStatus, "sessionStatus must not be null");
        Objects.requireNonNull(riskLevelSnapshot, "riskLevelSnapshot must not be null");
        riskReason = normalizeNullable(riskReason);
        Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
        if (refreshVersion < 0) {
            throw new IllegalArgumentException("refreshVersion must not be negative");
        }
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static MobileSession issue(
            UUID id,
            DeviceBinding binding,
            UUID currentAssignmentId,
            UUID currentPositionId,
            Instant issuedAt,
            Instant expiresAt
    ) {
        Objects.requireNonNull(binding, "binding must not be null");
        if (!binding.isActive()) {
            throw new IllegalStateException("Only active devices can issue mobile sessions");
        }
        MobileSessionStatus status = binding.riskLevel() == MobileRiskLevel.HIGH
                ? MobileSessionStatus.RISK_FROZEN
                : MobileSessionStatus.ACTIVE;
        Instant frozenAt = status == MobileSessionStatus.RISK_FROZEN ? issuedAt : null;
        String reason = status == MobileSessionStatus.RISK_FROZEN ? "Device risk level is HIGH" : null;
        return new MobileSession(
                id,
                binding.id(),
                binding.personId(),
                binding.accountId(),
                currentAssignmentId,
                currentPositionId,
                status,
                binding.riskLevel(),
                frozenAt,
                reason,
                issuedAt,
                expiresAt,
                null,
                0,
                binding.tenantId(),
                issuedAt,
                issuedAt
        );
    }

    public boolean isActiveAt(Instant now) {
        return sessionStatus == MobileSessionStatus.ACTIVE && expiresAt.isAfter(now);
    }

    public MobileSession refresh(
            int expectedRefreshVersion,
            MobileRiskLevel currentRiskLevel,
            Instant now,
            Instant newExpiresAt
    ) {
        Objects.requireNonNull(currentRiskLevel, "currentRiskLevel must not be null");
        if (refreshVersion != expectedRefreshVersion) {
            throw new IllegalStateException("Mobile session refresh version conflict");
        }
        if (sessionStatus == MobileSessionStatus.RISK_FROZEN) {
            throw new IllegalStateException("Mobile session is risk frozen");
        }
        if (sessionStatus != MobileSessionStatus.ACTIVE) {
            throw new IllegalStateException("Only active mobile sessions can be refreshed");
        }
        if (!expiresAt.isAfter(now)) {
            return expire(now);
        }
        if (currentRiskLevel == MobileRiskLevel.HIGH) {
            return freeze(currentRiskLevel, "Device risk level is HIGH", now);
        }
        return new MobileSession(
                id,
                deviceBindingId,
                personId,
                accountId,
                currentAssignmentId,
                currentPositionId,
                MobileSessionStatus.ACTIVE,
                currentRiskLevel,
                null,
                null,
                issuedAt,
                newExpiresAt,
                now,
                refreshVersion + 1,
                tenantId,
                createdAt,
                now
        );
    }

    public MobileSession updateIdentitySnapshot(UUID assignmentId, UUID positionId, Instant now) {
        return new MobileSession(
                id,
                deviceBindingId,
                personId,
                accountId,
                assignmentId,
                positionId,
                sessionStatus,
                riskLevelSnapshot,
                riskFrozenAt,
                riskReason,
                issuedAt,
                expiresAt,
                lastHeartbeatAt,
                refreshVersion,
                tenantId,
                createdAt,
                now
        );
    }

    public MobileSession expire(Instant now) {
        if (sessionStatus == MobileSessionStatus.EXPIRED) {
            return this;
        }
        return withStatus(MobileSessionStatus.EXPIRED, riskLevelSnapshot, riskFrozenAt, riskReason, now);
    }

    public MobileSession revoke(Instant now) {
        if (sessionStatus == MobileSessionStatus.REVOKED) {
            return this;
        }
        return withStatus(MobileSessionStatus.REVOKED, riskLevelSnapshot, riskFrozenAt, riskReason, now);
    }

    public MobileSession freeze(MobileRiskLevel riskLevel, String reason, Instant now) {
        return withStatus(MobileSessionStatus.RISK_FROZEN, riskLevel, now, reason, now);
    }

    public MobileSessionView toView() {
        return new MobileSessionView(
                id,
                deviceBindingId,
                personId,
                accountId,
                currentAssignmentId,
                currentPositionId,
                sessionStatus,
                riskLevelSnapshot,
                riskFrozenAt,
                riskReason,
                issuedAt,
                expiresAt,
                lastHeartbeatAt,
                refreshVersion,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    private MobileSession withStatus(
            MobileSessionStatus status,
            MobileRiskLevel riskLevel,
            Instant riskFrozenAt,
            String riskReason,
            Instant now
    ) {
        return new MobileSession(
                id,
                deviceBindingId,
                personId,
                accountId,
                currentAssignmentId,
                currentPositionId,
                status,
                riskLevel,
                riskFrozenAt,
                riskReason,
                issuedAt,
                expiresAt,
                lastHeartbeatAt,
                refreshVersion,
                tenantId,
                createdAt,
                now
        );
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
