package com.hjo2oa.msg.ecosystem.domain;

import java.util.UUID;

public record IntegrationAvailabilityView(
        UUID id,
        IntegrationType integrationType,
        String configRef,
        HealthStatus healthStatus,
        boolean available
) {
}
