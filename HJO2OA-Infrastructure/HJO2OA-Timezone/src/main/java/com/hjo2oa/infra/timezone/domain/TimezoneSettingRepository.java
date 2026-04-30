package com.hjo2oa.infra.timezone.domain;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface TimezoneSettingRepository {

    Optional<TimezoneSetting> findByScope(TimezoneScopeType scopeType, UUID scopeId);

    Optional<TimezoneSetting> findDefault();

    Optional<TimezoneSetting> findEffectiveForTenant(UUID tenantId);

    Optional<TimezoneSetting> findEffectiveForPerson(UUID personId);

    List<TimezoneSetting> findAll();

    TimezoneSetting save(TimezoneSetting setting);
}
