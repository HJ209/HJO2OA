package com.hjo2oa.msg.mobile.support.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DeviceBinding(
        UUID id,
        UUID personId,
        UUID accountId,
        String deviceId,
        String deviceFingerprint,
        MobilePlatform platform,
        MobileAppType appType,
        String pushToken,
        DeviceBindStatus bindStatus,
        MobileRiskLevel riskLevel,
        Instant lastLoginAt,
        Instant lastSeenAt,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {

    public DeviceBinding {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(personId, "personId must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
        deviceId = requireText(deviceId, "deviceId");
        deviceFingerprint = normalizeNullable(deviceFingerprint);
        Objects.requireNonNull(platform, "platform must not be null");
        Objects.requireNonNull(appType, "appType must not be null");
        pushToken = normalizeNullable(pushToken);
        Objects.requireNonNull(bindStatus, "bindStatus must not be null");
        Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static DeviceBinding create(
            UUID id,
            UUID personId,
            UUID accountId,
            String deviceId,
            String deviceFingerprint,
            MobilePlatform platform,
            MobileAppType appType,
            String pushToken,
            UUID tenantId,
            Instant now
    ) {
        return new DeviceBinding(
                id,
                personId,
                accountId,
                deviceId,
                deviceFingerprint,
                platform,
                appType,
                pushToken,
                DeviceBindStatus.ACTIVE,
                MobileRiskLevel.LOW,
                now,
                now,
                tenantId,
                now,
                now
        );
    }

    public boolean isActive() {
        return bindStatus == DeviceBindStatus.ACTIVE;
    }

    public DeviceBinding bindAgain(
            UUID personId,
            UUID accountId,
            String deviceFingerprint,
            MobilePlatform platform,
            MobileAppType appType,
            String pushToken,
            Instant now
    ) {
        ensureCanBind();
        return new DeviceBinding(
                id,
                personId,
                accountId,
                deviceId,
                deviceFingerprint,
                platform,
                appType,
                pushToken,
                DeviceBindStatus.ACTIVE,
                riskLevel,
                now,
                now,
                tenantId,
                createdAt,
                now
        );
    }

    public DeviceBinding updatePushToken(String pushToken, Instant now) {
        ensureActive("Only active devices can update push token");
        return new DeviceBinding(
                id,
                personId,
                accountId,
                deviceId,
                deviceFingerprint,
                platform,
                appType,
                pushToken,
                bindStatus,
                riskLevel,
                lastLoginAt,
                now,
                tenantId,
                createdAt,
                now
        );
    }

    public DeviceBinding updateRiskLevel(MobileRiskLevel riskLevel, Instant now) {
        return new DeviceBinding(
                id,
                personId,
                accountId,
                deviceId,
                deviceFingerprint,
                platform,
                appType,
                pushToken,
                bindStatus,
                riskLevel,
                lastLoginAt,
                lastSeenAt,
                tenantId,
                createdAt,
                now
        );
    }

    public DeviceBinding revoke(Instant now) {
        if (bindStatus == DeviceBindStatus.REVOKED) {
            return this;
        }
        return withStatus(DeviceBindStatus.REVOKED, null, now);
    }

    public DeviceBinding markLost(Instant now) {
        return withStatus(DeviceBindStatus.LOST, null, now);
    }

    public DeviceBinding disable(Instant now) {
        return withStatus(DeviceBindStatus.DISABLED, null, now);
    }

    public DeviceBindingView toView() {
        return new DeviceBindingView(
                id,
                personId,
                accountId,
                deviceId,
                deviceFingerprint,
                platform,
                appType,
                pushToken,
                bindStatus,
                riskLevel,
                lastLoginAt,
                lastSeenAt,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    private DeviceBinding withStatus(DeviceBindStatus status, String pushToken, Instant now) {
        return new DeviceBinding(
                id,
                personId,
                accountId,
                deviceId,
                deviceFingerprint,
                platform,
                appType,
                pushToken,
                status,
                riskLevel,
                lastLoginAt,
                lastSeenAt,
                tenantId,
                createdAt,
                now
        );
    }

    private void ensureCanBind() {
        if (bindStatus == DeviceBindStatus.REVOKED || bindStatus == DeviceBindStatus.LOST) {
            throw new IllegalStateException("Device must be restored before binding again");
        }
        if (bindStatus == DeviceBindStatus.DISABLED) {
            throw new IllegalStateException("Device is disabled");
        }
    }

    private void ensureActive(String message) {
        if (!isActive()) {
            throw new IllegalStateException(message);
        }
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
