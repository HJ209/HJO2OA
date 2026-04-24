package com.hjo2oa.portal.portal.model.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateDeprecatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplatePublishedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateView;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalTemplateRepository;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalWidgetReferenceStatusRepository;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortalTemplateApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T10:30:00Z");

    @Test
    void shouldCreateTemplateAndPublishCreatedEvent() {
        InMemoryPortalTemplateRepository repository = new InMemoryPortalTemplateRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        PortalTemplateApplicationService applicationService = applicationService(repository, publishedEvents);

        PortalTemplateView template = applicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));

        assertThat(template.templateId()).isEqualTo("template-1");
        assertThat(template.templateCode()).isEqualTo("home-default");
        assertThat(template.latestVersionNo()).isEqualTo(1);
        assertThat(template.publishedVersionNo()).isNull();
        assertThat(template.versions()).singleElement().satisfies(version ->
                assertThat(version.status().name()).isEqualTo("DRAFT")
        );
        assertThat(publishedEvents).singleElement().isInstanceOf(PortalTemplateCreatedEvent.class);
    }

    @Test
    void shouldPublishTemplateVersionAndEmitPublishedEvent() {
        InMemoryPortalTemplateRepository repository = new InMemoryPortalTemplateRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        PortalTemplateApplicationService applicationService = applicationService(repository, publishedEvents);
        applicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "office-default",
                "Office Default",
                PortalPublicationSceneType.OFFICE_CENTER
        ));
        publishedEvents.clear();

        PortalTemplateView publishedTemplate = applicationService.publish(new PublishPortalTemplateVersionCommand(
                "template-1",
                1
        ));

        assertThat(publishedTemplate.publishedVersionNo()).isEqualTo(1);
        assertThat(publishedTemplate.versions()).singleElement().satisfies(version ->
                assertThat(version.status().name()).isEqualTo("PUBLISHED")
        );
        assertThat(publishedEvents).singleElement().isInstanceOf(PortalTemplatePublishedEvent.class);
    }

    @Test
    void shouldDeprecatePublishedVersionAndEmitDeprecatedEvent() {
        InMemoryPortalTemplateRepository repository = new InMemoryPortalTemplateRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        PortalTemplateApplicationService applicationService = applicationService(repository, publishedEvents);
        applicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "mobile-default",
                "Mobile Default",
                PortalPublicationSceneType.MOBILE_WORKBENCH
        ));
        applicationService.publish(new PublishPortalTemplateVersionCommand("template-1", 1));
        publishedEvents.clear();

        PortalTemplateView deprecatedTemplate = applicationService.deprecate(new DeprecatePortalTemplateVersionCommand(
                "template-1",
                1
        ));

        assertThat(deprecatedTemplate.publishedVersionNo()).isNull();
        assertThat(deprecatedTemplate.versions()).singleElement().satisfies(version ->
                assertThat(version.status().name()).isEqualTo("DEPRECATED")
        );
        assertThat(publishedEvents).singleElement().isInstanceOf(PortalTemplateDeprecatedEvent.class);
    }

    @Test
    void shouldRejectDuplicateTemplateCode() {
        PortalTemplateApplicationService applicationService = applicationService(
                new InMemoryPortalTemplateRepository(),
                new ArrayList<>()
        );
        applicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "shared-code",
                "Template One",
                PortalPublicationSceneType.HOME
        ));

        assertThatThrownBy(() -> applicationService.create(new CreatePortalTemplateCommand(
                "template-2",
                "shared-code",
                "Template Two",
                PortalPublicationSceneType.HOME
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Portal template code already exists");
    }

    @Test
    void shouldRejectPublishWhenDraftCanvasContainsRepairRequiredWidgetReference() {
        InMemoryPortalTemplateRepository repository = new InMemoryPortalTemplateRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService =
                new PortalWidgetReferenceStatusApplicationService(new InMemoryPortalWidgetReferenceStatusRepository());
        PortalTemplateApplicationService applicationService = applicationService(
                repository,
                "tenant-1",
                publishedEvents,
                widgetReferenceStatusApplicationService
        );
        PortalTemplateCanvasApplicationService canvasApplicationService = new PortalTemplateCanvasApplicationService(
                repository,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                widgetReferenceStatusApplicationService
        );
        applicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        canvasApplicationService.save(new SavePortalTemplateCanvasCommand(
                "template-1",
                List.of(new com.hjo2oa.portal.portal.model.domain.PortalPage(
                        "page-home-custom",
                        "home-custom",
                        "Home Custom",
                        true,
                        com.hjo2oa.portal.portal.model.domain.PortalTemplateLayoutMode.THREE_SECTION,
                        List.of(new com.hjo2oa.portal.portal.model.domain.PortalLayoutRegion(
                                "region-work-focus",
                                "work-focus",
                                "Work Focus",
                                true,
                                List.of(new com.hjo2oa.portal.portal.model.domain.PortalWidgetPlacement(
                                        "placement-message",
                                        "placement-message",
                                        "message-card",
                                        WidgetCardType.MESSAGE,
                                        10,
                                        false,
                                        false,
                                        java.util.Map.of()
                                ))
                        ))
                ))
        ));
        widgetReferenceStatusApplicationService.markDisabled(new PortalWidgetDisabledEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "widget-message",
                "message-card",
                WidgetCardType.MESSAGE,
                WidgetSceneType.HOME
        ));

        assertThatThrownBy(() -> applicationService.publish(new PublishPortalTemplateVersionCommand(
                "template-1",
                1
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("repair-required widget references")
                .hasMessageContaining("widgetCode=message-card")
                .hasMessageContaining("placementCode=placement-message");
    }

    @Test
    void shouldListTemplatesForCurrentTenantWithOptionalSceneFilter() {
        InMemoryPortalTemplateRepository repository = new InMemoryPortalTemplateRepository();
        PortalTemplateApplicationService tenantOneService = applicationService(
                repository,
                "tenant-1",
                new ArrayList<>()
        );
        PortalTemplateApplicationService tenantTwoService = applicationService(
                repository,
                "tenant-2",
                new ArrayList<>()
        );
        tenantOneService.create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        tenantOneService.create(new CreatePortalTemplateCommand(
                "template-2",
                "office-default",
                "Office Default",
                PortalPublicationSceneType.OFFICE_CENTER
        ));
        tenantTwoService.create(new CreatePortalTemplateCommand(
                "template-3",
                "mobile-default",
                "Mobile Default",
                PortalPublicationSceneType.MOBILE_WORKBENCH
        ));

        List<PortalTemplateView> allTemplates = tenantOneService.list(null);
        List<PortalTemplateView> officeTemplates = tenantOneService.list(PortalPublicationSceneType.OFFICE_CENTER);

        assertThat(allTemplates).extracting(PortalTemplateView::templateId)
                .containsExactly("template-1", "template-2");
        assertThat(allTemplates).extracting(PortalTemplateView::tenantId)
                .containsOnly("tenant-1");
        assertThat(officeTemplates).singleElement().satisfies(template ->
                assertThat(template.templateId()).isEqualTo("template-2")
        );
    }

    private PortalTemplateApplicationService applicationService(
            InMemoryPortalTemplateRepository repository,
            List<DomainEvent> publishedEvents
    ) {
        return applicationService(
                repository,
                "tenant-1",
                publishedEvents,
                PortalWidgetReferenceStatusApplicationService.noop()
        );
    }

    private PortalTemplateApplicationService applicationService(
            InMemoryPortalTemplateRepository repository,
            String tenantId,
            List<DomainEvent> publishedEvents
    ) {
        return applicationService(
                repository,
                tenantId,
                publishedEvents,
                PortalWidgetReferenceStatusApplicationService.noop()
        );
    }

    private PortalTemplateApplicationService applicationService(
            InMemoryPortalTemplateRepository repository,
            String tenantId,
            List<DomainEvent> publishedEvents,
            PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService
    ) {
        PortalModelContextProvider contextProvider = () -> new PortalModelContext(tenantId, "portal-admin");
        return new PortalTemplateApplicationService(
                repository,
                contextProvider,
                publishedEvents::add,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                widgetReferenceStatusApplicationService
        );
    }
}
