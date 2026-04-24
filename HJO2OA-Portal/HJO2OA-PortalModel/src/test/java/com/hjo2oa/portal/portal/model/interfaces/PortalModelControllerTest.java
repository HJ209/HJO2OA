package com.hjo2oa.portal.portal.model.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.portal.model.application.ActivatePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalPublicationRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PortalModelControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T06:30:00Z");

    @Test
    void shouldActivatePublicationUsingSharedWebContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(put("/api/v1/portal/model/publications/publication-1/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-publication-activate-1")
                        .content("""
                                {
                                  "templateId":"template-1",
                                  "sceneType":"OFFICE_CENTER",
                                  "clientType":"PC",
                                  "personId":"person-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.publicationId").value("publication-1"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.audience.type").value("person"))
                .andExpect(jsonPath("$.data.audience.subjectId").value("person-1"))
                .andExpect(jsonPath("$.meta.requestId").value("req-publication-activate-1"));
    }

    @Test
    void shouldReturnCurrentPublication() throws Exception {
        PortalPublicationApplicationService applicationService = applicationService();
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.ALL,
                PortalPublicationAudience.ofPerson("person-1")
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/portal/model/publications/publication-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.templateId").value("template-1"))
                .andExpect(jsonPath("$.data.audience.type").value("person"))
                .andExpect(jsonPath("$.data.audience.subjectId").value("person-1"));
    }

    @Test
    void shouldReturnPublicationListWithOptionalFilters() throws Exception {
        InMemoryPortalPublicationRepository repository = new InMemoryPortalPublicationRepository();
        PortalPublicationApplicationService tenantOneService = applicationService(repository, "tenant-1");
        PortalPublicationApplicationService tenantTwoService = applicationService(repository, "tenant-2");
        tenantOneService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.ofPerson("person-1")
        ));
        tenantOneService.activate(new ActivatePortalPublicationCommand(
                "publication-2",
                "template-2",
                PortalPublicationSceneType.OFFICE_CENTER,
                PortalPublicationClientType.ALL,
                PortalPublicationAudience.tenantDefault()
        ));
        tenantOneService.offline(new com.hjo2oa.portal.portal.model.application.OfflinePortalPublicationCommand(
                "publication-2"
        ));
        tenantTwoService.activate(new ActivatePortalPublicationCommand(
                "publication-3",
                "template-3",
                PortalPublicationSceneType.OFFICE_CENTER,
                PortalPublicationClientType.ALL,
                PortalPublicationAudience.tenantDefault()
        ));
        MockMvc mockMvc = buildMockMvc(tenantOneService);

        mockMvc.perform(get("/api/v1/portal/model/publications")
                        .param("sceneType", "OFFICE_CENTER")
                        .param("status", "OFFLINE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].publicationId").value("publication-2"))
                .andExpect(jsonPath("$.data[0].status").value("OFFLINE"))
                .andExpect(jsonPath("$.data[0].audience.type").value("tenant-default"));
    }

    @Test
    void shouldReturnCurrentActivePublication() throws Exception {
        PortalPublicationApplicationService applicationService = applicationService();
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-default",
                "template-default",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-person",
                "template-person",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.ofPerson("person-1")
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/portal/model/publications/active")
                        .param("sceneType", "HOME")
                        .param("clientType", "PC")
                        .param("personId", "person-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.publicationId").value("publication-person"))
                .andExpect(jsonPath("$.data.audience.type").value("person"));
    }

    @Test
    void shouldReturnNotFoundWhenActivePublicationWasOfflined() throws Exception {
        PortalPublicationApplicationService applicationService = applicationService();
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.ALL,
                PortalPublicationAudience.tenantDefault()
        ));
        applicationService.offline(new com.hjo2oa.portal.portal.model.application.OfflinePortalPublicationCommand(
                "publication-1"
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/portal/model/publications/active")
                        .param("sceneType", "HOME")
                        .param("clientType", "ALL"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldOfflinePublicationUsingSharedWebContract() throws Exception {
        PortalPublicationApplicationService applicationService = applicationService();
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(post("/api/v1/portal/model/publications/publication-1/offline")
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-publication-offline-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("OFFLINE"))
                .andExpect(jsonPath("$.data.audience.type").value("tenant-default"))
                .andExpect(jsonPath("$.meta.requestId").value("req-publication-offline-1"));
    }

    @Test
    void shouldRejectConflictingActivePublicationAsConflict() throws Exception {
        PortalPublicationApplicationService applicationService = applicationService();
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(put("/api/v1/portal/model/publications/publication-2/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateId":"template-2",
                                  "sceneType":"HOME",
                                  "clientType":"PC"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    private MockMvc buildMockMvc() {
        return buildMockMvc(applicationService());
    }

    private MockMvc buildMockMvc(PortalPublicationApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(new PortalModelController(applicationService, responseMetaFactory))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private PortalPublicationApplicationService applicationService() {
        return applicationService(new InMemoryPortalPublicationRepository(), "tenant-1");
    }

    private PortalPublicationApplicationService applicationService(
            InMemoryPortalPublicationRepository repository,
            String tenantId
    ) {
        PortalModelContextProvider contextProvider = () -> new PortalModelContext(tenantId, "portal-admin");
        return new PortalPublicationApplicationService(
                repository,
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
