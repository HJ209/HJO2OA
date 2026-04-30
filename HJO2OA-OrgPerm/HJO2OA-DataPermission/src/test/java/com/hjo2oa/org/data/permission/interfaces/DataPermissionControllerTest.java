package com.hjo2oa.org.data.permission.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.org.data.permission.application.DataPermissionApplicationService;
import com.hjo2oa.org.data.permission.domain.FieldPermissionRuntimeMasker;
import com.hjo2oa.org.data.permission.infrastructure.InMemoryDataPermissionRepository;
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

class DataPermissionControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-27T02:00:00Z");

    @Test
    void shouldUseSharedContractForPolicyLifecycleAndDecisions() throws Exception {
        MockMvc mockMvc = buildMockMvc();
        String roleId = "11111111-1111-1111-1111-111111111111";
        String personId = "22222222-2222-2222-2222-222222222222";

        String rowResponse = mockMvc.perform(post("/api/v1/org/data-permissions/row-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-row-1")
                        .content("""
                                {
                                  "subjectType":"ROLE",
                                  "subjectId":"11111111-1111-1111-1111-111111111111",
                                  "businessObject":"process_instance",
                                  "scopeType":"ALL",
                                  "effect":"ALLOW",
                                  "priority":10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.businessObject").value("process_instance"))
                .andExpect(jsonPath("$.meta.requestId").value("req-row-1"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String rowPolicyId = rowResponse.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/v1/org/data-permissions/row-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectType":"PERSON",
                                  "subjectId":"22222222-2222-2222-2222-222222222222",
                                  "businessObject":"process_instance",
                                  "scopeType":"SELF",
                                  "effect":"DENY",
                                  "priority":0
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/org/data-permissions/decisions/row")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessObject":"process_instance",
                                  "subjects":[
                                    {"subjectType":"ROLE","subjectId":"%s"},
                                    {"subjectType":"PERSON","subjectId":"%s"}
                                  ]
                                }
                                """.formatted(roleId, personId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(false))
                .andExpect(jsonPath("$.data.sqlCondition").value("1 = 0"))
                .andExpect(jsonPath("$.data.effect").value("DENY"))
                .andExpect(jsonPath("$.data.matchedPolicies.length()").value(2));

        mockMvc.perform(get("/api/v1/org/data-permissions/row-policies/" + rowPolicyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopeType").value("ALL"));

        mockMvc.perform(post("/api/v1/org/data-permissions/field-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectType":"ROLE",
                                  "subjectId":"11111111-1111-1111-1111-111111111111",
                                  "businessObject":"person_profile",
                                  "usageScenario":"view",
                                  "fieldCode":"mobile",
                                  "action":"DESENSITIZED",
                                  "effect":"ALLOW"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fieldCode").value("mobile"));

        mockMvc.perform(post("/api/v1/org/data-permissions/decisions/field")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessObject":"person_profile",
                                  "usageScenario":"view",
                                  "fieldCodes":["mobile"],
                                  "subjects":[
                                    {"subjectType":"ROLE","subjectId":"11111111-1111-1111-1111-111111111111"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fieldEffects.mobile.DESENSITIZED").value("ALLOW"));

        mockMvc.perform(delete("/api/v1/org/data-permissions/row-policies/" + rowPolicyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));
    }

    private MockMvc buildMockMvc() {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        DataPermissionApplicationService applicationService = new DataPermissionApplicationService(
                new InMemoryDataPermissionRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        DataPermissionDtoMapper dtoMapper = new DataPermissionDtoMapper();
        return MockMvcBuilders.standaloneSetup(
                        new DataPermissionController(
                                applicationService,
                                dtoMapper,
                                new FieldPermissionRuntimeMasker(),
                                responseMetaFactory
                        )
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }
}
