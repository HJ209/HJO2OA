package com.hjo2oa.msg.mobile.support.infrastructure;

import com.hjo2oa.msg.mobile.support.domain.DeviceBindStatus;
import com.hjo2oa.msg.mobile.support.domain.DeviceBinding;
import com.hjo2oa.msg.mobile.support.domain.MobilePushPreference;
import com.hjo2oa.msg.mobile.support.domain.MobileSession;
import com.hjo2oa.msg.mobile.support.domain.MobileSessionStatus;
import com.hjo2oa.msg.mobile.support.domain.MobileSupportRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryMobileSupportRepository implements MobileSupportRepository {

    private static final Comparator<MobileSession> SESSION_ORDER =
            Comparator.comparing(MobileSession::updatedAt).reversed();

    private final Map<UUID, DeviceBinding> deviceBindingsById = new LinkedHashMap<>();
    private final Map<UUID, MobileSession> mobileSessionsById = new LinkedHashMap<>();
    private final Map<UUID, MobilePushPreference> pushPreferencesById = new LinkedHashMap<>();

    @Override
    public Optional<DeviceBinding> findDeviceBindingById(UUID id) {
        return Optional.ofNullable(deviceBindingsById.get(id));
    }

    @Override
    public Optional<DeviceBinding> findDeviceBindingByTenantAndDeviceId(UUID tenantId, String deviceId) {
        return deviceBindingsById.values().stream()
                .filter(binding -> binding.tenantId().equals(tenantId))
                .filter(binding -> binding.deviceId().equals(deviceId))
                .findFirst();
    }

    @Override
    public List<DeviceBinding> findDeviceBindingsByPerson(UUID tenantId, UUID personId, DeviceBindStatus bindStatus) {
        return deviceBindingsById.values().stream()
                .filter(binding -> binding.tenantId().equals(tenantId))
                .filter(binding -> binding.personId().equals(personId))
                .filter(binding -> bindStatus == null || binding.bindStatus() == bindStatus)
                .toList();
    }

    @Override
    public DeviceBinding saveDeviceBinding(DeviceBinding deviceBinding) {
        deviceBindingsById.put(deviceBinding.id(), Objects.requireNonNull(deviceBinding, "deviceBinding must not be null"));
        return deviceBinding;
    }

    @Override
    public Optional<MobileSession> findMobileSessionById(UUID sessionId) {
        return Optional.ofNullable(mobileSessionsById.get(sessionId));
    }

    @Override
    public List<MobileSession> findMobileSessionsByDeviceBinding(
            UUID tenantId,
            UUID deviceBindingId,
            MobileSessionStatus status
    ) {
        return mobileSessionsById.values().stream()
                .filter(session -> session.tenantId().equals(tenantId))
                .filter(session -> session.deviceBindingId().equals(deviceBindingId))
                .filter(session -> status == null || session.sessionStatus() == status)
                .sorted(SESSION_ORDER)
                .toList();
    }

    @Override
    public List<MobileSession> findMobileSessionsByPerson(UUID tenantId, UUID personId, MobileSessionStatus status) {
        return mobileSessionsById.values().stream()
                .filter(session -> session.tenantId().equals(tenantId))
                .filter(session -> session.personId().equals(personId))
                .filter(session -> status == null || session.sessionStatus() == status)
                .sorted(SESSION_ORDER)
                .toList();
    }

    @Override
    public MobileSession saveMobileSession(MobileSession mobileSession) {
        mobileSessionsById.put(mobileSession.id(), Objects.requireNonNull(mobileSession, "mobileSession must not be null"));
        return mobileSession;
    }

    @Override
    public Optional<MobilePushPreference> findPushPreference(UUID tenantId, UUID personId) {
        return pushPreferencesById.values().stream()
                .filter(preference -> preference.tenantId().equals(tenantId))
                .filter(preference -> preference.personId().equals(personId))
                .findFirst();
    }

    @Override
    public MobilePushPreference savePushPreference(MobilePushPreference preference) {
        pushPreferencesById.put(preference.id(), Objects.requireNonNull(preference, "preference must not be null"));
        return preference;
    }
}
