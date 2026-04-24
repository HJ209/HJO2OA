package com.hjo2oa.infra.config.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.infra.config.domain.ConfigEntryView;
import com.hjo2oa.infra.config.domain.ConfigType;
import com.hjo2oa.infra.config.domain.ConfigUpdatedEvent;
import com.hjo2oa.infra.config.domain.FeatureFlagChangedEvent;
import com.hjo2oa.infra.config.domain.FeatureRuleType;
import com.hjo2oa.infra.config.domain.OverrideScopeType;
import com.hjo2oa.infra.config.domain.ResolvedValueSourceType;
import com.hjo2oa.infra.config.infrastructure.InMemoryConfigEntryRepository;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEvent;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConfigEntryApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T08:30:00Z");

    @Test
    void shouldUpdateDefaultAndPublishConfigUpdatedEvent() {
        RecordingDomainEventPublisher eventPublisher = new RecordingDomainEventPublisher();
        ConfigEntryApplicationService applicationService = applicationService(eventPublisher);

        ConfigEntryView created = applicationService.createEntry(
                "portal.banner.title",
                "Portal Banner Title",
                ConfigType.STRING,
                "Hello",
                true,
                false,
                null
        );

        ConfigEntryView updated = applicationService.updateDefault(created.id(), "Welcome");

        assertThat(updated.defaultValue()).isEqualTo("Welcome");
        assertThat(eventPublisher.events()).hasSize(1);
        assertThat(eventPublisher.events().get(0)).isInstanceOf(ConfigUpdatedEvent.class);
        ConfigUpdatedEvent event = (ConfigUpdatedEvent) eventPublisher.events().get(0);
        assertThat(event.eventType()).isEqualTo(ConfigUpdatedEvent.EVENT_TYPE);
        assertThat(event.configKey()).isEqualTo("portal.banner.title");
        assertThat(event.scopeType()).isNull();
        assertThat(event.scopeId()).isNull();
    }

    @Test
    void shouldResolveMostSpecificOverrideChain() {
        ConfigEntryApplicationService applicationService = applicationService(new RecordingDomainEventPublisher());
        ConfigEntryView created = applicationService.createEntry(
                "todo.page.size",
                "Todo Page Size",
                ConfigType.NUMBER,
                "20",
                true,
                true,
                "{\"min\":1,\"max\":100}"
        );

        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID orgId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID roleId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID userId = UUID.fromString("44444444-4444-4444-4444-444444444444");

        applicationService.addOverride(created.id(), OverrideScopeType.TENANT, tenantId, "30");
        applicationService.addOverride(created.id(), OverrideScopeType.ORGANIZATION, orgId, "40");
        applicationService.addOverride(created.id(), OverrideScopeType.ROLE, roleId, "50");
        applicationService.addOverride(created.id(), OverrideScopeType.USER, userId, "60");

        assertThat(applicationService.resolveValue("todo.page.size", tenantId, orgId, roleId, userId).resolvedValue())
                .isEqualTo("60");
        assertThat(applicationService.resolveValue("todo.page.size", tenantId, orgId, roleId, userId).sourceType())
                .isEqualTo(ResolvedValueSourceType.OVERRIDE);
        assertThat(applicationService.resolveValue("todo.page.size", tenantId, orgId, roleId, null).resolvedValue())
                .isEqualTo("50");
        assertThat(applicationService.resolveValue("todo.page.size", tenantId, orgId, null, null).resolvedValue())
                .isEqualTo("40");
        assertThat(applicationService.resolveValue("todo.page.size", tenantId, null, null, null).resolvedValue())
                .isEqualTo("30");
    }

    @Test
    void shouldAddFeatureRulePublishEventAndResolveFeatureFlag() {
        RecordingDomainEventPublisher eventPublisher = new RecordingDomainEventPublisher();
        ConfigEntryApplicationService applicationService = applicationService(eventPublisher);
        ConfigEntryView created = applicationService.createEntry(
                "portal.beta.enabled",
                "Portal Beta Enabled",
                ConfigType.FEATURE_FLAG,
                "false",
                true,
                true,
                null
        );

        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        ConfigEntryView updated = applicationService.addFeatureRule(
                created.id(),
                FeatureRuleType.TENANT,
                "{\"tenantId\":\"11111111-1111-1111-1111-111111111111\",\"enabled\":true}",
                0
        );

        assertThat(updated.featureRules()).hasSize(1);
        assertThat(eventPublisher.events()).hasSize(1);
        assertThat(eventPublisher.events().get(0)).isInstanceOf(FeatureFlagChangedEvent.class);
        FeatureFlagChangedEvent event = (FeatureFlagChangedEvent) eventPublisher.events().get(0);
        assertThat(event.eventType()).isEqualTo(FeatureFlagChangedEvent.EVENT_TYPE);
        assertThat(event.configKey()).isEqualTo("portal.beta.enabled");
        assertThat(event.ruleType()).isEqualTo(FeatureRuleType.TENANT);

        assertThat(applicationService.resolveValue("portal.beta.enabled", tenantId, null, null, null).resolvedValue())
                .isEqualTo("true");
        assertThat(applicationService.resolveValue("portal.beta.enabled", tenantId, null, null, null).sourceType())
                .isEqualTo(ResolvedValueSourceType.FEATURE_RULE);
    }

    @Test
    void shouldRejectRuntimeDefaultMutationForNonMutableActiveConfig() {
        ConfigEntryApplicationService applicationService = applicationService(new RecordingDomainEventPublisher());
        ConfigEntryView created = applicationService.createEntry(
                "system.license.mode",
                "System License Mode",
                ConfigType.STRING,
                "community",
                false,
                false,
                null
        );

        assertThatThrownBy(() -> applicationService.updateDefault(created.id(), "enterprise"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Runtime mutation is forbidden");
    }

    private ConfigEntryApplicationService applicationService(RecordingDomainEventPublisher eventPublisher) {
        return new ConfigEntryApplicationService(
                new InMemoryConfigEntryRepository(),
                eventPublisher,
                new ObjectMapper().findAndRegisterModules(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    private static final class RecordingDomainEventPublisher implements DomainEventPublisher {

        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            events.add(event);
        }

        private List<DomainEvent> events() {
            return List.copyOf(events);
        }
    }
}
