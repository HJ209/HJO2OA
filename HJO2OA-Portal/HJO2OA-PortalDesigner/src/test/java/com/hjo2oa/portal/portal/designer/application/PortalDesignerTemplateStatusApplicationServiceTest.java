package com.hjo2oa.portal.portal.designer.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplatePublishedEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortalDesignerTemplateStatusApplicationServiceTest {

    @Test
    void shouldReturnCurrentDesignerTemplateStatus() {
        InMemoryPortalDesignerTemplateProjectionRepository repository =
                new InMemoryPortalDesignerTemplateProjectionRepository();
        PortalDesignerTemplateStatusApplicationService applicationService =
                new PortalDesignerTemplateStatusApplicationService(repository);
        PortalDesignerTemplateProjection projection = PortalDesignerTemplateProjection.initialize(
                new PortalTemplateCreatedEvent(
                        UUID.randomUUID(),
                        Instant.parse("2026-04-20T12:00:00Z"),
                        "tenant-1",
                        "template-1",
                        "home-default",
                        com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType.HOME
                )
        ).applyTemplatePublished(new PortalTemplatePublishedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-20T12:05:00Z"),
                "tenant-1",
                "template-1",
                1,
                com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType.HOME
        ));
        repository.save(projection);

        assertThat(applicationService.current("template-1"))
                .hasValueSatisfying(view -> {
                    assertThat(view.templateCode()).isEqualTo("home-default");
                    assertThat(view.publishedVersionNo()).isEqualTo(1);
                });
    }
}
