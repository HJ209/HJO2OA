package com.hjo2oa.wf.form.metadata.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.wf.form.metadata.application.FormMetadataApplicationService;
import com.hjo2oa.wf.form.metadata.infrastructure.InMemoryFormMetadataRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FormMetadataControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-27T01:00:00Z");
    private static final String TENANT_ID = "11111111-1111-1111-1111-111111111111";

    @Test
    void shouldUseSharedWebContractForMetadataLifecycleAndRenderSchema() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        String createResponse = mockMvc.perform(post("/api/v1/form/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-form-create-1")
                        .content("""
                                {
                                  "code":"leave.request",
                                  "name":"Leave Request",
                                  "nameI18nKey":"form.leave.name",
                                  "tenantId":"11111111-1111-1111-1111-111111111111",
                                  "layout":{"type":"grid"},
                                  "fieldPermissionMap":{
                                    "start":{"title":{"visible":true,"editable":true,"required":true}}
                                  },
                                  "fieldSchema":[
                                    {
                                      "fieldCode":"title",
                                      "fieldName":"Title",
                                      "fieldNameI18nKey":"form.field.title",
                                      "fieldType":"TEXT",
                                      "required":true,
                                      "multiValue":false,
                                      "visible":true,
                                      "editable":true,
                                      "maxLength":128
                                    },
                                    {
                                      "fieldCode":"leaveType",
                                      "fieldName":"Leave Type",
                                      "fieldNameI18nKey":"form.field.leaveType",
                                      "fieldType":"SELECT",
                                      "required":true,
                                      "dictionaryCode":"leave.type",
                                      "multiValue":false,
                                      "visible":true,
                                      "editable":true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.nameI18nKey").value("form.leave.name"))
                .andExpect(jsonPath("$.meta.requestId").value("req-form-create-1"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String metadataId = createResponse.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/v1/form/metadata/" + metadataId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fieldSchema.length()").value(2));

        mockMvc.perform(post("/api/v1/form/metadata/" + metadataId + "/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].fieldCode").value("leaveType"));

        mockMvc.perform(put("/api/v1/form/metadata/" + metadataId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Leave Request V1",
                                  "nameI18nKey":"form.leave.name",
                                  "layout":{"type":"grid"},
                                  "fieldSchema":[
                                    {
                                      "fieldCode":"title",
                                      "fieldName":"Title",
                                      "fieldType":"TEXT",
                                      "required":true,
                                      "multiValue":false,
                                      "visible":true,
                                      "editable":true,
                                      "maxLength":128
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Leave Request V1"));

        mockMvc.perform(post("/api/v1/form/metadata/" + metadataId + "/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/form/render-schemas/leave.request")
                        .param("tenantId", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1));

        mockMvc.perform(post("/api/v1/form/metadata/" + metadataId + "/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mockMvc.perform(get("/api/v1/form/metadata/leave.request/versions")
                        .param("tenantId", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/v1/form/metadata")
                        .param("tenantId", TENANT_ID)
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void shouldRejectDuplicateFieldCodeAsBusinessRuleViolation() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/form/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"bad.form",
                                  "name":"Bad Form",
                                  "tenantId":"11111111-1111-1111-1111-111111111111",
                                  "layout":{"type":"grid"},
                                  "fieldSchema":[
                                    {
                                      "fieldCode":"title",
                                      "fieldName":"Title",
                                      "fieldType":"TEXT",
                                      "required":true,
                                      "visible":true,
                                      "editable":true
                                    },
                                    {
                                      "fieldCode":"title",
                                      "fieldName":"Title Copy",
                                      "fieldType":"TEXT",
                                      "required":true,
                                      "visible":true,
                                      "editable":true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    private MockMvc buildMockMvc() {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        FormMetadataApplicationService applicationService = new FormMetadataApplicationService(
                new InMemoryFormMetadataRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        return MockMvcBuilders.standaloneSetup(new FormMetadataController(
                        applicationService,
                        new FormMetadataDtoMapper(),
                        responseMetaFactory
                ))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }
}
