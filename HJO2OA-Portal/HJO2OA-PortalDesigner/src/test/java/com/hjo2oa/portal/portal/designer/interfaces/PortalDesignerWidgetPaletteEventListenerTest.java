package com.hjo2oa.portal.portal.designer.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerWidgetPaletteApplicationService;
import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerWidgetPaletteProjectionRepository;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class PortalDesignerWidgetPaletteEventListenerTest {

    @Test
    void shouldProjectWidgetPaletteFromWidgetConfigEvents() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(InMemoryPortalDesignerWidgetPaletteProjectionRepository.class);
            context.registerBean(PortalDesignerWidgetPaletteApplicationService.class);
            context.registerBean(PortalDesignerWidgetPaletteEventListener.class);
            context.refresh();

            context.publishEvent(new PortalWidgetUpdatedEvent(
                    UUID.randomUUID(),
                    Instant.parse("2026-04-20T14:20:00Z"),
                    "tenant-1",
                    "widget-1",
                    "todo-card",
                    WidgetCardType.TODO,
                    WidgetSceneType.OFFICE_CENTER,
                    List.of("widgetCode")
            ));
            context.publishEvent(new PortalWidgetDisabledEvent(
                    UUID.randomUUID(),
                    Instant.parse("2026-04-20T14:25:00Z"),
                    "tenant-1",
                    "widget-2",
                    "message-card",
                    WidgetCardType.MESSAGE,
                    WidgetSceneType.HOME
            ));

            PortalDesignerWidgetPaletteApplicationService applicationService =
                    context.getBean(PortalDesignerWidgetPaletteApplicationService.class);
            assertThat(applicationService.currentPalette().activeWidgets())
                    .singleElement()
                    .satisfies(widget -> assertThat(widget.widgetId()).isEqualTo("widget-1"));
            assertThat(applicationService.currentPalette().disabledWidgets())
                    .singleElement()
                    .satisfies(widget -> assertThat(widget.widgetId()).isEqualTo("widget-2"));
        }
    }
}
