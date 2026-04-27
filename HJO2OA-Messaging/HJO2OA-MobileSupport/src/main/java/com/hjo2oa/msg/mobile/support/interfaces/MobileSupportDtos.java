package com.hjo2oa.msg.mobile.support.interfaces;

import com.hjo2oa.msg.mobile.support.application.MobileSupportCommands;
import com.hjo2oa.msg.mobile.support.domain.DeviceBindStatus;
import com.hjo2oa.msg.mobile.support.domain.MobileAppType;
import com.hjo2oa.msg.mobile.support.domain.MobilePlatform;
import com.hjo2oa.msg.mobile.support.domain.MobileRiskLevel;
import com.hjo2oa.msg.mobile.support.domain.MobileSessionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class MobileSupportDtos {

    private MobileSupportDtos() {
    }

    public record BindDeviceRequest(
            @NotNull UUID tenantId,
            @NotNull UUID personId,
            @NotNull UUID accountId,
            @NotBlank @Size(max = 128) String deviceId,
            @Size(max = 256) String deviceFingerprint,
            @NotNull MobilePlatform platform,
            @NotNull MobileAppType appType,
            @Size(max = 512) String pushToken
    ) {

        public MobileSupportCommands.BindDeviceCommand toCommand() {
            return new MobileSupportCommands.BindDeviceCommand(
                    tenantId,
                    personId,
                    accountId,
                    deviceId,
                    deviceFingerprint,
                    platform,
                    appType,
                    pushToken
            );
        }
    }

    public record UpdatePushTokenRequest(
            @NotNull UUID tenantId,
            @NotNull UUID personId,
            @NotBlank @Size(max = 512) String pushToken
    ) {

        public MobileSupportCommands.UpdatePushTokenCommand toCommand(String deviceId) {
            return new MobileSupportCommands.UpdatePushTokenCommand(tenantId, personId, deviceId, pushToken);
        }
    }

    public record CreateSessionRequest(
            @NotNull UUID tenantId,
            @NotNull UUID personId,
            @NotNull UUID accountId,
            @NotBlank @Size(max = 128) String deviceId,
            UUID currentAssignmentId,
            UUID currentPositionId,
            @Positive Long ttlSeconds
    ) {

        public MobileSupportCommands.CreateSessionCommand toCommand() {
            return new MobileSupportCommands.CreateSessionCommand(
                    tenantId,
                    personId,
                    accountId,
                    deviceId,
                    currentAssignmentId,
                    currentPositionId,
                    ttlSeconds == null ? null : Duration.ofSeconds(ttlSeconds)
            );
        }
    }

    public record RefreshSessionRequest(
            @NotNull UUID tenantId,
            @PositiveOrZero int refreshVersion,
            @Positive Long ttlSeconds
    ) {

        public MobileSupportCommands.RefreshSessionCommand toCommand(UUID sessionId) {
            return new MobileSupportCommands.RefreshSessionCommand(
                    tenantId,
                    sessionId,
                    refreshVersion,
                    ttlSeconds == null ? null : Duration.ofSeconds(ttlSeconds)
            );
        }
    }

    public record UpdateSessionIdentitySnapshotRequest(
            @NotNull UUID tenantId,
            UUID currentAssignmentId,
            UUID currentPositionId
    ) {

        public MobileSupportCommands.UpdateSessionIdentitySnapshotCommand toCommand(UUID sessionId) {
            return new MobileSupportCommands.UpdateSessionIdentitySnapshotCommand(
                    tenantId,
                    sessionId,
                    currentAssignmentId,
                    currentPositionId
            );
        }
    }

    public record RevokeDeviceRequest(
            @NotNull UUID tenantId,
            @Size(max = 512) String reason
    ) {

        public MobileSupportCommands.RevokeDeviceCommand toCommand(String deviceId) {
            return new MobileSupportCommands.RevokeDeviceCommand(tenantId, deviceId, reason);
        }
    }

    public record FreezeSessionRequest(
            @NotNull UUID tenantId,
            MobileRiskLevel riskLevel,
            @Size(max = 512) String reason
    ) {

        public MobileSupportCommands.FreezeSessionCommand toCommand(UUID sessionId) {
            return new MobileSupportCommands.FreezeSessionCommand(tenantId, sessionId, riskLevel, reason);
        }
    }

    public record DeviceBindingResponse(
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

    public record MobileSessionResponse(
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
    }
}
