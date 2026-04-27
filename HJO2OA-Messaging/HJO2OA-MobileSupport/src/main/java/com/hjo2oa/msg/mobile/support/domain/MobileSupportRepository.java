package com.hjo2oa.msg.mobile.support.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MobileSupportRepository {

    Optional<DeviceBinding> findDeviceBindingById(UUID id);

    Optional<DeviceBinding> findDeviceBindingByTenantAndDeviceId(UUID tenantId, String deviceId);

    List<DeviceBinding> findDeviceBindingsByPerson(UUID tenantId, UUID personId, DeviceBindStatus bindStatus);

    DeviceBinding saveDeviceBinding(DeviceBinding deviceBinding);

    Optional<MobileSession> findMobileSessionById(UUID sessionId);

    List<MobileSession> findMobileSessionsByDeviceBinding(UUID tenantId, UUID deviceBindingId, MobileSessionStatus status);

    List<MobileSession> findMobileSessionsByPerson(UUID tenantId, UUID personId, MobileSessionStatus status);

    MobileSession saveMobileSession(MobileSession mobileSession);
}
