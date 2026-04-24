package com.hjo2oa.portal.portal.designer.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerWidgetPaletteProjectionRepository;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortalDesignerWidgetPaletteApplicationServiceTest {

    @Test
    void shouldListActiveAndDisabledWidgetPaletteEntries() {
        PortalDesignerWidgetPaletteApplicationService applicationService = applicationService();
        applicationService.markUpdated(new PortalWidgetUpdatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-20T14:00:00Z"),
                "tenant-1",
                "widget-1",
                "todo-card",
                WidgetCardType.TODO,
                WidgetSceneType.OFFICE_CENTER,
                List.of("widgetCode", "cardType")
        ));
        applicationService.markDisabled(new PortalWidgetDisabledEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-20T14:05:00Z"),
                "tenant-1",
                "widget-2",
                "message-card",
                WidgetCardType.MESSAGE,
                WidgetSceneType.HOME
        ));

        var palette = applicationService.currentPalette();

        assertThat(palette.totalWidgets()).isEqualTo(2);
        assertThat(palette.activeWidgets()).singleElement().satisfies(widget -> {
            assertThat(widget.widgetId()).isEqualTo("widget-1");
            assertThat(widget.state().name()).isEqualTo("ACTIVE");
        });
        assertThat(palette.disabledWidgets()).singleElement().satisfies(widget -> {
            assertThat(widget.widgetId()).isEqualTo("widget-2");
            assertThat(widget.state().name()).isEqualTo("DISABLED");
        });
    }

    @Test
    void shouldReplaceExistingWidgetProjectionWhenRepeatedEventsArrive() {
        PortalDesignerWidgetPaletteApplicationService applicationService = applicationService();
        applicationService.markUpdated(new PortalWidgetUpdatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-20T14:10:00Z"),
                "tenant-1",
                "widget-1",
                "todo-card",
                WidgetCardType.TODO,
                WidgetSceneType.HOME,
                List.of("widgetCode")
        ));

        applicationService.markUpdated(new PortalWidgetUpdatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-20T14:12:00Z"),
                "tenant-1",
                "widget-1",
                "identity-card",
                WidgetCardType.IDENTITY,
                WidgetSceneType.OFFICE_CENTER,
                List.of("cardType", "sceneType")
        ));

        var palette = applicationService.currentPalette();

        assertThat(palette.totalWidgets()).isEqualTo(1);
        assertThat(palette.activeWidgets()).singleElement().satisfies(widget -> {
            assertThat(widget.widgetCode()).isEqualTo("identity-card");
            assertThat(widget.cardType()).isEqualTo(WidgetCardType.IDENTITY);
            assertThat(widget.sceneType()).isEqualTo(WidgetSceneType.OFFICE_CENTER);
            assertThat(widget.changedFields()).containsExactly("cardType", "sceneType");
        });
    }

    @Test
    void shouldRetainTenantWideWidgetEntriesWhenSceneTypeIsNull() {
        PortalDesignerWidgetPaletteApplicationService applicationService = applicationService();
        applicationService.markUpdated(new PortalWidgetUpdatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-20T14:15:00Z"),
                "tenant-1",
                "widget-1",
                "identity-card",
                WidgetCardType.IDENTITY,
                null,
                List.of("sceneType")
        ));
        applicationService.markDisabled(new PortalWidgetDisabledEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-20T14:16:00Z"),
                "tenant-1",
                "widget-2",
                "message-card",
                WidgetCardType.MESSAGE,
                null
        ));

        var palette = applicationService.currentPalette();

        assertThat(palette.totalWidgets()).isEqualTo(2);
        assertThat(palette.activeWidgets()).singleElement().satisfies(widget -> {
            assertThat(widget.widgetId()).isEqualTo("widget-1");
            assertThat(widget.sceneType()).isNull();
        });
        assertThat(palette.disabledWidgets()).singleElement().satisfies(widget -> {
            assertThat(widget.widgetId()).isEqualTo("widget-2");
            assertThat(widget.sceneType()).isNull();
        });
    }

    private PortalDesignerWidgetPaletteApplicationService applicationService() {
        return new PortalDesignerWidgetPaletteApplicationService(
                new InMemoryPortalDesignerWidgetPaletteProjectionRepository()
        );
    }
}
