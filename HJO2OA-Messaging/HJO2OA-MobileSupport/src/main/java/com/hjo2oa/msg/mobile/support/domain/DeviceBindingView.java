package com.hjo2oa.msg.mobile.support.domain;

import java.time.Instant;
import java.util.UUID;

public record DeviceBindingView(
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
}
