package com.hjo2oa.wf.process.definition.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import com.hjo2oa.wf.process.definition.application.ProcessDefinitionApplicationService;
import com.hjo2oa.wf.process.definition.infrastructure.InMemoryActionDefinitionRepository;
import com.hjo2oa.wf.process.definition.infrastructure.InMemoryProcessDefinitionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProcessDefinitionControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T06:00:00Z");
    private static final String TENANT_ID = "11111111-1111-1111-1111-111111111111";

    @Test
    void shouldUseSharedWebContractForDefinitionLifecycleAndActions() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        String createResponse = mockMvc.perform(post("/api/v1/workflow/process-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-proc-def-create-1")
                        .content(definitionJson("leave", "Leave Approval")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.code").value("leave"))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.meta.requestId").value("req-proc-def-create-1"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String definitionId = createResponse.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/v1/workflow/process-definitions/" + definitionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(definitionJson("leave", "Leave Approval Updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Leave Approval Updated"));

        mockMvc.perform(put("/api/v1/workflow/process-definitions/" + definitionId + "/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publishedBy\":\"22222222-2222-2222-2222-222222222222\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.publishedBy").value("22222222-2222-2222-2222-222222222222"));

        mockMvc.perform(post("/api/v1/workflow/process-definitions/" + definitionId + "/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mockMvc.perform(get("/api/v1/workflow/process-definitions")
                        .param("tenantId", TENANT_ID)
                        .param("code", "leave"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        String actionResponse = mockMvc.perform(post("/api/v1/workflow/process-definitions/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actionJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("approve"))
                .andExpect(jsonPath("$.data.category").value("APPROVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String actionId = actionResponse.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/v1/workflow/process-definitions/actions")
                        .param("tenantId", TENANT_ID)
                        .param("category", "APPROVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(delete("/api/v1/workflow/process-definitions/actions/" + actionId))
                .andExpect(status().isOk());
    }

    private String definitionJson(String code, String name) {
        return """
                {
                  "code":"%s",
                  "name":"%s",
                  "category":"HR",
                  "startNodeId":"start",
                  "endNodeId":"end",
                  "nodes":[],
                  "routes":[],
                  "tenantId":"%s"
                }
                """.formatted(code, name, TENANT_ID);
    }

    private String actionJson() {
        return """
                {
                  "code":"approve",
                  "name":"Approve",
                  "category":"APPROVE",
                  "routeTarget":"NEXT_NODE",
                  "requireOpinion":false,
                  "requireTarget":false,
                  "uiConfig":{"color":"green"},
                  "tenantId":"%s"
                }
                """.formatted(TENANT_ID);
    }

    private MockMvc buildMockMvc() {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        ProcessDefinitionApplicationService applicationService = new ProcessDefinitionApplicationService(
                new InMemoryProcessDefinitionRepository(),
                new InMemoryActionDefinitionRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        ProcessDefinitionDtoMapper dtoMapper = new ProcessDefinitionDtoMapper();
        return MockMvcBuilders.standaloneSetup(
                        new ProcessDefinitionController(applicationService, dtoMapper, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }
}
