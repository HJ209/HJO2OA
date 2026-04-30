package com.hjo2oa.infra.i18n.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.infra.i18n.domain.ResolvedLocaleMessageView;
import com.hjo2oa.infra.i18n.infrastructure.InMemoryLocaleBundleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LocaleBundleApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T06:00:00Z");

    @Test
    void shouldResolveMessageThroughFallbackLocaleChain() {
        LocaleBundleApplicationService service = service();
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        service.addEntry(
                service.createBundle(new LocaleBundleCommands.CreateBundleCommand(
                        "portal.messages",
                        "portal",
                        "en-US",
                        null,
                        null
                )).id(),
                "greeting",
                "Hello"
        );
        service.activateBundle(service.queryByCode("portal.messages").stream()
                .filter(bundle -> bundle.locale().equals("en-us"))
                .findFirst()
                .orElseThrow()
                .id());

        UUID zhBundleId = service.createBundle(new LocaleBundleCommands.CreateBundleCommand(
                "portal.messages",
                "portal",
                "zh-CN",
                "en-US",
                null
        )).id();
        service.activateBundle(zhBundleId);

        UUID tenantBundleId = service.createBundle(new LocaleBundleCommands.CreateBundleCommand(
                "portal.messages",
                "portal",
                "en-US",
                null,
                tenantId
        )).id();
        service.addEntry(tenantBundleId, "greeting", "Hello Tenant");
        service.activateBundle(tenantBundleId);

        ResolvedLocaleMessageView globalResolved = service.resolveMessage(
                "portal.messages",
                "greeting",
                "zh-CN",
                null
        );
        ResolvedLocaleMessageView tenantResolved = service.resolveMessage(
                "portal.messages",
                "greeting",
                "zh-CN",
                tenantId
        );

        assertThat(globalResolved.resourceValue()).isEqualTo("Hello");
        assertThat(globalResolved.resolvedLocale()).isEqualTo("en-us");
        assertThat(globalResolved.usedFallback()).isTrue();

        assertThat(tenantResolved.resourceValue()).isEqualTo("Hello Tenant");
        assertThat(tenantResolved.tenantId()).isEqualTo(tenantId);
        assertThat(tenantResolved.resolvedLocale()).isEqualTo("en-us");
        assertThat(tenantResolved.usedFallback()).isTrue();
    }

    @Test
    void shouldQueryBundlesByModuleAndLocale() {
        LocaleBundleApplicationService service = service();

        service.createBundle(new LocaleBundleCommands.CreateBundleCommand(
                "portal.messages",
                "portal",
                "en-US",
                null,
                null
        ));
        service.createBundle(new LocaleBundleCommands.CreateBundleCommand(
                "portal.labels",
                "portal",
                "en-US",
                null,
                null
        ));
        service.createBundle(new LocaleBundleCommands.CreateBundleCommand(
                "workflow.messages",
                "workflow",
                "en-US",
                null,
                null
        ));

        assertThat(service.queryByModule("portal", "en-US"))
                .extracting(bundle -> bundle.bundleCode())
                .containsExactly("portal.labels", "portal.messages");
    }

    @Test
    void shouldInvalidateResolvedMessageCacheAfterEntryUpdate() {
        LocaleBundleApplicationService service = service();
        UUID bundleId = service.createBundle(new LocaleBundleCommands.CreateBundleCommand(
                "portal.cache",
                "portal",
                "en-US",
                null,
                null
        )).id();
        service.addEntry(bundleId, "title", "Old title");
        service.activateBundle(bundleId);

        assertThat(service.resolveMessage("portal.cache", "title", "en-US", null).resourceValue())
                .isEqualTo("Old title");

        service.updateEntry(bundleId, "title", "New title");

        assertThat(service.resolveMessage("portal.cache", "title", "en-US", null).resourceValue())
                .isEqualTo("New title");
    }

    private LocaleBundleApplicationService service() {
        return new LocaleBundleApplicationService(
                new InMemoryLocaleBundleRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
