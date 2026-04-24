package com.hjo2oa.portal.portal.model.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudienceType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationIdentity;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalPublicationRepository;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalTemplateRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PortalActiveTemplateResolutionApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T12:30:00Z");

    @Test
    void shouldResolveActivePublicationAndTemplateInOneQuery() {
        PortalModelContextProvider contextProvider = contextProvider();
        PortalPublicationApplicationService publicationApplicationService = new PortalPublicationApplicationService(
                new InMemoryPortalPublicationRepository(),
                contextProvider,
                event -> {
                },
                fixedClock()
        );
        PortalTemplateApplicationService templateApplicationService = new PortalTemplateApplicationService(
                new InMemoryPortalTemplateRepository(),
                contextProvider,
                event -> {
                },
                fixedClock()
        );
        PortalActiveTemplateResolutionApplicationService applicationService =
                new PortalActiveTemplateResolutionApplicationService(
                        publicationApplicationService,
                        templateApplicationService
                );

        templateApplicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        templateApplicationService.publish(new PublishPortalTemplateVersionCommand("template-1", 1));
        publicationApplicationService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.ALL,
                PortalPublicationAudience.tenantDefault()
        ));

        var resolution = applicationService.currentActive(
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.ALL
        );

        assertThat(resolution).isPresent();
        assertThat(resolution.get().publicationId()).isEqualTo("publication-1");
        assertThat(resolution.get().templateId()).isEqualTo("template-1");
        assertThat(resolution.get().templateCode()).isEqualTo("home-default");
        assertThat(resolution.get().templateDisplayName()).isEqualTo("Home Default");
        assertThat(resolution.get().audience().type()).isEqualTo(PortalPublicationAudienceType.TENANT_DEFAULT);
        assertThat(resolution.get().publishedVersionNo()).isEqualTo(1);
        assertThat(resolution.get().publicationActivatedAt()).isEqualTo(FIXED_TIME);
        assertThat(resolution.get().templateUpdatedAt()).isEqualTo(FIXED_TIME);
    }

    @Test
    void shouldPreferScopedAudienceBeforeTenantDefaultWhenIdentityIsProvided() {
        PortalModelContextProvider contextProvider = contextProvider();
        PortalPublicationApplicationService publicationApplicationService = new PortalPublicationApplicationService(
                new InMemoryPortalPublicationRepository(),
                contextProvider,
                event -> {
                },
                fixedClock()
        );
        PortalTemplateApplicationService templateApplicationService = new PortalTemplateApplicationService(
                new InMemoryPortalTemplateRepository(),
                contextProvider,
                event -> {
                },
                fixedClock()
        );
        PortalActiveTemplateResolutionApplicationService applicationService =
                new PortalActiveTemplateResolutionApplicationService(
                        publicationApplicationService,
                        templateApplicationService
                );

        templateApplicationService.create(new CreatePortalTemplateCommand(
                "template-default",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        templateApplicationService.publish(new PublishPortalTemplateVersionCommand("template-default", 1));
        templateApplicationService.create(new CreatePortalTemplateCommand(
                "template-person",
                "home-person",
                "Home Person",
                PortalPublicationSceneType.HOME
        ));
        templateApplicationService.publish(new PublishPortalTemplateVersionCommand("template-person", 1));
        publicationApplicationService.activate(new ActivatePortalPublicationCommand(
                "publication-default",
                "template-default",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        publicationApplicationService.activate(new ActivatePortalPublicationCommand(
                "publication-person",
                "template-person",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.ofPerson("person-1")
        ));

        var resolution = applicationService.currentActive(
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                new PortalPublicationIdentity(null, null, "person-1")
        );

        assertThat(resolution).isPresent();
        assertThat(resolution.get().publicationId()).isEqualTo("publication-person");
        assertThat(resolution.get().audience().type()).isEqualTo(PortalPublicationAudienceType.PERSON);
    }

    @Test
    void shouldReturnEmptyWhenActivePublicationIsMissing() {
        PortalActiveTemplateResolutionApplicationService applicationService = applicationService();

        var resolution = applicationService.currentActive(
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC
        );

        assertThat(resolution).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenLinkedTemplateIsMissing() {
        PortalModelContextProvider contextProvider = contextProvider();
        PortalPublicationApplicationService publicationApplicationService = new PortalPublicationApplicationService(
                new InMemoryPortalPublicationRepository(),
                contextProvider,
                event -> {
                },
                fixedClock()
        );
        PortalTemplateApplicationService templateApplicationService = new PortalTemplateApplicationService(
                new InMemoryPortalTemplateRepository(),
                contextProvider,
                event -> {
                },
                fixedClock()
        );
        PortalActiveTemplateResolutionApplicationService applicationService =
                new PortalActiveTemplateResolutionApplicationService(
                        publicationApplicationService,
                        templateApplicationService
                );

        publicationApplicationService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "missing-template",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));

        var resolution = applicationService.currentActive(
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC
        );

        assertThat(resolution).isEmpty();
    }

    private PortalActiveTemplateResolutionApplicationService applicationService() {
        PortalModelContextProvider contextProvider = contextProvider();
        return new PortalActiveTemplateResolutionApplicationService(
                new PortalPublicationApplicationService(
                        new InMemoryPortalPublicationRepository(),
                        contextProvider,
                        event -> {
                        },
                        fixedClock()
                ),
                new PortalTemplateApplicationService(
                        new InMemoryPortalTemplateRepository(),
                        contextProvider,
                        event -> {
                        },
                        fixedClock()
                )
        );
    }

    private PortalModelContextProvider contextProvider() {
        return () -> new PortalModelContext("tenant-1", "portal-admin");
    }

    private Clock fixedClock() {
        return Clock.fixed(FIXED_TIME, ZoneOffset.UTC);
    }
}
