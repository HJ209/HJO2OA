package com.hjo2oa.infra.timezone.application;

import com.hjo2oa.infra.timezone.domain.ResolvedTimezoneView;
import com.hjo2oa.infra.timezone.domain.TimezoneScopeType;
import com.hjo2oa.infra.timezone.domain.TimezoneSetting;
import com.hjo2oa.infra.timezone.domain.TimezoneSettingRepository;
import com.hjo2oa.infra.timezone.domain.TimezoneSettingView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TimezoneSettingApplicationService {

    private final TimezoneSettingRepository repository;
    private final Clock clock;
    private final ConcurrentMap<TimezoneCacheKey, ResolvedTimezoneView> resolveCache = new ConcurrentHashMap<>();

    @Autowired
    public TimezoneSettingApplicationService(TimezoneSettingRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public TimezoneSettingApplicationService(TimezoneSettingRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public TimezoneSettingView setSystemDefault(String timezoneId) {
        String normalizedTimezoneId = normalizeTimezoneId(timezoneId);
        Instant now = now();
        TimezoneSetting timezoneSetting = repository.findByScope(TimezoneScopeType.SYSTEM, null)
                .map(existing -> existing.update(normalizedTimezoneId, true, now, true, null, now))
                .orElseGet(() -> TimezoneSetting.create(
                        TimezoneScopeType.SYSTEM,
                        null,
                        normalizedTimezoneId,
                        true,
                        now,
                        null,
                        now
                ));
        return saveAndInvalidate(timezoneSetting).toView();
    }

    public TimezoneSettingView setTenantTimezone(UUID tenantId, String timezoneId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        String normalizedTimezoneId = normalizeTimezoneId(timezoneId);
        Instant now = now();
        TimezoneSetting timezoneSetting = repository.findByScope(TimezoneScopeType.TENANT, tenantId)
                .map(existing -> existing.update(normalizedTimezoneId, true, now, true, tenantId, now))
                .orElseGet(() -> TimezoneSetting.create(
                        TimezoneScopeType.TENANT,
                        tenantId,
                        normalizedTimezoneId,
                        true,
                        now,
                        tenantId,
                        now
                ));
        return saveAndInvalidate(timezoneSetting).toView();
    }

    public TimezoneSettingView setPersonTimezone(UUID personId, String timezoneId) {
        Objects.requireNonNull(personId, "personId must not be null");
        String normalizedTimezoneId = normalizeTimezoneId(timezoneId);
        Instant now = now();
        TimezoneSetting timezoneSetting = repository.findByScope(TimezoneScopeType.PERSON, personId)
                .map(existing -> existing.update(
                        normalizedTimezoneId,
                        true,
                        now,
                        true,
                        existing.tenantId(),
                        now
                ))
                .orElseGet(() -> TimezoneSetting.create(
                        TimezoneScopeType.PERSON,
                        personId,
                        normalizedTimezoneId,
                        true,
                        now,
                        null,
                        now
                ));
        return saveAndInvalidate(timezoneSetting).toView();
    }

    public ResolvedTimezoneView resolveEffectiveTimezone(UUID tenantId, UUID personId) {
        TimezoneCacheKey cacheKey = new TimezoneCacheKey(tenantId, personId);
        return resolveCache.computeIfAbsent(cacheKey, key -> resolveEffectiveTimezoneUncached(tenantId, personId));
    }

    private ResolvedTimezoneView resolveEffectiveTimezoneUncached(UUID tenantId, UUID personId) {
        if (personId != null) {
            return repository.findEffectiveForPerson(personId)
                    .map(setting -> ResolvedTimezoneView.from(setting, tenantId, personId))
                    .orElseGet(() -> resolveTenantOrSystem(tenantId, personId));
        }
        return resolveTenantOrSystem(tenantId, null);
    }

    public List<TimezoneSettingView> listSettings(UUID tenantId, TimezoneScopeType scopeType) {
        return repository.findAll().stream()
                .filter(setting -> tenantId == null || tenantMatches(setting, tenantId))
                .filter(setting -> scopeType == null || setting.scopeType() == scopeType)
                .sorted(Comparator.comparing(TimezoneSetting::scopeType)
                        .thenComparing(setting -> setting.scopeId() == null ? "" : setting.scopeId().toString()))
                .map(TimezoneSetting::toView)
                .toList();
    }

    public Instant convertToUtc(LocalDateTime localDateTime, String timezoneId) {
        Objects.requireNonNull(localDateTime, "localDateTime must not be null");
        return localDateTime.atZone(parseZoneId(timezoneId)).toInstant();
    }

    public LocalDateTime convertFromUtc(Instant utcInstant, String timezoneId) {
        Objects.requireNonNull(utcInstant, "utcInstant must not be null");
        return LocalDateTime.ofInstant(utcInstant, parseZoneId(timezoneId));
    }

    private ResolvedTimezoneView resolveTenantOrSystem(UUID tenantId, UUID personId) {
        if (tenantId != null) {
            return repository.findEffectiveForTenant(tenantId)
                    .map(setting -> ResolvedTimezoneView.from(setting, tenantId, personId))
                    .orElseGet(() -> resolveSystem(tenantId, personId));
        }
        return resolveSystem(tenantId, personId);
    }

    private ResolvedTimezoneView resolveSystem(UUID tenantId, UUID personId) {
        return repository.findDefault()
                .map(setting -> ResolvedTimezoneView.from(setting, tenantId, personId))
                .orElseGet(() -> ResolvedTimezoneView.fallbackUtc(tenantId, personId));
    }

    private Instant now() {
        return clock.instant();
    }

    private TimezoneSetting saveAndInvalidate(TimezoneSetting setting) {
        TimezoneSetting saved = repository.save(setting);
        invalidateCaches();
        return saved;
    }

    public void invalidateCaches() {
        resolveCache.clear();
    }

    private boolean tenantMatches(TimezoneSetting setting, UUID tenantId) {
        return setting.tenantId() == null
                || setting.tenantId().equals(tenantId)
                || Objects.equals(setting.scopeId(), tenantId);
    }

    private String normalizeTimezoneId(String timezoneId) {
        return parseZoneId(timezoneId).getId();
    }

    private ZoneId parseZoneId(String timezoneId) {
        Objects.requireNonNull(timezoneId, "timezoneId must not be null");
        String normalized = timezoneId.trim();
        if (normalized.isEmpty()) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "Timezone ID must not be blank"
            );
        }
        try {
            return ZoneId.of(normalized);
        } catch (DateTimeException ex) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "Invalid timezone ID"
            );
        }
    }

    private record TimezoneCacheKey(UUID tenantId, UUID personId) {
    }
}
