package com.hjo2oa.portal.portal.designer.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplateListApplicationService;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PortalDesignerTemplateListControllerTest {

    @Test
    void shouldReturnDesignerTemplateListWithOptionalSceneFilter() throws Exception {
        InMemoryPortalDesignerTemplateProjectionRepository repository =
                new InMemoryPortalDesignerTemplateProjectionRepository();
        repository.save(projection("tenant-1", "template-1", "home-default", PortalPublicationSceneType.HOME));
        repository.save(projection("tenant-1", "template-2", "office-default", PortalPublicationSceneType.OFFICE_CENTER));
        repository.save(projection("tenant-2", "template-3", "cross-tenant", PortalPublicationSceneType.OFFICE_CENTER));
        MockMvc mockMvc = buildMockMvc(repository, "tenant-1");

        mockMvc.perform(get("/api/v1/portal/designer/templates")
                        .param("sceneType", "OFFICE_CENTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].templateId").value("template-2"))
                .andExpect(jsonPath("$.data[0].sceneType").value("OFFICE_CENTER"));
    }

    private MockMvc buildMockMvc(
            InMemoryPortalDesignerTemplateProjectionRepository repository,
            String tenantId
    ) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        PortalDesignerTemplateListApplicationService applicationService =
                new PortalDesignerTemplateListApplicationService(repository, contextProvider(tenantId));
        return MockMvcBuilders.standaloneSetup(
                        new PortalDesignerTemplateListController(applicationService, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private PortalDesignerTemplateProjection projection(
            String tenantId,
            String templateId,
            String templateCode,
            PortalPublicationSceneType sceneType
    ) {
        return PortalDesignerTemplateProjection.initialize(new PortalTemplateCreatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-20T15:30:00Z"),
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
