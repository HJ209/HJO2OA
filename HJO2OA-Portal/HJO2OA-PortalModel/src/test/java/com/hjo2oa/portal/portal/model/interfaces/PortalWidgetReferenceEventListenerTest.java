package com.hjo2oa.portal.portal.model.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.portal.model.application.PortalWidgetReferenceStatusApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetReferenceState;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalWidgetReferenceStatusRepository;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class PortalWidgetReferenceEventListenerTest {

    @Test
    void shouldDispatchWidgetUpdatedEventToReferenceProjection() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(InMemoryPortalWidgetReferenceStatusRepository.class);
            context.registerBean(PortalWidgetReferenceStatusApplicationService.class);
            context.registerBean(PortalWidgetReferenceEventListener.class);
            context.refresh();

            context.publishEvent(new PortalWidgetUpdatedEvent(
                    UUID.randomUUID(),
                    Instant.parse("2026-04-20T08:40:00Z"),
                    "tenant-1",
                    "widget-1",
                    "todo-card",
                    WidgetCardType.TODO,
                    WidgetSceneType.HOME,
                    List.of("displayName")
            ));

            PortalWidgetReferenceStatusApplicationService applicationService =
                    context.getBean(PortalWidgetReferenceStatusApplicationService.class);
            assertThat(applicationService.current("widget-1"))
                    .hasValueSatisfying(status ->
                            assertThat(status.state()).isEqualTo(PortalWidgetReferenceState.STALE)
                    );
        }
    }

    @Test
    void shouldDispatchWidgetDisabledEventToReferenceProjection() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(InMemoryPortalWidgetReferenceStatusRepository.class);
            context.registerBean(PortalWidgetReferenceStatusApplicationService.class);
            context.registerBean(PortalWidgetReferenceEventListener.class);
            context.refresh();

            context.publishEvent(new PortalWidgetDisabledEvent(
                    UUID.randomUUID(),
                    Instant.parse("2026-04-20T08:45:00Z"),
                    "tenant-1",
                    "widget-2",
                    "message-card",
                    WidgetCardType.MESSAGE,
                    null
            ));

            PortalWidgetReferenceStatusApplicationService applicationService =
                    context.getBean(PortalWidgetReferenceStatusApplicationService.class);
            assertThat(applicationService.current("widget-2"))
                    .hasValueSatisfying(status ->
                            assertThat(status.state()).isEqualTo(PortalWidgetReferenceState.REPAIR_REQUIRED)
                    );
        }
    }
}
