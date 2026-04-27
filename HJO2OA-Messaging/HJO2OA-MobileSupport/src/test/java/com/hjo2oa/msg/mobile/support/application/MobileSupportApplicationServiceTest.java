package com.hjo2oa.msg.mobile.support.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.msg.mobile.support.domain.DeviceBindStatus;
import com.hjo2oa.msg.mobile.support.domain.MobileAppType;
import com.hjo2oa.msg.mobile.support.domain.MobilePlatform;
import com.hjo2oa.msg.mobile.support.domain.MobileSessionStatus;
import com.hjo2oa.msg.mobile.support.infrastructure.InMemoryMobileSupportRepository;
import com.hjo2oa.shared.kernel.BizException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MobileSupportApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-27T01:00:00Z");
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PERSON_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ACCOUNT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ASSIGNMENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID POSITION_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Test
    void shouldBindDeviceCreateSessionAndRefreshWithVersion() {
        MobileSupportApplicationService service = service();

        var binding = service.bindDevice(bindCommand("push-token-1"));
        var session = service.createSession(new MobileSupportCommands.CreateSessionCommand(
                TENANT_ID,
                PERSON_ID,
                ACCOUNT_ID,
                "device-1",
                ASSIGNMENT_ID,
                POSITION_ID,
                Duration.ofHours(2)
        ));
        var refreshed = service.refreshSession(new MobileSupportCommands.RefreshSessionCommand(
                TENANT_ID,
                session.id(),
                0,
                Duration.ofHours(3)
        ));

        assertThat(binding.bindStatus()).isEqualTo(DeviceBindStatus.ACTIVE);
        assertThat(session.sessionStatus()).isEqualTo(MobileSessionStatus.ACTIVE);
        assertThat(refreshed.refreshVersion()).isEqualTo(1);
        assertThat(refreshed.lastHeartbeatAt()).isEqualTo(FIXED_TIME);
        assertThat(refreshed.expiresAt()).isEqualTo(FIXED_TIME.plus(Duration.ofHours(3)));
    }

    @Test
    void shouldRevokeDeviceClearTokenAndRevokeActiveSessions() {
        MobileSupportApplicationService service = service();
        service.bindDevice(bindCommand("push-token-1"));
        var session = service.createSession(new MobileSupportCommands.CreateSessionCommand(
                TENANT_ID,
                PERSON_ID,
                ACCOUNT_ID,
                "device-1",
                ASSIGNMENT_ID,
                POSITION_ID,
                null
        ));

        var revoked = service.revokeDevice(new MobileSupportCommands.RevokeDeviceCommand(
                TENANT_ID,
                "device-1",
                "lost"
        ));

        assertThat(revoked.bindStatus()).isEqualTo(DeviceBindStatus.REVOKED);
        assertThat(revoked.pushToken()).isNull();
        assertThatThrownBy(() -> service.refreshSession(new MobileSupportCommands.RefreshSessionCommand(
                TENANT_ID,
                session.id(),
                0,
                null
        ))).isInstanceOf(BizException.class);
    }

    @Test
    void shouldRejectRefreshVersionConflict() {
        MobileSupportApplicationService service = service();
        service.bindDevice(bindCommand("push-token-1"));
        var session = service.createSession(new MobileSupportCommands.CreateSessionCommand(
                TENANT_ID,
                PERSON_ID,
                ACCOUNT_ID,
                "device-1",
                ASSIGNMENT_ID,
                POSITION_ID,
                null
        ));

        assertThatThrownBy(() -> service.refreshSession(new MobileSupportCommands.RefreshSessionCommand(
                TENANT_ID,
                session.id(),
                9,
                null
        ))).isInstanceOf(BizException.class);
    }

    private MobileSupportCommands.BindDeviceCommand bindCommand(String pushToken) {
        return new MobileSupportCommands.BindDeviceCommand(
                TENANT_ID,
                PERSON_ID,
                ACCOUNT_ID,
                "device-1",
                "fingerprint-1",
                MobilePlatform.IOS,
                MobileAppType.NATIVE_APP,
                pushToken
        );
    }

    private MobileSupportApplicationService service() {
        return new MobileSupportApplicationService(
                new InMemoryMobileSupportRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
