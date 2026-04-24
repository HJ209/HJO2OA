package com.hjo2oa.portal.portal.model.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.portal.model.application.CreatePortalTemplateCommand;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalTemplateRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PortalTemplateControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T11:00:00Z");

    @Test
    void shouldCreateTemplateUsingSharedWebContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/portal/model/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-template-create-1")
                        .content("""
                                {
                                  "templateId":"template-1",
                                  "templateCode":"home-default",
                                  "displayName":"Home Default",
                                  "sceneType":"HOME"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.templateId").value("template-1"))
                .andExpect(jsonPath("$.data.templateCode").value("home-default"))
                .andExpect(jsonPath("$.meta.requestId").value("req-template-create-1"));
    }

    @Test
    void shouldReturnCurrentTemplate() throws Exception {
        PortalTemplateApplicationService applicationService = applicationService();
        applicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "office-default",
                "Office Default",
                PortalPublicationSceneType.OFFICE_CENTER
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/portal/model/templates/template-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.displayName").value("Office Default"))
                .andExpect(jsonPath("$.data.versions[0].status").value("DRAFT"));
    }

    @Test
    void shouldReturnTemplateListWithOptionalSceneFilter() throws Exception {
        InMemoryPortalTemplateRepository repository = new InMemoryPortalTemplateRepository();
        PortalTemplateApplicationService tenantOneService = applicationService(repository, "tenant-1");
        PortalTemplateApplicationService tenantTwoService = applicationService(repository, "tenant-2");
        tenantOneService.create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        tenantOneService.create(new CreatePortalTemplateCommand(
                "template-2",
                "office-default",
                "Office Default",
                PortalPublicationSceneType.OFFICE_CENTER
        ));
        tenantTwoService.create(new CreatePortalTemplateCommand(
                "template-3",
                "office-tenant-two",
                "Tenant Two Office",
                PortalPublicationSceneType.OFFICE_CENTER
        ));
        MockMvc mockMvc = buildMockMvc(tenantOneService);

        mockMvc.perform(get("/api/v1/portal/model/templates")
                        .param("sceneType", "OFFICE_CENTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].templateId").value("template-2"))
                .andExpect(jsonPath("$.data[0].sceneType").value("OFFICE_CENTER"));
    }

    @Test
    void shouldPublishTemplateVersionUsingSharedWebContract() throws Exception {
        PortalTemplateApplicationService applicationService = applicationService();
        applicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(put("/api/v1/portal/model/templates/template-1/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versionNo":1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.publishedVersionNo").value(1))
                .andExpect(jsonPath("$.data.versions[0].status").value("PUBLISHED"));
    }

    @Test
    void shouldDeprecateTemplateVersionUsingSharedWebContract() throws Exception {
        PortalTemplateApplicationService applicationService = applicationService();
        applicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "mobile-default",
                "Mobile Default",
                PortalPublicationSceneType.MOBILE_WORKBENCH
        ));
        applicationService.publish(new com.hjo2oa.portal.portal.model.application.PublishPortalTemplateVersionCommand(
                "template-1",
                1
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(post("/api/v1/portal/model/templates/template-1/versions/1/deprecate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.versions[0].status").value("DEPRECATED"));
    }

    @Test
    void shouldRejectDuplicateTemplateCodeAsConflict() throws Exception {
        PortalTemplateApplicationService applicationService = applicationService();
        applicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(post("/api/v1/portal/model/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateId":"template-2",
                                  "templateCode":"home-default",
                                  "displayName":"Duplicate Home Default",
                                  "sceneType":"HOME"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    private MockMvc buildMockMvc() {
        return buildMockMvc(applicationService());
    }

    private MockMvc buildMockMvc(PortalTemplateApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(new PortalTemplateController(applicationService, responseMetaFactory))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private PortalTemplateApplicationService applicationService() {
        return applicationService(new InMemoryPortalTemplateRepository(), "tenant-1");
    }

    private PortalTemplateApplicationService applicationService(
            InMemoryPortalTemplateRepository repository,
            String tenantId
    ) {
        PortalModelContextProvider contextProvider = () -> new PortalModelContext(tenantId, "portal-admin");
        return new PortalTemplateApplicationService(
                repository,
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
