package com.hjo2oa.portal.portal.designer.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplateDraftApplicationService;
import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplateInitializationApplicationService;
import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplateStatusApplicationService;
import com.hjo2oa.portal.portal.designer.application.PortalDesignerTemplateWidgetPaletteApplicationService;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.infrastructure.InMemoryPortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.model.application.CreatePortalTemplateCommand;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalWidgetReferenceStatusApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalTemplateRepository;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalWidgetReferenceStatusRepository;
import com.hjo2oa.portal.widget.config.application.UpsertWidgetDefinitionCommand;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.application.WidgetDefinitionApplicationService;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContext;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContextProvider;
import com.hjo2oa.portal.widget.config.domain.WidgetDataSourceType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import com.hjo2oa.portal.widget.config.infrastructure.InMemoryWidgetDefinitionRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PortalDesignerTemplateDraftControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T17:30:00Z");

    @Test
    void shouldSaveDesignerDraftAndReturnInitializationPayload() throws Exception {
        TestFixture fixture = fixture();
        fixture.projectionRepository().save(PortalDesignerTemplateProjection.initialize(new PortalTemplateCreatedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "template-1",
                "home-default",
                PortalPublicationSceneType.HOME
        )));
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        fixture.widgetDefinitionApplicationService().upsert(new UpsertWidgetDefinitionCommand(
                "widget-1",
                "home-message",
                "Home Message",
                WidgetCardType.MESSAGE,
                WidgetSceneType.HOME,
                "message-center",
                WidgetDataSourceType.AGGREGATION_QUERY,
                false,
                true,
                5
        ));
        MockMvc mockMvc = buildMockMvc(fixture.applicationService());

        mockMvc.perform(put("/api/v1/portal/designer/templates/template-1/draft")
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
                .andExpect(jsonPath("$.data.templateId").value("template-1"))
                .andExpect(jsonPath("$.data.canvas.pages[0].pageCode").value("home-custom"))
                .andExpect(jsonPath("$.data.canvas.pages[0].regions[0].placements[0].widgetCode").value("message-card"))
                .andExpect(jsonPath("$.data.widgetPalette.widgets[0].widgetCode").value("home-message"));
    }

    @Test
    void shouldRejectInvalidDesignerDraftCanvas() throws Exception {
        TestFixture fixture = fixture();
        fixture.projectionRepository().save(PortalDesignerTemplateProjection.initialize(new PortalTemplateCreatedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "template-1",
                "home-default",
                PortalPublicationSceneType.HOME
        )));
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        MockMvc mockMvc = buildMockMvc(fixture.applicationService());

        mockMvc.perform(put("/api/v1/portal/designer/templates/template-1/draft")
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

    @Test
    void shouldRejectRepairRequiredWidgetReferenceWhenSavingDesignerDraft() throws Exception {
        TestFixture fixture = fixture();
        fixture.projectionRepository().save(PortalDesignerTemplateProjection.initialize(new PortalTemplateCreatedEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "template-1",
                "home-default",
                PortalPublicationSceneType.HOME
        )));
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Default",
                PortalPublicationSceneType.HOME
        ));
        fixture.widgetReferenceStatusApplicationService().markDisabled(new PortalWidgetDisabledEvent(
                UUID.randomUUID(),
                FIXED_TIME,
                "tenant-1",
                "widget-message",
                "message-card",
                WidgetCardType.MESSAGE,
                WidgetSceneType.HOME
        ));
        MockMvc mockMvc = buildMockMvc(fixture.applicationService());

        mockMvc.perform(put("/api/v1/portal/designer/templates/template-1/draft")
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
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("widgetCode=message-card")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                        "placementCode=placement-message"
                )));
    }

    private MockMvc buildMockMvc(PortalDesignerTemplateDraftApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(
                        new PortalDesignerTemplateDraftController(applicationService, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private TestFixture fixture() {
        InMemoryPortalDesignerTemplateProjectionRepository projectionRepository =
                new InMemoryPortalDesignerTemplateProjectionRepository();
        InMemoryPortalTemplateRepository templateRepository = new InMemoryPortalTemplateRepository();
        PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService =
                new PortalWidgetReferenceStatusApplicationService(new InMemoryPortalWidgetReferenceStatusRepository());

        PortalModelContextProvider portalModelContextProvider =
                () -> new PortalModelContext("tenant-1", "portal-admin");
        PortalTemplateApplicationService templateApplicationService = new PortalTemplateApplicationService(
                templateRepository,
                portalModelContextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                widgetReferenceStatusApplicationService
        );

        WidgetConfigContextProvider widgetContextProvider =
                () -> new WidgetConfigContext("tenant-1", "portal-admin");
        WidgetDefinitionApplicationService widgetDefinitionApplicationService =
                new WidgetDefinitionApplicationService(
                        new InMemoryWidgetDefinitionRepository(),
                        widgetContextProvider,
                        event -> {
                        },
                        Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
                );

        PortalTemplateCanvasApplicationService templateCanvasApplicationService =
                new PortalTemplateCanvasApplicationService(
                        templateRepository,
                        Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                        widgetReferenceStatusApplicationService
                );

        PortalDesignerTemplateDraftApplicationService applicationService =
                new PortalDesignerTemplateDraftApplicationService(
                        templateCanvasApplicationService,
                        new PortalDesignerTemplateInitializationApplicationService(
                                new PortalDesignerTemplateStatusApplicationService(projectionRepository),
                                new PortalDesignerTemplateWidgetPaletteApplicationService(
                                        projectionRepository,
                                        widgetDefinitionApplicationService
                                ),
                                templateCanvasApplicationService
                        )
                );

        return new TestFixture(
                projectionRepository,
                templateApplicationService,
                widgetDefinitionApplicationService,
                widgetReferenceStatusApplicationService,
                applicationService
        );
    }

    private record TestFixture(
            InMemoryPortalDesignerTemplateProjectionRepository projectionRepository,
            PortalTemplateApplicationService templateApplicationService,
            WidgetDefinitionApplicationService widgetDefinitionApplicationService,
            PortalWidgetReferenceStatusApplicationService widgetReferenceStatusApplicationService,
            PortalDesignerTemplateDraftApplicationService applicationService
    ) {
    }
}
