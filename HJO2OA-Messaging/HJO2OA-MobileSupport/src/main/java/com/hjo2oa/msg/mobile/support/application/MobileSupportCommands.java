package com.hjo2oa.msg.mobile.support.application;

import com.hjo2oa.msg.mobile.support.domain.MobileAppType;
import com.hjo2oa.msg.mobile.support.domain.MobilePlatform;
import com.hjo2oa.msg.mobile.support.domain.MobileRiskLevel;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class MobileSupportCommands {

    private MobileSupportCommands() {
    }

    public record BindDeviceCommand(
            UUID tenantId,
            UUID personId,
            UUID accountId,
            String deviceId,
            String deviceFingerprint,
            MobilePlatform platform,
            MobileAppType appType,
            String pushToken
    ) {
    }

    public record UpdatePushTokenCommand(
            UUID tenantId,
            UUID personId,
            String deviceId,
            String pushToken
    ) {
    }

    public record CreateSessionCommand(
            UUID tenantId,
            UUID personId,
            UUID accountId,
            String deviceId,
            UUID currentAssignmentId,
            UUID currentPositionId,
            Duration ttl
    ) {
    }

    public record RefreshSessionCommand(
            UUID tenantId,
            UUID sessionId,
            int refreshVersion,
            Duration ttl
    ) {
    }

    public record UpdateSessionIdentitySnapshotCommand(
            UUID tenantId,
            UUID sessionId,
            UUID currentAssignmentId,
            UUID currentPositionId
    ) {
    }

    public record RevokeDeviceCommand(
            UUID tenantId,
            String deviceId,
            String reason
    ) {
    }

    public record FreezeSessionCommand(
            UUID tenantId,
            UUID sessionId,
            MobileRiskLevel riskLevel,
            String reason
    ) {
    }

    public record SavePushPreferenceCommand(
            UUID tenantId,
            UUID personId,
            boolean pushEnabled,
            LocalTime quietStartsAt,
            LocalTime quietEndsAt,
            List<String> mutedCategories
    ) {

        public SavePushPreferenceCommand {
            mutedCategories = mutedCategories == null ? List.of() : List.copyOf(mutedCategories);
        }
    }
}
