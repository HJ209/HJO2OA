package com.hjo2oa.portal.portal.model.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.portal.model.application.ActivatePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.application.CreatePortalTemplateCommand;
import com.hjo2oa.portal.portal.model.application.PortalActiveTemplateResolutionApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.application.PublishPortalTemplateVersionCommand;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalPublicationRepository;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalTemplateRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PortalActiveTemplateResolutionControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T13:00:00Z");

    @Test
    void shouldReturnCurrentActiveTemplateResolution() throws Exception {
        TestFixture fixture = fixture();
        seedTemplate(
                fixture.templateApplicationService(),
                "template-default",
                "home-default",
                "Home Default"
        );
        fixture.publicationApplicationService().activate(new ActivatePortalPublicationCommand(
                "publication-default",
                "template-default",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.ALL,
                PortalPublicationAudience.tenantDefault()
        ));
        seedTemplate(
                fixture.templateApplicationService(),
                "template-person",
                "home-person",
                "Home Person"
        );
        fixture.publicationApplicationService().activate(new ActivatePortalPublicationCommand(
                "publication-person",
                "template-person",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.ALL,
                PortalPublicationAudience.ofPerson("person-1")
        ));
        MockMvc mockMvc = buildMockMvc(fixture.applicationService());

        mockMvc.perform(get("/api/v1/portal/model/resolutions/active")
                        .param("sceneType", "HOME")
                        .param("clientType", "ALL")
                        .param("personId", "person-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.publicationId").value("publication-person"))
                .andExpect(jsonPath("$.data.templateCode").value("home-person"))
                .andExpect(jsonPath("$.data.audience.type").value("person"))
                .andExpect(jsonPath("$.data.publishedVersionNo").value(1));
    }

    @Test
    void shouldReturnTenantDefaultResolutionWhenIdentityIsNotProvided() throws Exception {
        TestFixture fixture = fixture();
        seedTemplate(
                fixture.templateApplicationService(),
                "template-default",
                "home-default",
                "Home Default"
        );
        fixture.publicationApplicationService().activate(new ActivatePortalPublicationCommand(
                "publication-default",
                "template-default",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.ALL,
                PortalPublicationAudience.tenantDefault()
        ));
        MockMvc mockMvc = buildMockMvc(fixture.applicationService());

        mockMvc.perform(get("/api/v1/portal/model/resolutions/active")
                        .param("sceneType", "HOME")
                        .param("clientType", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.publicationId").value("publication-default"))
                .andExpect(jsonPath("$.data.audience.type").value("tenant-default"));
    }

    @Test
    void shouldReturnNotFoundWhenActiveResolutionIsMissing() throws Exception {
        MockMvc mockMvc = buildMockMvc(fixture().applicationService());

        mockMvc.perform(get("/api/v1/portal/model/resolutions/active")
                        .param("sceneType", "HOME")
                        .param("clientType", "PC"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private MockMvc buildMockMvc(PortalActiveTemplateResolutionApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(
                        new PortalActiveTemplateResolutionController(applicationService, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private TestFixture fixture() {
        PortalModelContextProvider contextProvider = () -> new PortalModelContext("tenant-1", "portal-admin");
        PortalPublicationApplicationService publicationApplicationService = new PortalPublicationApplicationService(
                new InMemoryPortalPublicationRepository(),
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        PortalTemplateApplicationService templateApplicationService = new PortalTemplateApplicationService(
                new InMemoryPortalTemplateRepository(),
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        return new TestFixture(
                publicationApplicationService,
                templateApplicationService,
                new PortalActiveTemplateResolutionApplicationService(
                        publicationApplicationService,
                        templateApplicationService
                )
        );
    }

    private void seedTemplate(
            PortalTemplateApplicationService templateApplicationService,
            String templateId,
            String templateCode,
            String displayName
    ) {
        templateApplicationService.create(new CreatePortalTemplateCommand(
                templateId,
                templateCode,
                displayName,
                PortalPublicationSceneType.HOME
        ));
        templateApplicationService.publish(new PublishPortalTemplateVersionCommand(templateId, 1));
    }

    private record TestFixture(
            PortalPublicationApplicationService publicationApplicationService,
            PortalTemplateApplicationService templateApplicationService,
            PortalActiveTemplateResolutionApplicationService applicationService
    ) {
    }
}
