package com.hjo2oa.portal.portal.designer.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplateStatusApplicationService;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplatePublishedEvent;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PortalDesignerTemplateControllerTest {

    @Test
    void shouldReturnDesignerTemplateStatus() throws Exception {
        InMemoryPortalDesignerTemplateProjectionRepository repository =
                new InMemoryPortalDesignerTemplateProjectionRepository();
        PortalDesignerTemplateProjection projection = PortalDesignerTemplateProjection.initialize(
                new PortalTemplateCreatedEvent(
                        UUID.randomUUID(),
                        Instant.parse("2026-04-20T13:30:00Z"),
                        "tenant-1",
                        "template-1",
                        "home-default",
                        PortalPublicationSceneType.HOME
                )
        ).applyTemplatePublished(new PortalTemplatePublishedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-20T13:35:00Z"),
                "tenant-1",
                "template-1",
                1,
                PortalPublicationSceneType.HOME
        ));
        repository.save(projection);
        MockMvc mockMvc = buildMockMvc(repository);

        mockMvc.perform(get("/api/v1/portal/designer/templates/template-1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.templateCode").value("home-default"))
                .andExpect(jsonPath("$.data.publishedVersionNo").value(1))
                .andExpect(jsonPath("$.data.hasActivePublication").value(false));
    }

    @Test
    void shouldReturnNotFoundWhenDesignerTemplateStatusIsMissing() throws Exception {
        MockMvc mockMvc = buildMockMvc(new InMemoryPortalDesignerTemplateProjectionRepository());

        mockMvc.perform(get("/api/v1/portal/designer/templates/template-1/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private MockMvc buildMockMvc(InMemoryPortalDesignerTemplateProjectionRepository repository) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        PortalDesignerTemplateStatusApplicationService applicationService =
                new PortalDesignerTemplateStatusApplicationService(repository);
        return MockMvcBuilders.standaloneSetup(
                        new PortalDesignerTemplateController(applicationService, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }
}
