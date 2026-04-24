package com.hjo2oa.portal.portal.designer.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplatePublicationListApplicationService;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.model.application.ActivatePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.application.OfflinePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalPublicationRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PortalDesignerTemplatePublicationListControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-21T02:30:00Z");

    @Test
    void shouldReturnTemplatePublicationsWithCombinedFilters() throws Exception {
        TestFixture fixture = fixture("tenant-1");
        fixture.seedTemplate("tenant-1", "template-1", PortalPublicationSceneType.HOME);
        fixture.seedTemplate("tenant-1", "template-2", PortalPublicationSceneType.HOME);
        fixture.seedTemplate("tenant-2", "template-3", PortalPublicationSceneType.HOME);

        fixture.tenantPublicationService("tenant-1").activate(new ActivatePortalPublicationCommand(
                "publication-old",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        fixture.tenantPublicationService("tenant-1").offline(new OfflinePortalPublicationCommand("publication-old"));
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
                "publication-3",
                "template-3",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));

        MockMvc mockMvc = buildMockMvc(fixture.applicationService());

        mockMvc.perform(get("/api/v1/portal/designer/templates/template-1/publications")
                        .param("clientType", "PC")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].publicationId").value("publication-1"))
                .andExpect(jsonPath("$.data[0].templateId").value("template-1"))
                .andExpect(jsonPath("$.data[0].clientType").value("PC"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    void shouldReturnNotFoundWhenTemplateIsOutsideCurrentTenant() throws Exception {
        TestFixture fixture = fixture("tenant-1");
        fixture.seedTemplate("tenant-2", "template-3", PortalPublicationSceneType.HOME);
        MockMvc mockMvc = buildMockMvc(fixture.applicationService());

        mockMvc.perform(get("/api/v1/portal/designer/templates/template-3/publications"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private MockMvc buildMockMvc(PortalDesignerTemplatePublicationListApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(
                        new PortalDesignerTemplatePublicationListController(applicationService, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private TestFixture fixture(String tenantId) {
        InMemoryPortalDesignerTemplateProjectionRepository projectionRepository =
                new InMemoryPortalDesignerTemplateProjectionRepository();
        InMemoryPortalPublicationRepository publicationRepository = new InMemoryPortalPublicationRepository();
        return new TestFixture(tenantId, projectionRepository, publicationRepository);
    }

    private record TestFixture(
            String tenantId,
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

        private PortalDesignerTemplatePublicationListApplicationService applicationService() {
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
