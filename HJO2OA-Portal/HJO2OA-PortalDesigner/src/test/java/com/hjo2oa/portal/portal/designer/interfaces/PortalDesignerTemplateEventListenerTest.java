package com.hjo2oa.portal.portal.designer.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplateStatusApplicationService;
import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationActivatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationOfflinedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateDeprecatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplatePublishedEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class PortalDesignerTemplateEventListenerTest {

    @Test
    void shouldProjectTemplateTimelineAndPublicationStatusFromEvents() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(InMemoryPortalDesignerTemplateProjectionRepository.class);
            context.registerBean(PortalDesignerTemplateStatusApplicationService.class);
            context.registerBean(PortalDesignerTemplateEventListener.class);
            context.refresh();

            context.publishEvent(new PortalTemplateCreatedEvent(
                    UUID.randomUUID(),
                    Instant.parse("2026-04-20T12:30:00Z"),
                    "tenant-1",
                    "template-1",
                    "office-default",
                    PortalPublicationSceneType.OFFICE_CENTER
            ));
            context.publishEvent(new PortalTemplatePublishedEvent(
                    UUID.randomUUID(),
                    Instant.parse("2026-04-20T12:35:00Z"),
                    "tenant-1",
                    "template-1",
                    1,
                    PortalPublicationSceneType.OFFICE_CENTER
            ));
            context.publishEvent(new PortalPublicationActivatedEvent(
                    UUID.randomUUID(),
                    Instant.parse("2026-04-20T12:40:00Z"),
                    "tenant-1",
                    "publication-1",
                    "template-1",
                    PortalPublicationSceneType.OFFICE_CENTER,
                    PortalPublicationClientType.PC
            ));

            PortalDesignerTemplateStatusApplicationService applicationService =
                    context.getBean(PortalDesignerTemplateStatusApplicationService.class);
            assertThat(applicationService.current("template-1"))
                    .hasValueSatisfying(view -> {
                        assertThat(view.publishedVersionNo()).isEqualTo(1);
                        assertThat(view.activePublicationIds()).containsExactly("publication-1");
                        assertThat(view.hasActivePublication()).isTrue();
                    });
        }
    }

    @Test
    void shouldReflectDeprecatedVersionAndOfflinedPublication() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(InMemoryPortalDesignerTemplateProjectionRepository.class);
            context.registerBean(PortalDesignerTemplateStatusApplicationService.class);
            context.registerBean(PortalDesignerTemplateEventListener.class);
            context.refresh();

            context.publishEvent(new PortalTemplateCreatedEvent(
                    UUID.randomUUID(),
                    Instant.parse("2026-04-20T12:45:00Z"),
                    "tenant-1",
                    "template-1",
                    "mobile-default",
                    PortalPublicationSceneType.MOBILE_WORKBENCH
            ));
            context.publishEvent(new PortalTemplatePublishedEvent(
                    UUID.randomUUID(),
                    Instant.parse("2026-04-20T12:50:00Z"),
                    "tenant-1",
                    "template-1",
                    1,
                    PortalPublicationSceneType.MOBILE_WORKBENCH
            ));
            context.publishEvent(new PortalPublicationActivatedEvent(
                    UUID.randomUUID(),
                    Instant.parse("2026-04-20T12:55:00Z"),
                    "tenant-1",
                    "publication-1",
                    "template-1",
                    PortalPublicationSceneType.MOBILE_WORKBENCH,
                    PortalPublicationClientType.MOBILE
            ));
            context.publishEvent(new PortalTemplateDeprecatedEvent(
                    UUID.randomUUID(),
                    Instant.parse("2026-04-20T13:00:00Z"),
                    "tenant-1",
                    "template-1",
                    1
            ));
            context.publishEvent(new PortalPublicationOfflinedEvent(
                    UUID.randomUUID(),
                    Instant.parse("2026-04-20T13:05:00Z"),
                    "tenant-1",
                    "publication-1",
                    "template-1",
                    PortalPublicationSceneType.MOBILE_WORKBENCH
            ));

            PortalDesignerTemplateStatusApplicationService applicationService =
                    context.getBean(PortalDesignerTemplateStatusApplicationService.class);
            assertThat(applicationService.current("template-1"))
                    .hasValueSatisfying(view -> {
                        assertThat(view.publishedVersionNo()).isNull();
                        assertThat(view.versions()).singleElement().satisfies(version ->
                                assertThat(version.status().name()).isEqualTo("DEPRECATED")
                        );
                        assertThat(view.activePublicationIds()).isEmpty();
                        assertThat(view.hasActivePublication()).isFalse();
                    });
        }
    }
}
