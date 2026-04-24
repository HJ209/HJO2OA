package com.hjo2oa.portal.personalization.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.personalization.infrastructure.MutablePersonalizationBasePublicationResolver;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationActivatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationOfflinedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class PersonalizationPublicationEventListenerTest {

    @Test
    void shouldBindActivatedPublicationForScene() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(MutablePersonalizationBasePublicationResolver.class);
            context.registerBean(PersonalizationPublicationEventListener.class);
            context.refresh();

            context.publishEvent(new PortalPublicationActivatedEvent(
                    UUID.randomUUID(),
                    Instant.parse("2026-04-20T07:30:00Z"),
                    "tenant-1",
                    "publication-office-v2",
                    "template-1",
                    PortalPublicationSceneType.OFFICE_CENTER,
                    PortalPublicationClientType.PC
            ));

            MutablePersonalizationBasePublicationResolver resolver =
                    context.getBean(MutablePersonalizationBasePublicationResolver.class);
            assertThat(resolver.resolveBasePublicationId(PersonalizationSceneType.OFFICE_CENTER))
                    .isEqualTo("publication-office-v2");
        }
    }

    @Test
    void shouldRevertToSeededBindingWhenActivatedPublicationIsOfflined() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(MutablePersonalizationBasePublicationResolver.class);
            context.registerBean(PersonalizationPublicationEventListener.class);
            context.refresh();

            context.publishEvent(new PortalPublicationActivatedEvent(
                    UUID.randomUUID(),
                    Instant.parse("2026-04-20T07:35:00Z"),
                    "tenant-1",
                    "publication-home-v2",
                    "template-2",
                    PortalPublicationSceneType.HOME,
                    PortalPublicationClientType.ALL
            ));
            context.publishEvent(new PortalPublicationOfflinedEvent(
                    UUID.randomUUID(),
                    Instant.parse("2026-04-20T07:40:00Z"),
                    "tenant-1",
                    "publication-home-v2",
                    "template-2",
                    PortalPublicationSceneType.HOME
            ));

            MutablePersonalizationBasePublicationResolver resolver =
                    context.getBean(MutablePersonalizationBasePublicationResolver.class);
            assertThat(resolver.resolveBasePublicationId(PersonalizationSceneType.HOME))
                    .isEqualTo("publication-home-default");
        }
    }
}
