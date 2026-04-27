package com.hjo2oa.msg.ecosystem.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EcosystemIntegrationTest {

    @Test
    void createRequiresConfigRefOnly() {
        Instant now = Instant.parse("2026-04-27T00:00:00Z");

        assertThatThrownBy(() -> EcosystemIntegration.create(
                UUID.randomUUID(),
                IntegrationType.EMAIL,
                "Email",
                AuthMode.SIGNATURE,
                null,
                "HMAC_SHA256",
                " ",
                UUID.randomUUID(),
                now
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void healthDoesNotRewriteBusinessStatus() {
        Instant now = Instant.parse("2026-04-27T00:00:00Z");
        EcosystemIntegration integration = EcosystemIntegration.create(
                UUID.randomUUID(),
                IntegrationType.EMAIL,
                "Email",
                AuthMode.SIGNATURE,
                null,
                "HMAC_SHA256",
                "config:email",
                UUID.randomUUID(),
                now
        ).changeStatus(IntegrationStatus.ENABLED, now);

        EcosystemIntegration degraded = integration.updateHealth(HealthStatus.DEGRADED, "timeout", now.plusSeconds(1));

        assertThat(degraded.status()).isEqualTo(IntegrationStatus.ENABLED);
        assertThat(degraded.available()).isFalse();
        assertThat(degraded.updateHealth(HealthStatus.HEALTHY, null, now.plusSeconds(2)).available()).isTrue();
    }
}
