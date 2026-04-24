package com.hjo2oa.portal.portal.model.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.portal.model.application.CreatePortalTemplateCommand;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
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

class PortalTemplateCanvasControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T14:30:00Z");

    @Test
    void shouldReturnTemplateCanvas() throws Exception {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        MockMvc mockMvc = buildMockMvc(fixture.applicationService());

        mockMvc.perform(get("/api/v1/portal/model/templates/template-1/canvas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.templateId").value("template-1"))
                .andExpect(jsonPath("$.data.pages[0].pageCode").value("home-main"))
                .andExpect(jsonPath("$.data.pages[0].regions[1].placements[0].widgetCode").value("todo-card"));
    }

    @Test
    void shouldReturnNotFoundWhenTemplateCanvasIsMissing() throws Exception {
        MockMvc mockMvc = buildMockMvc(fixture().applicationService());

        mockMvc.perform(get("/api/v1/portal/model/templates/missing-template/canvas"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldSaveTemplateCanvas() throws Exception {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        MockMvc mockMvc = buildMockMvc(fixture.applicationService());

        mockMvc.perform(put("/api/v1/portal/model/templates/template-1/canvas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pages":[
                                    {
                                      "pageId":"page-home-custom",
                                      "pageCode":"home-custom",
                                      "title":"Home Custom",
                                      "defaultPage":true,
                                      "layoutMode":"THREE_SECTION",
                                      "regions":[
                                        {
                                          "regionId":"region-work-focus",
                                          "regionCode":"work-focus",
                                          "title":"Work Focus",
                                          "required":true,
                                          "placements":[
                                            {
                                              "placementId":"placement-todo",
                                              "placementCode":"placement-todo",
                                              "widgetCode":"todo-card",
                                              "cardType":"TODO",
                                              "orderNo":20
                                            },
                                            {
                                              "placementId":"placement-message",
                                              "placementCode":"placement-message",
                                              "widgetCode":"message-card",
                                              "cardType":"MESSAGE",
                                              "orderNo":10
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.pages[0].pageCode").value("home-custom"))
                .andExpect(jsonPath("$.data.pages[0].regions[0].placements[0].widgetCode").value("message-card"))
                .andExpect(jsonPath("$.data.pages[0].regions[0].placements[1].widgetCode").value("todo-card"));
    }

    @Test
    void shouldRejectDuplicateRegionCodeWhenSavingTemplateCanvas() throws Exception {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        MockMvc mockMvc = buildMockMvc(fixture.applicationService());

        mockMvc.perform(put("/api/v1/portal/model/templates/template-1/canvas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pages":[
                                    {
                                      "pageId":"page-home-1",
                                      "pageCode":"home-1",
                                      "title":"Home One",
                                      "defaultPage":true,
                                      "layoutMode":"THREE_SECTION",
                                      "regions":[
                                        {
                                          "regionId":"region-1",
                                          "regionCode":"work-focus",
                                          "title":"Work Focus",
                                          "required":true,
                                          "placements":[
                                            {
                                              "placementId":"placement-1",
                                              "placementCode":"placement-1",
                                              "widgetCode":"todo-card",
                                              "cardType":"TODO",
                                              "orderNo":10
                                            }
                                          ]
                                        }
                                      ]
                                    },
                                    {
                                      "pageId":"page-home-2",
                                      "pageCode":"home-2",
                                      "title":"Home Two",
                                      "defaultPage":false,
                                      "layoutMode":"THREE_SECTION",
                                      "regions":[
                                        {
                                          "regionId":"region-2",
                                          "regionCode":"work-focus",
                                          "title":"Work Focus Again",
                                          "required":true,
                                          "placements":[
                                            {
                                              "placementId":"placement-2",
                                              "placementCode":"placement-2",
                                              "widgetCode":"message-card",
                                              "cardType":"MESSAGE",
                                              "orderNo":20
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    private MockMvc buildMockMvc(PortalTemplateCanvasApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(
                        new PortalTemplateCanvasController(applicationService, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private TestFixture fixture() {
        InMemoryPortalTemplateRepository repository = new InMemoryPortalTemplateRepository();
        PortalModelContextProvider contextProvider = () -> new PortalModelContext("tenant-1", "portal-admin");
        PortalTemplateApplicationService templateApplicationService = new PortalTemplateApplicationService(
                repository,
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        return new TestFixture(
                templateApplicationService,
                new PortalTemplateCanvasApplicationService(
                        repository,
                        Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
                )
        );
    }

    private record TestFixture(
            PortalTemplateApplicationService templateApplicationService,
            PortalTemplateCanvasApplicationService applicationService
    ) {
    }
}
