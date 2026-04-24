package com.hjo2oa.infra.timezone.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.infra.timezone.domain.ResolvedTimezoneView;
import com.hjo2oa.infra.timezone.domain.TimezoneScopeType;
import com.hjo2oa.infra.timezone.domain.TimezoneSettingView;
import com.hjo2oa.infra.timezone.infrastructure.InMemoryTimezoneSettingRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TimezoneSettingApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T08:00:00Z");

    @Test
    void shouldResolveEffectiveTimezoneByPersonTenantAndSystemPriority() {
        InMemoryTimezoneSettingRepository repository = new InMemoryTimezoneSettingRepository(fixedClock());
        TimezoneSettingApplicationService applicationService =
                new TimezoneSettingApplicationService(repository, fixedClock());
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID personId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        applicationService.setSystemDefault("UTC");
        applicationService.setTenantTimezone(tenantId, "Europe/Berlin");
        applicationService.setPersonTimezone(personId, "America/New_York");

        ResolvedTimezoneView personResolved = applicationService.resolveEffectiveTimezone(tenantId, personId);
        ResolvedTimezoneView tenantResolved = applicationService.resolveEffectiveTimezone(tenantId, null);
        ResolvedTimezoneView systemResolved = applicationService.resolveEffectiveTimezone(null, null);

        assertThat(personResolved.scopeType()).isEqualTo(TimezoneScopeType.PERSON);
        assertThat(personResolved.timezoneId()).isEqualTo("America/New_York");
        assertThat(tenantResolved.scopeType()).isEqualTo(TimezoneScopeType.TENANT);
        assertThat(tenantResolved.timezoneId()).isEqualTo("Europe/Berlin");
        assertThat(systemResolved.scopeType()).isEqualTo(TimezoneScopeType.SYSTEM);
        assertThat(systemResolved.timezoneId()).isEqualTo("UTC");
    }

    @Test
    void shouldFallbackToUtcWhenNoSettingExists() {
        TimezoneSettingApplicationService applicationService = new TimezoneSettingApplicationService(
                new InMemoryTimezoneSettingRepository(fixedClock()),
                fixedClock()
        );
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID personId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        ResolvedTimezoneView resolvedTimezone = applicationService.resolveEffectiveTimezone(tenantId, personId);

        assertThat(resolvedTimezone.scopeType()).isEqualTo(TimezoneScopeType.SYSTEM);
        assertThat(resolvedTimezone.timezoneId()).isEqualTo("UTC");
        assertThat(resolvedTimezone.settingId()).isNull();
    }

    @Test
    void shouldUpdateExistingScopeSettingInsteadOfCreatingDuplicateView() {
        TimezoneSettingApplicationService applicationService = new TimezoneSettingApplicationService(
                new InMemoryTimezoneSettingRepository(fixedClock()),
                fixedClock()
        );
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        TimezoneSettingView first = applicationService.setTenantTimezone(tenantId, "Asia/Shanghai");
        TimezoneSettingView second = applicationService.setTenantTimezone(tenantId, "Europe/Berlin");

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.timezoneId()).isEqualTo("Europe/Berlin");
        assertThat(applicationService.resolveEffectiveTimezone(tenantId, null).timezoneId()).isEqualTo("Europe/Berlin");
    }

    @Test
    void shouldConvertBetweenLocalDateTimeAndUtc() {
        TimezoneSettingApplicationService applicationService = new TimezoneSettingApplicationService(
                new InMemoryTimezoneSettingRepository(fixedClock()),
                fixedClock()
        );

        Instant utcInstant = applicationService.convertToUtc(
                LocalDateTime.parse("2026-04-20T16:00:00"),
                "Asia/Shanghai"
        );
        LocalDateTime localDateTime = applicationService.convertFromUtc(
                Instant.parse("2026-04-20T08:00:00Z"),
                "Asia/Shanghai"
        );

        assertThat(utcInstant).isEqualTo(Instant.parse("2026-04-20T08:00:00Z"));
        assertThat(localDateTime).isEqualTo(LocalDateTime.parse("2026-04-20T16:00:00"));
    }

    private Clock fixedClock() {
        return Clock.fixed(FIXED_TIME, ZoneOffset.UTC);
    }
}
