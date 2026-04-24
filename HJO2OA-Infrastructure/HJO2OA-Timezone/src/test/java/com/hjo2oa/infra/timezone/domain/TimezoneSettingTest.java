package com.hjo2oa.infra.timezone.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TimezoneSettingTest {

    private static final Instant CREATED_AT = Instant.parse("2026-04-20T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-04-20T01:00:00Z");

    @Test
    void shouldCreateUpdateDeactivateAndConvertToView() {
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        TimezoneSetting created = TimezoneSetting.create(
                TimezoneScopeType.TENANT,
                tenantId,
                "Asia/Shanghai",
                true,
                CREATED_AT,
                tenantId,
                CREATED_AT
        );

        TimezoneSetting updated = created.update(
                "Europe/Berlin",
                true,
                UPDATED_AT,
                true,
                tenantId,
                UPDATED_AT
        );
        TimezoneSetting deactivated = updated.deactivate(UPDATED_AT.plusSeconds(60));
        TimezoneSettingView view = deactivated.toView();

        assertThat(created.scopeType()).isEqualTo(TimezoneScopeType.TENANT);
        assertThat(updated.timezoneId()).isEqualTo("Europe/Berlin");
        assertThat(updated.updatedAt()).isEqualTo(UPDATED_AT);
        assertThat(deactivated.active()).isFalse();
        assertThat(view.scopeId()).isEqualTo(tenantId);
        assertThat(view.timezoneId()).isEqualTo("Europe/Berlin");
    }

    @Test
    void shouldRejectInvalidScopeDefinition() {
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        assertThatThrownBy(() -> new TimezoneSetting(
                UUID.randomUUID(),
                TimezoneScopeType.SYSTEM,
                tenantId,
                "UTC",
                true,
                CREATED_AT,
                true,
                null,
                CREATED_AT,
                CREATED_AT
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scopeId must be null");

        assertThatThrownBy(() -> new TimezoneSetting(
                UUID.randomUUID(),
                TimezoneScopeType.TENANT,
                tenantId,
                "UTC",
                true,
                CREATED_AT,
                true,
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                CREATED_AT,
                CREATED_AT
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenant scopeId must equal tenantId");
    }

    @Test
    void shouldRejectInvalidTimezoneId() {
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        assertThatThrownBy(() -> TimezoneSetting.create(
                TimezoneScopeType.TENANT,
                tenantId,
                "Not/A-Real-Timezone",
                true,
                CREATED_AT,
                tenantId,
                CREATED_AT
        ))
                .isInstanceOf(Exception.class);
    }
}
