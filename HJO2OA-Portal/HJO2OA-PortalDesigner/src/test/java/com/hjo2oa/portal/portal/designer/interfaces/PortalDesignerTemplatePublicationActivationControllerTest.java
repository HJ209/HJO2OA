package com.hjo2oa.portal.portal.designer.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplatePublicationActivationApplicationService;
import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplateStatusApplicationService;
import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.model.application.CreatePortalTemplateCommand;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.application.PublishPortalTemplateVersionCommand;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationActivatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationOfflinedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateDeprecatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplatePublishedEvent;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalPublicationRepository;
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

class PortalDesignerTemplatePublicationActivationControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T19:15:00Z");

    @Test
    void shouldActivateDesignerTemplatePublication() throws Exception {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        fixture.templateApplicationService().publish(new PublishPortalTemplateVersionCommand("template-1", 1));
        MockMvc mockMvc = buildMockMvc(fixture.applicationService());

        mockMvc.perform(put("/api/v1/portal/designer/templates/template-1/publications/publication-1/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientType":"PC"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.templateId").value("template-1"))
                .andExpect(jsonPath("$.data.hasActivePublication").value(true))
                .andExpect(jsonPath("$.data.activePublicationIds[0]").value("publication-1"));
    }

    @Test
    void shouldRejectActivationWhenDesignerTemplateHasNotBeenPublished() throws Exception {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        MockMvc mockMvc = buildMockMvc(fixture.applicationService());

        mockMvc.perform(put("/api/v1/portal/designer/templates/template-1/publications/publication-1/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientType":"ALL"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    private MockMvc buildMockMvc(PortalDesignerTemplatePublicationActivationApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(
                        new PortalDesignerTemplatePublicationActivationController(applicationService, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private TestFixture fixture() {
        InMemoryPortalDesignerTemplateProjectionRepository projectionRepository =
                new InMemoryPortalDesignerTemplateProjectionRepository();
        PortalDesignerTemplateEventListener eventListener =
                new PortalDesignerTemplateEventListener(projectionRepository);

        PortalModelContextProvider contextProvider = () -> new PortalModelContext("tenant-1", "portal-admin");
        PortalTemplateApplicationService templateApplicationService = new PortalTemplateApplicationService(
                new InMemoryPortalTemplateRepository(),
                contextProvider,
                event -> {
                    if (event instanceof PortalTemplateCreatedEvent createdEvent) {
                        eventListener.onTemplateCreated(createdEvent);
                    } else if (event instanceof PortalTemplatePublishedEvent publishedEvent) {
                        eventListener.onTemplatePublished(publishedEvent);
                    } else if (event instanceof PortalTemplateDeprecatedEvent deprecatedEvent) {
                        eventListener.onTemplateDeprecated(deprecatedEvent);
                    }
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        PortalPublicationApplicationService publicationApplicationService = new PortalPublicationApplicationService(
                new InMemoryPortalPublicationRepository(),
                contextProvider,
                event -> {
                    if (event instanceof PortalPublicationActivatedEvent activatedEvent) {
                        eventListener.onPublicationActivated(activatedEvent);
                    } else if (event instanceof PortalPublicationOfflinedEvent offlinedEvent) {
                        eventListener.onPublicationOfflined(offlinedEvent);
                    }
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        return new TestFixture(
                templateApplicationService,
                new PortalDesignerTemplatePublicationActivationApplicationService(
                        templateApplicationService,
                        publicationApplicationService,
                        new PortalDesignerTemplateStatusApplicationService(projectionRepository)
                )
        );
    }

    private record TestFixture(
            PortalTemplateApplicationService templateApplicationService,
            PortalDesignerTemplatePublicationActivationApplicationService applicationService
    ) {
    }
}
