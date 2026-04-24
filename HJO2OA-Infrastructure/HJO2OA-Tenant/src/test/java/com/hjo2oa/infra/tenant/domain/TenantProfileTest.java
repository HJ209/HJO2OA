package com.hjo2oa.infra.tenant.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantProfileTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID QUOTA_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant CREATED_AT = Instant.parse("2026-04-24T08:00:00Z");

    @Test
    void shouldCreateDraftTenantProfileAndExposeView() {
        TenantProfile profile = TenantProfile.create(
                TENANT_ID,
                "tenant-alpha",
                "Tenant Alpha",
                IsolationMode.SHARED_DB,
                "basic",
                "zh-CN",
                "Asia/Shanghai",
                CREATED_AT
        );

        TenantProfileView view = profile.toView();

        assertThat(profile.status()).isEqualTo(TenantStatus.DRAFT);
        assertThat(profile.initialized()).isFalse();
        assertThat(view.tenantCode()).isEqualTo("tenant-alpha");
        assertThat(view.defaultTimezone()).isEqualTo("Asia/Shanghai");
        assertThat(view.createdAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void shouldActivateSuspendArchiveAndInitializeTenantProfile() {
        TenantProfile created = TenantProfile.create(
                TENANT_ID,
                "tenant-beta",
                "Tenant Beta",
                IsolationMode.DEDICATED_DB,
                null,
                null,
                null,
                CREATED_AT
        );

        TenantProfile activated = created.activate(CREATED_AT.plusSeconds(60));
        TenantProfile initialized = activated.markInitialized(CREATED_AT.plusSeconds(120));
        TenantProfile suspended = initialized.suspend(CREATED_AT.plusSeconds(180));
        TenantProfile archived = suspended.archive(CREATED_AT.plusSeconds(240));

        assertThat(activated.status()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(initialized.initialized()).isTrue();
        assertThat(suspended.status()).isEqualTo(TenantStatus.SUSPENDED);
        assertThat(archived.status()).isEqualTo(TenantStatus.ARCHIVED);
        assertThat(archived.updatedAt()).isEqualTo(CREATED_AT.plusSeconds(240));
    }

    @Test
    void shouldIncrementResetAndWarnOnQuota() {
        TenantQuota quota = new TenantQuota(
                QUOTA_ID,
                TENANT_ID,
                QuotaType.USER_COUNT,
                100,
                50,
                60L
        );

        TenantQuota increased = quota.incrementUsage(15);
        TenantQuota reset = increased.resetUsage();

        assertThat(quota.isWarning()).isFalse();
        assertThat(increased.usedValue()).isEqualTo(65);
        assertThat(increased.isWarning()).isTrue();
        assertThat(reset.usedValue()).isZero();
        assertThat(reset.isWarning()).isFalse();
    }
}
