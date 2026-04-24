package com.hjo2oa.portal.portal.designer.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortalDesignerTemplateListApplicationServiceTest {

    @Test
    void shouldListTemplatesForCurrentTenantWithOptionalSceneFilter() {
        InMemoryPortalDesignerTemplateProjectionRepository repository =
                new InMemoryPortalDesignerTemplateProjectionRepository();
        repository.save(projection("tenant-1", "template-2", "office-default", PortalPublicationSceneType.OFFICE_CENTER));
        repository.save(projection("tenant-1", "template-1", "home-default", PortalPublicationSceneType.HOME));
        repository.save(projection("tenant-2", "template-3", "cross-tenant", PortalPublicationSceneType.HOME));

        PortalDesignerTemplateListApplicationService applicationService =
                new PortalDesignerTemplateListApplicationService(repository, contextProvider("tenant-1"));

        List<com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateStatusView> allTemplates =
                applicationService.list(null);
        List<com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateStatusView> homeTemplates =
                applicationService.list(PortalPublicationSceneType.HOME);

        assertThat(allTemplates).extracting(com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateStatusView::templateId)
                .containsExactly("template-1", "template-2");
        assertThat(homeTemplates).extracting(com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateStatusView::templateId)
                .containsExactly("template-1");
    }

    private PortalDesignerTemplateProjection projection(
            String tenantId,
            String templateId,
            String templateCode,
            PortalPublicationSceneType sceneType
    ) {
        return PortalDesignerTemplateProjection.initialize(new PortalTemplateCreatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-20T15:00:00Z"),
                tenantId,
                templateId,
                templateCode,
                sceneType
        ));
    }

    private PortalModelContextProvider contextProvider(String tenantId) {
        return () -> new PortalModelContext(tenantId, "portal-admin");
    }
}
