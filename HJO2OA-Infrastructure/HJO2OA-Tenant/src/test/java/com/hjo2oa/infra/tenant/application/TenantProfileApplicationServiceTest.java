package com.hjo2oa.infra.tenant.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.infra.tenant.domain.IsolationMode;
import com.hjo2oa.infra.tenant.domain.QuotaType;
import com.hjo2oa.infra.tenant.domain.TenantCreatedEvent;
import com.hjo2oa.infra.tenant.domain.TenantInitializedEvent;
import com.hjo2oa.infra.tenant.domain.TenantProfileView;
import com.hjo2oa.infra.tenant.domain.TenantQuota;
import com.hjo2oa.infra.tenant.domain.TenantQuotaView;
import com.hjo2oa.infra.tenant.domain.TenantQuotaWarningEvent;
import com.hjo2oa.infra.tenant.domain.TenantStatus;
import com.hjo2oa.infra.tenant.infrastructure.InMemoryTenantProfileRepository;
import com.hjo2oa.infra.tenant.infrastructure.InMemoryTenantQuotaRepository;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TenantProfileApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T09:00:00Z");

    @Test
    void shouldCreateTenantAndPublishCreatedEvent() {
        InMemoryTenantProfileRepository profileRepository = new InMemoryTenantProfileRepository();
        InMemoryTenantQuotaRepository quotaRepository = new InMemoryTenantQuotaRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        TenantProfileApplicationService applicationService = applicationService(
                profileRepository,
                quotaRepository,
                publishedEvents
        );

        TenantProfileView createdTenant = applicationService.createTenant(
                "tenant-one",
                "Tenant One",
                IsolationMode.SHARED_DB,
                "basic"
        );

        assertThat(createdTenant.id()).isNotNull();
        assertThat(createdTenant.tenantCode()).isEqualTo("tenant-one");
        assertThat(createdTenant.status()).isEqualTo(TenantStatus.DRAFT);
        assertThat(publishedEvents).singleElement().isInstanceOf(TenantCreatedEvent.class);
        assertThat(((TenantCreatedEvent) publishedEvents.get(0)).tenantCode()).isEqualTo("tenant-one");
    }

    @Test
    void shouldInitializeTenantOnlyOnceAndPublishInitializedEventOnce() {
        InMemoryTenantProfileRepository profileRepository = new InMemoryTenantProfileRepository();
        InMemoryTenantQuotaRepository quotaRepository = new InMemoryTenantQuotaRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        TenantProfileApplicationService applicationService = applicationService(
                profileRepository,
                quotaRepository,
                publishedEvents
        );
        TenantProfileView createdTenant = applicationService.createTenant(
                "tenant-two",
                "Tenant Two",
                IsolationMode.SHARED_DB,
                null
        );
        publishedEvents.clear();

        TenantProfileView initializedTenant = applicationService.initializeTenant(createdTenant.id());
        TenantProfileView initializedAgain = applicationService.initializeTenant(createdTenant.id());

        assertThat(initializedTenant.initialized()).isTrue();
        assertThat(initializedAgain.initialized()).isTrue();
        assertThat(publishedEvents).singleElement().isInstanceOf(TenantInitializedEvent.class);
    }

    @Test
    void shouldPublishQuotaWarningEventWhenQuotaReachesThreshold() {
        InMemoryTenantProfileRepository profileRepository = new InMemoryTenantProfileRepository();
        InMemoryTenantQuotaRepository quotaRepository = new InMemoryTenantQuotaRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        TenantProfileApplicationService applicationService = applicationService(
                profileRepository,
                quotaRepository,
                publishedEvents
        );
        TenantProfileView createdTenant = applicationService.createTenant(
                "tenant-three",
                "Tenant Three",
                IsolationMode.DEDICATED_DB,
                "enterprise"
        );
        applicationService.updateQuota(createdTenant.id(), QuotaType.USER_COUNT, 100, 60L);
        TenantQuota quota = quotaRepository.findByTenantProfileIdAndQuotaType(createdTenant.id(), QuotaType.USER_COUNT)
                .orElseThrow()
                .incrementUsage(65);
        quotaRepository.save(quota);
        publishedEvents.clear();

        TenantQuotaView quotaView = applicationService.checkQuota(createdTenant.id(), QuotaType.USER_COUNT);

        assertThat(quotaView.warning()).isTrue();
        assertThat(quotaView.usedValue()).isEqualTo(65);
        assertThat(publishedEvents).singleElement().isInstanceOf(TenantQuotaWarningEvent.class);
        assertThat(((TenantQuotaWarningEvent) publishedEvents.get(0)).quotaType()).isEqualTo(QuotaType.USER_COUNT);
    }

    @Test
    void shouldListOnlyActiveTenants() {
        TenantProfileApplicationService applicationService = applicationService(
                new InMemoryTenantProfileRepository(),
                new InMemoryTenantQuotaRepository(),
                new ArrayList<>()
        );
        TenantProfileView first = applicationService.createTenant(
                "tenant-four",
                "Tenant Four",
                IsolationMode.SHARED_DB,
                null
        );
        applicationService.createTenant(
                "tenant-five",
                "Tenant Five",
                IsolationMode.SHARED_DB,
                null
        );

        applicationService.activateTenant(first.id());

        assertThat(applicationService.listActive())
                .extracting(TenantProfileView::tenantCode)
                .containsExactly("tenant-four");
    }

    private TenantProfileApplicationService applicationService(
            InMemoryTenantProfileRepository profileRepository,
            InMemoryTenantQuotaRepository quotaRepository,
            List<DomainEvent> publishedEvents
    ) {
        return new TenantProfileApplicationService(
                profileRepository,
                quotaRepository,
                publishedEvents::add,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
