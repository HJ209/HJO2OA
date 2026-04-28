package com.hjo2oa.msg.mobile.support.application;

import com.hjo2oa.msg.mobile.support.domain.DeviceBindStatus;
import com.hjo2oa.msg.mobile.support.domain.DeviceBinding;
import com.hjo2oa.msg.mobile.support.domain.DeviceBindingView;
import com.hjo2oa.msg.mobile.support.domain.MobileRiskLevel;
import com.hjo2oa.msg.mobile.support.domain.MobileSession;
import com.hjo2oa.msg.mobile.support.domain.MobileSessionStatus;
import com.hjo2oa.msg.mobile.support.domain.MobileSessionView;
import com.hjo2oa.msg.mobile.support.domain.MobileSupportRepository;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class MobileSupportApplicationService {

    private static final Duration DEFAULT_SESSION_TTL = Duration.ofDays(30);
    private static final Comparator<MobileSession> LATEST_SESSION_ORDER =
            Comparator.comparing(MobileSession::updatedAt).reversed();

    private final MobileSupportRepository repository;
    private final Clock clock;
    @Autowired
    public MobileSupportApplicationService(MobileSupportRepository repository) {
        this(repository, Clock.systemUTC());
    }
    public MobileSupportApplicationService(MobileSupportRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public DeviceBindingView bindDevice(MobileSupportCommands.BindDeviceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Instant now = now();
        DeviceBinding binding = repository
                .findDeviceBindingByTenantAndDeviceId(command.tenantId(), command.deviceId())
                .map(existing -> existing.bindAgain(
                        command.personId(),
                        command.accountId(),
                        command.deviceFingerprint(),
                        command.platform(),
                        command.appType(),
                        command.pushToken(),
                        now
                ))
                .orElseGet(() -> DeviceBinding.create(
                        UUID.randomUUID(),
                        command.personId(),
                        command.accountId(),
                        command.deviceId(),
                        command.deviceFingerprint(),
                        command.platform(),
                        command.appType(),
                        command.pushToken(),
                        command.tenantId(),
                        now
                ));
        return repository.saveDeviceBinding(binding).toView();
    }

    public DeviceBindingView updatePushToken(MobileSupportCommands.UpdatePushTokenCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        DeviceBinding binding = loadDeviceByTenantAndDeviceId(command.tenantId(), command.deviceId());
        ensureSamePerson(binding, command.personId());
        return repository.saveDeviceBinding(binding.updatePushToken(command.pushToken(), now())).toView();
    }

    public MobileSessionView createSession(MobileSupportCommands.CreateSessionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Instant now = now();
        DeviceBinding binding = loadDeviceByTenantAndDeviceId(command.tenantId(), command.deviceId());
        ensureSamePerson(binding, command.personId());
        if (!binding.accountId().equals(command.accountId())) {
            throw new BizException(SharedErrorDescriptors.FORBIDDEN, "Device does not belong to current account");
        }
        if (!binding.isActive()) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Device is not active");
        }
        MobileSession session = MobileSession.issue(
                UUID.randomUUID(),
                binding,
                command.currentAssignmentId(),
                command.currentPositionId(),
                now,
                now.plus(sessionTtl(command.ttl()))
        );
        return repository.saveMobileSession(session).toView();
    }

    public MobileSessionView refreshSession(MobileSupportCommands.RefreshSessionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Instant now = now();
        MobileSession session = loadSession(command.tenantId(), command.sessionId());
        DeviceBinding binding = repository.findDeviceBindingById(session.deviceBindingId())
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Device binding not found"));
        if (!binding.isActive()) {
            MobileSession revoked = repository.saveMobileSession(session.revoke(now));
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Device is not active: " + revoked.sessionStatus());
        }
        try {
            MobileSession refreshed = session.refresh(
                    command.refreshVersion(),
                    binding.riskLevel(),
                    now,
                    now.plus(sessionTtl(command.ttl()))
            );
            MobileSession saved = repository.saveMobileSession(refreshed);
            if (saved.sessionStatus() == MobileSessionStatus.EXPIRED) {
                throw new BizException(SharedErrorDescriptors.UNAUTHORIZED, "Mobile session expired");
            }
            if (saved.sessionStatus() == MobileSessionStatus.RISK_FROZEN) {
                throw new BizException(SharedErrorDescriptors.CONFLICT, "Mobile session is risk frozen");
            }
            return saved.toView();
        } catch (IllegalStateException ex) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, ex.getMessage());
        }
    }

    public MobileSessionView currentSession(UUID tenantId, UUID personId) {
        return repository.findMobileSessionsByPerson(tenantId, personId, MobileSessionStatus.ACTIVE).stream()
                .sorted(LATEST_SESSION_ORDER)
                .filter(session -> session.isActiveAt(now()))
                .findFirst()
                .map(MobileSession::toView)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Mobile session not found"));
    }

    public MobileSessionView updateSessionIdentitySnapshot(
            MobileSupportCommands.UpdateSessionIdentitySnapshotCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        MobileSession session = loadSession(command.tenantId(), command.sessionId());
        return repository.saveMobileSession(session.updateIdentitySnapshot(
                command.currentAssignmentId(),
                command.currentPositionId(),
                now()
        )).toView();
    }

    public DeviceBindingView revokeDevice(MobileSupportCommands.RevokeDeviceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Instant now = now();
        DeviceBinding binding = loadDeviceByTenantAndDeviceId(command.tenantId(), command.deviceId()).revoke(now);
        DeviceBinding saved = repository.saveDeviceBinding(binding);
        repository.findMobileSessionsByDeviceBinding(
                        command.tenantId(),
                        saved.id(),
                        MobileSessionStatus.ACTIVE
                )
                .forEach(session -> repository.saveMobileSession(session.revoke(now)));
        return saved.toView();
    }

    public MobileSessionView freezeSession(MobileSupportCommands.FreezeSessionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        MobileSession session = loadSession(command.tenantId(), command.sessionId());
        MobileRiskLevel riskLevel = command.riskLevel() == null ? MobileRiskLevel.HIGH : command.riskLevel();
        return repository.saveMobileSession(session.freeze(riskLevel, command.reason(), now())).toView();
    }

    public List<DeviceBindingView> activeDevices(UUID tenantId, UUID personId) {
        return repository.findDeviceBindingsByPerson(tenantId, personId, DeviceBindStatus.ACTIVE).stream()
                .map(DeviceBinding::toView)
                .toList();
    }

    private DeviceBinding loadDeviceByTenantAndDeviceId(UUID tenantId, String deviceId) {
        return repository.findDeviceBindingByTenantAndDeviceId(tenantId, deviceId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Device binding not found"));
    }

    private MobileSession loadSession(UUID tenantId, UUID sessionId) {
        MobileSession session = repository.findMobileSessionById(sessionId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Mobile session not found"));
        if (!session.tenantId().equals(tenantId)) {
            throw new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Mobile session not found");
        }
        return session;
    }

    private void ensureSamePerson(DeviceBinding binding, UUID personId) {
        if (!binding.personId().equals(personId)) {
            throw new BizException(SharedErrorDescriptors.FORBIDDEN, "Device does not belong to current person");
        }
    }

    private Duration sessionTtl(Duration requestedTtl) {
        if (requestedTtl == null || requestedTtl.isNegative() || requestedTtl.isZero()) {
            return DEFAULT_SESSION_TTL;
        }
        return requestedTtl;
    }

    private Instant now() {
        return clock.instant();
    }
}
