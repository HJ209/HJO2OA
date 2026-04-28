package com.hjo2oa.infra.timezone.infrastructure;

import com.hjo2oa.infra.timezone.domain.TimezoneScopeType;
import com.hjo2oa.infra.timezone.domain.TimezoneSetting;
import com.hjo2oa.infra.timezone.domain.TimezoneSettingRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryTimezoneSettingRepository implements TimezoneSettingRepository {

    private final Map<UUID, TimezoneSetting> settings = new ConcurrentHashMap<>();
    private final Clock clock;
    @Autowired
    public InMemoryTimezoneSettingRepository() {
        this(Clock.systemUTC());
    }
    public InMemoryTimezoneSettingRepository(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Optional<TimezoneSetting> findByScope(TimezoneScopeType scopeType, UUID scopeId) {
        Objects.requireNonNull(scopeType, "scopeType must not be null");
        return settings.values().stream()
                .filter(setting -> setting.scopeType() == scopeType)
                .filter(setting -> Objects.equals(setting.scopeId(), scopeId))
                .sorted(latestFirst())
                .findFirst();
    }

    @Override
    public Optional<TimezoneSetting> findDefault() {
        return currentSetting(TimezoneScopeType.SYSTEM, null)
                .filter(TimezoneSetting::isDefault);
    }

    @Override
    public Optional<TimezoneSetting> findEffectiveForTenant(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        return currentSetting(TimezoneScopeType.TENANT, tenantId);
    }

    @Override
    public Optional<TimezoneSetting> findEffectiveForPerson(UUID personId) {
        Objects.requireNonNull(personId, "personId must not be null");
        return currentSetting(TimezoneScopeType.PERSON, personId);
    }

    @Override
    public TimezoneSetting save(TimezoneSetting setting) {
        settings.put(setting.id(), setting);
        return setting;
    }

    private Optional<TimezoneSetting> currentSetting(TimezoneScopeType scopeType, UUID scopeId) {
        Instant now = clock.instant();
        return settings.values().stream()
                .filter(setting -> setting.scopeType() == scopeType)
                .filter(setting -> Objects.equals(setting.scopeId(), scopeId))
                .filter(TimezoneSetting::active)
                .filter(setting -> setting.effectiveFrom() == null || !setting.effectiveFrom().isAfter(now))
                .sorted(latestFirst())
                .findFirst();
    }

    private Comparator<TimezoneSetting> latestFirst() {
        return Comparator.comparing(TimezoneSetting::updatedAt)
                .thenComparing(setting -> effectiveAt(setting.effectiveFrom()))
                .thenComparing(TimezoneSetting::createdAt)
                .reversed();
    }

    private Instant effectiveAt(Instant effectiveFrom) {
        return effectiveFrom == null ? Instant.EPOCH : effectiveFrom;
    }
}
