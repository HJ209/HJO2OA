package com.hjo2oa.portal.portal.designer.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateInitializationView;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateWidgetPaletteItemView;
import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.model.application.CreatePortalTemplateCommand;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
import com.hjo2oa.portal.portal.model.application.PublishPortalTemplateVersionCommand;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplatePublishedEvent;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalTemplateRepository;
import com.hjo2oa.portal.widget.config.application.UpsertWidgetDefinitionCommand;
import com.hjo2oa.portal.widget.config.application.WidgetDefinitionApplicationService;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContext;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContextProvider;
import com.hjo2oa.portal.widget.config.domain.WidgetDataSourceType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import com.hjo2oa.portal.widget.config.infrastructure.InMemoryWidgetDefinitionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortalDesignerTemplateInitializationApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T16:00:00Z");

    @Test
    void shouldAssembleTemplateStatusCanvasAndWidgetPalette() {
        TestFixture fixture = fixture();
        seedTemplateProjection(fixture.projectionRepository());
        seedTemplateCanvas(fixture.templateApplicationService());
        seedWidgets(fixture.widgetDefinitionApplicationService());

        PortalDesignerTemplateInitializationView initialization =
                fixture.applicationService().current("template-1").orElseThrow();

        assertThat(initialization.templateId()).isEqualTo("template-1");
        assertThat(initialization.templateCode()).isEqualTo("home-default");
        assertThat(initialization.status().publishedVersionNo()).isEqualTo(1);
        assertThat(initialization.canvas().pages()).singleElement().satisfies(page -> {
            assertThat(page.pageCode()).isEqualTo("home-main");
            assertThat(page.regions()).hasSize(2);
        });
        assertThat(initialization.widgetPalette().widgets())
                .extracting(PortalDesignerTemplateWidgetPaletteItemView::widgetCode)
                .containsExactly("home-identity", "home-message");
    }

    @Test
    void shouldReturnEmptyWhenTemplateProjectionIsMissing() {
        TestFixture fixture = fixture();
        seedTemplateCanvas(fixture.templateApplicationService());
        seedWidgets(fixture.widgetDefinitionApplicationService());

        assertThat(fixture.applicationService().current("template-1")).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenTemplateCanvasIsMissing() {
        TestFixture fixture = fixture();
        seedTemplateProjection(fixture.projectionRepository());
        seedWidgets(fixture.widgetDefinitionApplicationService());

        assertThat(fixture.applicationService().current("template-1")).isEmpty();
    }

    private TestFixture fixture() {
        InMemoryPortalDesignerTemplateProjectionRepository projectionRepository =
                new InMemoryPortalDesignerTemplateProjectionRepository();
        InMemoryPortalTemplateRepository templateRepository = new InMemoryPortalTemplateRepository();

        PortalModelContextProvider portalModelContextProvider =
                () -> new PortalModelContext("tenant-1", "portal-admin");
        PortalTemplateApplicationService templateApplicationService = new PortalTemplateApplicationService(
                templateRepository,
                portalModelContextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        WidgetConfigContextProvider widgetContextProvider =
                () -> new WidgetConfigContext("tenant-1", "portal-admin");
        WidgetDefinitionApplicationService widgetDefinitionApplicationService =
                new WidgetDefinitionApplicationService(
                        new InMemoryWidgetDefinitionRepository(),
                        widgetContextProvider,
                        event -> {
                        },
                        Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
                );

        return new TestFixture(
                projectionRepository,
                templateApplicationService,
                widgetDefinitionApplicationService,
                new PortalDesignerTemplateInitializationApplicationService(
                        new PortalDesignerTemplateStatusApplicationService(projectionRepository),
                        new PortalDesignerTemplateWidgetPaletteApplicationService(
                                projectionRepository,
                                widgetDefinitionApplicationService
                        ),
                        new PortalTemplateCanvasApplicationService(templateRepository)
                )
        );
    }

    private void seedTemplateProjection(InMemoryPortalDesignerTemplateProjectionRepository projectionRepository) {
        projectionRepository.save(PortalDesignerTemplateProjection.initialize(new PortalTemplateCreatedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "template-1",
                "home-default",
                PortalPublicationSceneType.HOME
        )).applyTemplatePublished(new PortalTemplatePublishedEvent(
                UUID.randomUUID(),
                FIXED_TIME.plusSeconds(300),
                "tenant-1",
                "template-1",
                1,
                PortalPublicationSceneType.HOME
        )));
    }

    private void seedTemplateCanvas(PortalTemplateApplicationService templateApplicationService) {
        templateApplicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        templateApplicationService.publish(new PublishPortalTemplateVersionCommand("template-1", 1));
    }

    private void seedWidgets(WidgetDefinitionApplicationService widgetDefinitionApplicationService) {
        widgetDefinitionApplicationService.upsert(new UpsertWidgetDefinitionCommand(
                "widget-1",
                "home-message",
                "Home Message",
                WidgetCardType.MESSAGE,
                WidgetSceneType.HOME,
                "message-center",
                WidgetDataSourceType.AGGREGATION_QUERY,
                false,
                true,
                5
        ));
        widgetDefinitionApplicationService.upsert(new UpsertWidgetDefinitionCommand(
                "widget-2",
                "office-todo",
                "Office Todo",
                WidgetCardType.TODO,
                WidgetSceneType.OFFICE_CENTER,
                "todo-center",
                WidgetDataSourceType.AGGREGATION_QUERY,
                true,
                true,
                8
        ));
        widgetDefinitionApplicationService.upsert(new UpsertWidgetDefinitionCommand(
                "widget-3",
                "home-identity",
                "Home Identity",
                WidgetCardType.IDENTITY,
                WidgetSceneType.HOME,
                "identity-context",
                WidgetDataSourceType.AGGREGATION_QUERY,
                false,
                false,
                1
        ));
    }

    private record TestFixture(
            InMemoryPortalDesignerTemplateProjectionRepository projectionRepository,
            PortalTemplateApplicationService templateApplicationService,
            WidgetDefinitionApplicationService widgetDefinitionApplicationService,
            PortalDesignerTemplateInitializationApplicationService applicationService
    ) {
    }
}
