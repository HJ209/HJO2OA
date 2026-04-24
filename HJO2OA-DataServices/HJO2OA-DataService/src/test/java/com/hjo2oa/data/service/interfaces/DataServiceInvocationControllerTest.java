package com.hjo2oa.data.service.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.data.common.interfaces.web.DataServicesGlobalExceptionHandler;
import com.hjo2oa.data.service.application.DataServiceInvocationApplicationService;
import com.hjo2oa.data.service.domain.DataServiceDefinition;
import com.hjo2oa.data.service.domain.DataServiceOperationContext;
import com.hjo2oa.data.service.domain.DataServiceOperationContextProvider;
import com.hjo2oa.data.service.domain.ServiceFieldMapping;
import com.hjo2oa.data.service.domain.ServiceParameterDefinition;
import com.hjo2oa.data.service.support.InMemoryDataServiceDefinitionRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DataServiceInvocationControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T03:30:00Z");

    @Test
    void shouldPrepareQueryExecutionUsingControllerContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/data-services/runtime/employee-query/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-data-runtime-query-1")
                        .header("X-Idempotency-Key", "idem-query-1")
                        .content("""
                                {
                                  "appCode":"open-api",
                                  "parameters":{
                                    "keyword":"alice"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.serviceCode").value("employee-query"))
                .andExpect(jsonPath("$.data.cacheEnabled").value(true))
                .andExpect(jsonPath("$.data.normalizedParameters.keyword").value("alice"))
                .andExpect(jsonPath("$.data.outputFields[0]").value("name"))
                .andExpect(jsonPath("$.meta.requestId").value("req-data-runtime-query-1"));
    }

    @Test
    void shouldReturnValidationErrorWhenRequiredParameterMissing() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/data-services/runtime/employee-query/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appCode":"open-api",
                                  "parameters":{}
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DATA-COMMON-422"));
    }

    private MockMvc buildMockMvc() {
        InMemoryDataServiceDefinitionRepository repository = new InMemoryDataServiceDefinitionRepository();
        seedQueryService(repository);
        DataServiceOperationContextProvider contextProvider = () -> new DataServiceOperationContext(
                TENANT_ID,
                "runtime-user",
                Set.of(DataServiceOperationContext.ROLE_DATA_SERVICE_AUDITOR),
                Set.of("open-api"),
                Set.of("subject-1"),
                true
        );
        DataServiceInvocationApplicationService applicationService = new DataServiceInvocationApplicationService(
                repository,
                contextProvider,
                new ObjectMapper(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        DataServiceDefinitionDtoMapper dtoMapper = new DataServiceDefinitionDtoMapper();
        return MockMvcBuilders.standaloneSetup(
                        new DataServiceInvocationController(applicationService, dtoMapper, responseMetaFactory)
                )
                .setControllerAdvice(new DataServicesGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private void seedQueryService(InMemoryDataServiceDefinitionRepository repository) {
        UUID serviceId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        DataServiceDefinition definition = DataServiceDefinition.create(
                serviceId,
                TENANT_ID,
                "employee-query",
                "Employee Query",
                DataServiceDefinition.ServiceType.QUERY,
                DataServiceDefinition.SourceMode.INTERNAL_QUERY,
                DataServiceDefinition.PermissionMode.APP_SCOPED,
                new DataServiceDefinition.PermissionBoundary(List.of("open-api"), List.of(), List.of()),
                new DataServiceDefinition.CachePolicy(
                        true,
                        120L,
                        "{tenantId}:{serviceCode}:{appCode}:{param.keyword}",
                        DataServiceDefinition.CacheScope.APP,
                        false,
                        List.of("org.person.updated")
                ),
                "employee.query",
                null,
                "Employee query definition",
                "data-admin",
                FIXED_TIME,
                List.of(
                        new ServiceParameterDefinition(
                                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                                serviceId,
                                "keyword",
                                ServiceParameterDefinition.ParameterType.STRING,
                                true,
                                null,
                                new ServiceParameterDefinition.ValidationRule(1, 64, null, null, null, List.of(), null),
                                true,
                                "Keyword",
                                1
                        ),
                        new ServiceParameterDefinition(
                                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                                serviceId,
                                "page",
                                ServiceParameterDefinition.ParameterType.PAGEABLE,
                                false,
                                "{\"page\":1,\"size\":20}",
                                new ServiceParameterDefinition.ValidationRule(null, null, null, null, null, List.of(), 50),
                                true,
                                "Paging",
                                2
                        )
                ),
                List.of(
                        new ServiceFieldMapping(
                                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                                serviceId,
                                "employeeName",
                                "name",
                                ServiceFieldMapping.TransformRule.none(),
                                false,
                                "Name",
                                1
                        )
                )
        ).activate("data-admin", FIXED_TIME);
        repository.save(definition);
    }
}
