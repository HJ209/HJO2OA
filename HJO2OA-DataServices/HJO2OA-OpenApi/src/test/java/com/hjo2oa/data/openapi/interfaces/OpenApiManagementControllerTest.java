package com.hjo2oa.data.openapi.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.data.openapi.OpenApiTestFixtures;
import com.hjo2oa.data.openapi.application.OpenApiManagementApplicationService;
import com.hjo2oa.data.openapi.domain.OpenApiOperatorContext;
import com.hjo2oa.data.openapi.domain.OpenApiOperatorPermission;
import com.hjo2oa.data.openapi.infrastructure.InMemoryApiInvocationAuditLogRepository;
import com.hjo2oa.data.openapi.infrastructure.InMemoryApiQuotaUsageCounterRepository;
import com.hjo2oa.data.openapi.infrastructure.InMemoryOpenApiEndpointRepository;
import com.hjo2oa.data.openapi.infrastructure.StaticOpenApiOperatorContextProvider;
import com.hjo2oa.data.service.infrastructure.InMemoryDataServiceDefinitionRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OpenApiManagementControllerTest {

    @Test
    void shouldUpsertPublishAndListEndpointUsingSharedContract() throws Exception {
        InMemoryDataServiceDefinitionRepository dataServiceRepository = new InMemoryDataServiceDefinitionRepository();
        dataServiceRepository.save(OpenApiTestFixtures.activeDataService("employee.query"));
        OpenApiManagementApplicationService applicationService = new OpenApiManagementApplicationService(
                new InMemoryOpenApiEndpointRepository(),
                new InMemoryApiInvocationAuditLogRepository(),
                new InMemoryApiQuotaUsageCounterRepository(),
                dataServiceRepository,
                new StaticOpenApiOperatorContextProvider(new OpenApiOperatorContext(
                        OpenApiTestFixtures.TENANT_ID.toString(),
                        "open-api-admin",
                        EnumSet.allOf(OpenApiOperatorPermission.class)
                )),
                event -> {
                },
                Clock.fixed(OpenApiTestFixtures.FIXED_TIME, ZoneOffset.UTC)
        );
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new OpenApiManagementController(applicationService, responseMetaFactory))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();

        mockMvc.perform(put("/api/v1/data/open-api/endpoints/employee-directory/versions/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Employee Directory",
                                  "dataServiceCode":"employee.query",
                                  "path":"/api/open/employees",
                                  "httpMethod":"GET",
                                  "authType":"APP_KEY",
                                  "compatibilityNotes":"initial public release"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("employee-directory"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mockMvc.perform(put("/api/v1/data/open-api/endpoints/employee-directory/versions/v1/credentials/partner-app")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "secretRef":"secret-001",
                                  "scopes":["employee.read"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.credentialGrants.length()").value(1));

        mockMvc.perform(post("/api/v1/data/open-api/endpoints/employee-directory/versions/v1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/data/open-api/endpoints")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].code").value("employee-directory"))
                .andExpect(jsonPath("$.data.items[0].authType").value("APP_KEY"));
    }
}
