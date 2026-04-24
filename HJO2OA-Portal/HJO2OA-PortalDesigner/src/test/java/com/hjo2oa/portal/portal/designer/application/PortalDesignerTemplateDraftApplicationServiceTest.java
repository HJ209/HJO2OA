package com.hjo2oa.portal.portal.designer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateInitializationView;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.model.application.CreatePortalTemplateCommand;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
import com.hjo2oa.portal.portal.model.application.SavePortalTemplateCanvasCommand;
import com.hjo2oa.portal.portal.model.domain.PortalLayoutRegion;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPage;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateLayoutMode;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetPlacement;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalTemplateRepository;
import com.hjo2oa.portal.widget.config.application.UpsertWidgetDefinitionCommand;
import com.hjo2oa.portal.widget.config.application.WidgetDefinitionApplicationService;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContext;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContextProvider;
import com.hjo2oa.portal.widget.config.domain.WidgetDataSourceType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import com.hjo2oa.portal.widget.config.infrastructure.InMemoryWidgetDefinitionRepository;
import com.hjo2oa.shared.kernel.BizException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortalDesignerTemplateDraftApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T17:00:00Z");

    @Test
    void shouldSaveDraftAndReturnRefreshedInitializationPayload() {
        TestFixture fixture = fixture();
        seedTemplateProjection(fixture.projectionRepository());
        seedTemplateCanvas(fixture.templateApplicationService());
        seedWidgets(fixture.widgetDefinitionApplicationService());

        PortalDesignerTemplateInitializationView initialization = fixture.applicationService().save(
                new SavePortalTemplateCanvasCommand(
                        "template-1",
                        List.of(new PortalPage(
                                "page-home-custom",
                                "home-custom",
                                "Home Custom",
                                true,
                                PortalTemplateLayoutMode.THREE_SECTION,
                                List.of(new PortalLayoutRegion(
                                        "region-work-focus",
                                        "work-focus",
                                        "Work Focus",
                                        true,
                                        List.of(
                                                placement("placement-todo", "todo-card", WidgetCardType.TODO, 20),
                                                placement("placement-message", "message-card", WidgetCardType.MESSAGE, 10)
                                        )
                                ))
                        ))
                )
        );

        assertThat(initialization.templateId()).isEqualTo("template-1");
        assertThat(initialization.canvas().pages()).singleElement().satisfies(page -> {
            assertThat(page.pageCode()).isEqualTo("home-custom");
            assertThat(page.regions().get(0).placements()).extracting(placement -> placement.widgetCode())
                    .containsExactly("message-card", "todo-card");
        });
        assertThat(initialization.widgetPalette().widgets())
                .extracting(widget -> widget.widgetCode())
                .containsExactly("home-identity", "home-message");
    }

    @Test
    void shouldRejectInvalidDraftCanvas() {
        TestFixture fixture = fixture();
        seedTemplateProjection(fixture.projectionRepository());
        seedTemplateCanvas(fixture.templateApplicationService());
        seedWidgets(fixture.widgetDefinitionApplicationService());

        assertThatThrownBy(() -> fixture.applicationService().save(new SavePortalTemplateCanvasCommand(
                "template-1",
                List.of(
                        new PortalPage(
                                "page-1",
                                "home-1",
                                "Home One",
                                true,
                                PortalTemplateLayoutMode.THREE_SECTION,
                                List.of(new PortalLayoutRegion(
                                        "region-1",
                                        "work-focus",
                                        "Work Focus",
                                        true,
                                        List.of(placement("placement-1", "todo-card", WidgetCardType.TODO, 10))
                                ))
                        ),
                        new PortalPage(
                                "page-2",
                                "home-2",
                                "Home Two",
                                false,
                                PortalTemplateLayoutMode.THREE_SECTION,
                                List.of(new PortalLayoutRegion(
                                        "region-2",
                                        "work-focus",
                                        "Work Focus Again",
                                        true,
                                        List.of(placement("placement-2", "message-card", WidgetCardType.MESSAGE, 20))
                                ))
                        )
                )
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Duplicate region code");
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

        PortalTemplateCanvasApplicationService templateCanvasApplicationService =
                new PortalTemplateCanvasApplicationService(
                        templateRepository,
                        Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
                );

        return new TestFixture(
                projectionRepository,
                templateApplicationService,
                widgetDefinitionApplicationService,
                new PortalDesignerTemplateDraftApplicationService(
                        templateCanvasApplicationService,
                        new PortalDesignerTemplateInitializationApplicationService(
                                new PortalDesignerTemplateStatusApplicationService(projectionRepository),
                                new PortalDesignerTemplateWidgetPaletteApplicationService(
                                        projectionRepository,
                                        widgetDefinitionApplicationService
                                ),
                                templateCanvasApplicationService
                        )
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
        )));
    }

    private void seedTemplateCanvas(PortalTemplateApplicationService templateApplicationService) {
        templateApplicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
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

    private PortalWidgetPlacement placement(
            String placementCode,
            String widgetCode,
            WidgetCardType cardType,
            int orderNo
    ) {
        return new PortalWidgetPlacement(
                placementCode,
                placementCode,
                widgetCode,
                cardType,
                orderNo,
                false,
                false,
                Map.of()
        );
    }

    private record TestFixture(
            InMemoryPortalDesignerTemplateProjectionRepository projectionRepository,
            PortalTemplateApplicationService templateApplicationService,
            WidgetDefinitionApplicationService widgetDefinitionApplicationService,
            PortalDesignerTemplateDraftApplicationService applicationService
    ) {
    }
}
