package com.hjo2oa.infra.security.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.infra.security.domain.KeyStatus;
import com.hjo2oa.infra.security.domain.RateLimitSubjectType;
import com.hjo2oa.infra.security.domain.SecurityAnomalyDetectedEvent;
import com.hjo2oa.infra.security.domain.SecurityPolicyType;
import com.hjo2oa.infra.security.domain.SecurityPolicyUpdatedEvent;
import com.hjo2oa.infra.security.domain.SecurityPolicyView;
import com.hjo2oa.infra.security.infrastructure.InMemorySecurityPolicyRepository;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SecurityPolicyApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T06:00:00Z");

    @Test
    void shouldCreatePolicyAndPublishPolicyUpdatedEventWhenConfigChanges() {
        InMemorySecurityPolicyRepository repository = new InMemorySecurityPolicyRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        SecurityPolicyApplicationService applicationService = applicationService(repository, publishedEvents);

        SecurityPolicyView createdPolicy = applicationService.createPolicy(
                "mask-policy",
                SecurityPolicyType.MASKING,
                "Sensitive data masking",
                "{\"mode\":\"strict\"}",
                null
        );
        publishedEvents.clear();

        SecurityPolicyView updatedPolicy = applicationService.updateConfig(
                createdPolicy.id(),
                "{\"mode\":\"preview\"}"
        );

        assertThat(createdPolicy.status().name()).isEqualTo("ACTIVE");
        assertThat(updatedPolicy.configSnapshot()).isEqualTo("{\"mode\":\"preview\"}");
        assertThat(publishedEvents).singleElement().isInstanceOf(SecurityPolicyUpdatedEvent.class);
        SecurityPolicyUpdatedEvent event = (SecurityPolicyUpdatedEvent) publishedEvents.get(0);
        assertThat(event.eventType()).isEqualTo(SecurityPolicyUpdatedEvent.EVENT_TYPE);
        assertThat(event.policyCode()).isEqualTo("mask-policy");
        assertThat(event.policyType()).isEqualTo(SecurityPolicyType.MASKING);
    }

    @Test
    void shouldRotateKeysAndAdvancePreviousRotationState() {
        InMemorySecurityPolicyRepository repository = new InMemorySecurityPolicyRepository();
        SecurityPolicyApplicationService applicationService = applicationService(repository, new ArrayList<>());

        SecurityPolicyView createdPolicy = applicationService.createPolicy(
                "kms-policy",
                SecurityPolicyType.KEY_MANAGEMENT,
                "KMS policy",
                "{\"provider\":\"kms\"}",
                null
        );
        SecurityPolicyView withFirstKey = applicationService.addSecretKey(createdPolicy.id(), "kms:key:v1", "AES256");
        SecurityPolicyView withSecondKey = applicationService.addSecretKey(createdPolicy.id(), "kms:key:v2", "AES256");

        SecurityPolicyView rotatedPolicy = applicationService.rotateKey(
                createdPolicy.id(),
                withSecondKey.secretKeys().stream()
                        .filter(secretKey -> "kms:key:v2".equals(secretKey.keyRef()))
                        .findFirst()
                        .orElseThrow()
                        .id()
        );

        assertThat(withFirstKey.secretKeys()).singleElement().extracting(secretKey -> secretKey.keyStatus().name())
                .isEqualTo("ACTIVE");
        assertThat(rotatedPolicy.secretKeys()).extracting(secretKey -> secretKey.keyStatus().name())
                .containsExactly("ROTATING", "ACTIVE");
        assertThat(rotatedPolicy.secretKeys().get(0).keyStatus()).isEqualTo(KeyStatus.ROTATING);
    }

    @Test
    void shouldApplyMaskingRuleToValue() {
        InMemorySecurityPolicyRepository repository = new InMemorySecurityPolicyRepository();
        SecurityPolicyApplicationService applicationService = applicationService(repository, new ArrayList<>());

        SecurityPolicyView createdPolicy = applicationService.createPolicy(
                "phone-mask",
                SecurityPolicyType.MASKING,
                "Phone masking policy",
                "{\"scene\":\"display\"}",
                null
        );
        applicationService.addMaskingRule(createdPolicy.id(), "PHONE", "KEEP_SUFFIX(4)");

        String maskedValue = applicationService.maskValue("phone-mask", "PHONE", "13812345678");

        assertThat(maskedValue).isEqualTo("*******5678");
    }

    @Test
    void shouldPublishSecurityAnomalyDetectedEvent() {
        InMemorySecurityPolicyRepository repository = new InMemorySecurityPolicyRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        SecurityPolicyApplicationService applicationService = applicationService(repository, publishedEvents);

        applicationService.createPolicy(
                "access-guard",
                SecurityPolicyType.ACCESS_CONTROL,
                "Access control guard",
                "{\"riskLevel\":\"high\"}",
                null
        );

        applicationService.detectAnomaly("access-guard", RateLimitSubjectType.USER);

        assertThat(publishedEvents).singleElement().isInstanceOf(SecurityAnomalyDetectedEvent.class);
        SecurityAnomalyDetectedEvent event = (SecurityAnomalyDetectedEvent) publishedEvents.get(0);
        assertThat(event.eventType()).isEqualTo(SecurityAnomalyDetectedEvent.EVENT_TYPE);
        assertThat(event.policyCode()).isEqualTo("access-guard");
        assertThat(event.subjectType()).isEqualTo(RateLimitSubjectType.USER);
    }

    private SecurityPolicyApplicationService applicationService(
            InMemorySecurityPolicyRepository repository,
            List<DomainEvent> publishedEvents
    ) {
        return new SecurityPolicyApplicationService(
                repository,
                publishedEvents::add,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
