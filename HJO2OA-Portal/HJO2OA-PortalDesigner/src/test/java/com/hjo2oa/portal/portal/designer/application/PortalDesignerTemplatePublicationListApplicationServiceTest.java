package com.hjo2oa.portal.portal.designer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.model.application.ActivatePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.application.OfflinePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationStatus;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalPublicationRepository;
import com.hjo2oa.shared.kernel.BizException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortalDesignerTemplatePublicationListApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-21T02:00:00Z");

    @Test
    void shouldListOnlyCurrentTenantTemplatePublicationsInStableOrder() {
        TestFixture fixture = fixture();
        fixture.seedTemplate("tenant-1", "template-1", PortalPublicationSceneType.HOME);
        fixture.seedTemplate("tenant-1", "template-2", PortalPublicationSceneType.HOME);
        fixture.seedTemplate("tenant-2", "template-3", PortalPublicationSceneType.HOME);

        fixture.tenantPublicationService("tenant-1").activate(new ActivatePortalPublicationCommand(
                "publication-3",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        fixture.tenantPublicationService("tenant-1").offline(new OfflinePortalPublicationCommand("publication-3"));
        fixture.tenantPublicationService("tenant-1").activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        fixture.tenantPublicationService("tenant-1").activate(new ActivatePortalPublicationCommand(
                "publication-2",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.ALL,
                PortalPublicationAudience.tenantDefault()
        ));
        fixture.tenantPublicationService("tenant-1").activate(new ActivatePortalPublicationCommand(
                "publication-9",
                "template-2",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.MOBILE,
                PortalPublicationAudience.tenantDefault()
        ));
        fixture.tenantPublicationService("tenant-2").activate(new ActivatePortalPublicationCommand(
                "publication-4",
                "template-3",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));

        List<com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplatePublicationView> publications =
                fixture.applicationService("tenant-1").list("template-1", null, null);

        assertThat(publications)
                .extracting(com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplatePublicationView::publicationId)
                .containsExactly("publication-1", "publication-2", "publication-3");
        assertThat(publications)
                .extracting(com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplatePublicationView::templateId)
                .containsOnly("template-1");
    }

    @Test
    void shouldCombineClientTypeAndStatusFilters() {
        TestFixture fixture = fixture();
        fixture.seedTemplate("tenant-1", "template-1", PortalPublicationSceneType.HOME);

        fixture.tenantPublicationService("tenant-1").activate(new ActivatePortalPublicationCommand(
                "publication-offline",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        fixture.tenantPublicationService("tenant-1").offline(new OfflinePortalPublicationCommand("publication-offline"));
        fixture.tenantPublicationService("tenant-1").activate(new ActivatePortalPublicationCommand(
                "publication-active",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        fixture.tenantPublicationService("tenant-1").activate(new ActivatePortalPublicationCommand(
                "publication-all",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.ALL,
                PortalPublicationAudience.tenantDefault()
        ));

        List<com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplatePublicationView> publications =
                fixture.applicationService("tenant-1").list(
                        "template-1",
                        PortalPublicationClientType.PC,
                        PortalPublicationStatus.ACTIVE
                );

        assertThat(publications)
                .extracting(com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplatePublicationView::publicationId)
                .containsExactly("publication-active");
    }

    @Test
    void shouldRejectQueryWhenTemplateIsNotVisibleToCurrentTenant() {
        TestFixture fixture = fixture();
        fixture.seedTemplate("tenant-2", "template-3", PortalPublicationSceneType.HOME);

        assertThatThrownBy(() -> fixture.applicationService("tenant-1").list("template-3", null, null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Portal designer template not found");
    }

    private TestFixture fixture() {
        InMemoryPortalDesignerTemplateProjectionRepository projectionRepository =
                new InMemoryPortalDesignerTemplateProjectionRepository();
        InMemoryPortalPublicationRepository publicationRepository = new InMemoryPortalPublicationRepository();
        return new TestFixture(projectionRepository, publicationRepository);
    }

    private record TestFixture(
            InMemoryPortalDesignerTemplateProjectionRepository projectionRepository,
            InMemoryPortalPublicationRepository publicationRepository
    ) {

        private void seedTemplate(String tenantId, String templateId, PortalPublicationSceneType sceneType) {
            projectionRepository.save(PortalDesignerTemplateProjection.initialize(new PortalTemplateCreatedEvent(
                    UUID.randomUUID(),
                    FIXED_TIME,
                    tenantId,
                    templateId,
                    templateId + "-code",
                    sceneType
            )));
        }

        private PortalDesignerTemplatePublicationListApplicationService applicationService(String tenantId) {
            return new PortalDesignerTemplatePublicationListApplicationService(
                    projectionRepository,
                    tenantPublicationService(tenantId),
                    contextProvider(tenantId)
            );
        }

        private PortalPublicationApplicationService tenantPublicationService(String tenantId) {
            return new PortalPublicationApplicationService(
                    publicationRepository,
                    contextProvider(tenantId),
                    event -> {
                    },
                    Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
            );
        }

        private PortalModelContextProvider contextProvider(String tenantId) {
            return () -> new PortalModelContext(tenantId, "portal-admin");
        }
    }
}
