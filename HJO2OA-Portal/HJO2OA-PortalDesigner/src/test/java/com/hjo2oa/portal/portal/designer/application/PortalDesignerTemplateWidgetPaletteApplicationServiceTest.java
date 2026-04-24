package com.hjo2oa.portal.portal.designer.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateWidgetPaletteItemView;
import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import com.hjo2oa.portal.widget.config.application.UpsertWidgetDefinitionCommand;
import com.hjo2oa.portal.widget.config.application.WidgetDefinitionApplicationService;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContext;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContextProvider;
import com.hjo2oa.portal.widget.config.domain.WidgetDataSourceType;
import com.hjo2oa.portal.widget.config.domain.WidgetDefinitionStatus;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import com.hjo2oa.portal.widget.config.infrastructure.InMemoryWidgetDefinitionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortalDesignerTemplateWidgetPaletteApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T15:00:00Z");

    @Test
    void shouldReturnTemplateScopedWidgetPaletteFromWidgetConfigSource() {
        InMemoryPortalDesignerTemplateProjectionRepository projectionRepository =
                new InMemoryPortalDesignerTemplateProjectionRepository();
        projectionRepository.save(PortalDesignerTemplateProjection.initialize(new PortalTemplateCreatedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "template-1",
                "home-default",
                PortalPublicationSceneType.HOME
        )));
        WidgetDefinitionApplicationService widgetDefinitionApplicationService = widgetDefinitionApplicationService();
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

        PortalDesignerTemplateWidgetPaletteApplicationService applicationService =
                new PortalDesignerTemplateWidgetPaletteApplicationService(
                        projectionRepository,
                        widgetDefinitionApplicationService
                );

        var palette = applicationService.current("template-1");

        assertThat(palette).isPresent();
        assertThat(palette.get().templateCode()).isEqualTo("home-default");
        assertThat(palette.get().widgets())
                .extracting(PortalDesignerTemplateWidgetPaletteItemView::widgetCode)
                .containsExactly("home-identity", "home-message");
        assertThat(palette.get().widgets())
                .extracting(PortalDesignerTemplateWidgetPaletteItemView::status)
                .containsOnly(WidgetDefinitionStatus.ACTIVE);
    }

    @Test
    void shouldReturnEmptyWhenTemplateProjectionIsMissing() {
        PortalDesignerTemplateWidgetPaletteApplicationService applicationService =
                new PortalDesignerTemplateWidgetPaletteApplicationService(
                        new InMemoryPortalDesignerTemplateProjectionRepository(),
                        widgetDefinitionApplicationService()
                );

        assertThat(applicationService.current("template-missing")).isEmpty();
    }

    private WidgetDefinitionApplicationService widgetDefinitionApplicationService() {
        WidgetConfigContextProvider contextProvider = () -> new WidgetConfigContext("tenant-1", "portal-admin");
        return new WidgetDefinitionApplicationService(
                new InMemoryWidgetDefinitionRepository(),
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
