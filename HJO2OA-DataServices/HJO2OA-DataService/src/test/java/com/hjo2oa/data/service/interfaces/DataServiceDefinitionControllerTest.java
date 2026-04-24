package com.hjo2oa.data.service.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.data.common.interfaces.web.DataServicesGlobalExceptionHandler;
import com.hjo2oa.data.service.application.DataServiceDefinitionApplicationService;
import com.hjo2oa.data.service.domain.DataServiceOperationContext;
import com.hjo2oa.data.service.domain.DataServiceOperationContextProvider;
import com.hjo2oa.data.service.support.InMemoryDataServiceDefinitionRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DataServiceDefinitionControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T03:00:00Z");

    @Test
    void shouldCreateDefinitionUsingApiResponseContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/data-services/definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-data-service-create-1")
                        .content("""
                                {
                                  "serviceId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                                  "code":"todo-query",
                                  "name":"Todo Query",
                                  "serviceType":"QUERY",
                                  "sourceMode":"INTERNAL_QUERY",
                                  "permissionMode":"PUBLIC_INTERNAL",
                                  "permissionBoundary":{
                                    "allowedAppCodes":[],
                                    "allowedSubjectIds":[],
                                    "requiredRoles":[]
                                  },
                                  "cachePolicy":{
                                    "enabled":true,
                                    "ttlSeconds":300,
                                    "cacheKeyTemplate":"{tenantId}:{serviceCode}:{param.keyword}",
                                    "scope":"TENANT",
                                    "cacheNullValue":false,
                                    "invalidationEvents":["process.updated"]
                                  },
                                  "sourceRef":"todo-center.pending",
                                  "description":"Todo query definition"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.code").value("todo-query"))
                .andExpect(jsonPath("$.data.cachePolicy.enabled").value(true))
                .andExpect(jsonPath("$.meta.requestId").value("req-data-service-create-1"));
    }

    @Test
    void shouldReturnPagedListAndActivateDefinition() throws Exception {
        InMemoryDataServiceDefinitionRepository repository = new InMemoryDataServiceDefinitionRepository();
        DataServiceDefinitionApplicationService applicationService = applicationService(repository);
        applicationService.create(new DataServiceDefinitionDtos.CreateRequest(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "todo-query",
                "Todo Query",
                com.hjo2oa.data.service.domain.DataServiceDefinition.ServiceType.QUERY,
                com.hjo2oa.data.service.domain.DataServiceDefinition.SourceMode.INTERNAL_QUERY,
                com.hjo2oa.data.service.domain.DataServiceDefinition.PermissionMode.PUBLIC_INTERNAL,
                new DataServiceDefinitionDtos.PermissionBoundaryPayload(java.util.List.of(), java.util.List.of(), java.util.List.of()),
                new DataServiceDefinitionDtos.CachePolicyPayload(
                        true,
                        300L,
                        "{tenantId}:{serviceCode}:{param.keyword}",
                        com.hjo2oa.data.service.domain.DataServiceDefinition.CacheScope.TENANT,
                        false,
                        java.util.List.of("process.updated")
                ),
                "todo-center.pending",
                null,
                "Todo query definition",
                java.util.List.of(),
                java.util.List.of()
        ).toCommand());
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/data-services/definitions")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].code").value("todo-query"))
                .andExpect(jsonPath("$.data.pagination.total").value(1));

        mockMvc.perform(post("/api/v1/data-services/definitions/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/activate")
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-data-service-activate-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.meta.requestId").value("req-data-service-activate-1"));
    }

    @Test
    void shouldUpsertParameterAndDeleteDisabledDefinition() throws Exception {
        InMemoryDataServiceDefinitionRepository repository = new InMemoryDataServiceDefinitionRepository();
        DataServiceDefinitionApplicationService applicationService = applicationService(repository);
        applicationService.create(new DataServiceDefinitionDtos.CreateRequest(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "todo-query",
                "Todo Query",
                com.hjo2oa.data.service.domain.DataServiceDefinition.ServiceType.QUERY,
                com.hjo2oa.data.service.domain.DataServiceDefinition.SourceMode.INTERNAL_QUERY,
                com.hjo2oa.data.service.domain.DataServiceDefinition.PermissionMode.PUBLIC_INTERNAL,
                new DataServiceDefinitionDtos.PermissionBoundaryPayload(java.util.List.of(), java.util.List.of(), java.util.List.of()),
                new DataServiceDefinitionDtos.CachePolicyPayload(
                        true,
                        300L,
                        "{tenantId}:{serviceCode}:{param.keyword}",
                        com.hjo2oa.data.service.domain.DataServiceDefinition.CacheScope.TENANT,
                        false,
                        java.util.List.of("process.updated")
                ),
                "todo-center.pending",
                null,
                "Todo query definition",
                java.util.List.of(),
                java.util.List.of()
        ).toCommand());
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(put("/api/v1/data-services/definitions/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/parameters/keyword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paramType":"STRING",
                                  "required":true,
                                  "enabled":true,
                                  "sortOrder":1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paramCode").value("keyword"));

        mockMvc.perform(post("/api/v1/data-services/definitions/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(delete("/api/v1/data-services/definitions/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));
    }

    @Test
    void shouldReturnConflictForDuplicateServiceCode() throws Exception {
        InMemoryDataServiceDefinitionRepository repository = new InMemoryDataServiceDefinitionRepository();
        DataServiceDefinitionApplicationService applicationService = applicationService(repository);
        applicationService.create(new DataServiceDefinitionDtos.CreateRequest(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "todo-query",
                "Todo Query",
                com.hjo2oa.data.service.domain.DataServiceDefinition.ServiceType.QUERY,
                com.hjo2oa.data.service.domain.DataServiceDefinition.SourceMode.INTERNAL_QUERY,
                com.hjo2oa.data.service.domain.DataServiceDefinition.PermissionMode.PUBLIC_INTERNAL,
                new DataServiceDefinitionDtos.PermissionBoundaryPayload(java.util.List.of(), java.util.List.of(), java.util.List.of()),
                new DataServiceDefinitionDtos.CachePolicyPayload(
                        true,
                        300L,
                        "{tenantId}:{serviceCode}:{param.keyword}",
                        com.hjo2oa.data.service.domain.DataServiceDefinition.CacheScope.TENANT,
                        false,
                        java.util.List.of("process.updated")
                ),
                "todo-center.pending",
                null,
                "Todo query definition",
                java.util.List.of(),
                java.util.List.of()
        ).toCommand());
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(post("/api/v1/data-services/definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                                  "code":"todo-query",
                                  "name":"Todo Query Copy",
                                  "serviceType":"QUERY",
                                  "sourceMode":"INTERNAL_QUERY",
                                  "permissionMode":"PUBLIC_INTERNAL",
                                  "permissionBoundary":{
                                    "allowedAppCodes":[],
                                    "allowedSubjectIds":[],
                                    "requiredRoles":[]
                                  },
                                  "cachePolicy":{
                                    "enabled":true,
                                    "ttlSeconds":300,
                                    "cacheKeyTemplate":"{tenantId}:{serviceCode}:{param.keyword}",
                                    "scope":"TENANT",
                                    "cacheNullValue":false,
                                    "invalidationEvents":["process.updated"]
                                  },
                                  "sourceRef":"todo-center.pending",
                                  "description":"Todo query definition"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA-COMMON-409"));
    }

    private MockMvc buildMockMvc() {
        return buildMockMvc(applicationService(new InMemoryDataServiceDefinitionRepository()));
    }

    private MockMvc buildMockMvc(DataServiceDefinitionApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        DataServiceDefinitionDtoMapper dtoMapper = new DataServiceDefinitionDtoMapper();
        return MockMvcBuilders.standaloneSetup(
                        new DataServiceDefinitionController(applicationService, dtoMapper, responseMetaFactory)
                )
                .setControllerAdvice(new DataServicesGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private DataServiceDefinitionApplicationService applicationService(
            InMemoryDataServiceDefinitionRepository repository
    ) {
        DataServiceOperationContextProvider contextProvider = () -> new DataServiceOperationContext(
                TENANT_ID,
                "data-admin",
                Set.of(
                        DataServiceOperationContext.ROLE_PLATFORM_ADMIN,
                        DataServiceOperationContext.ROLE_DATA_SERVICE_MANAGER,
                        DataServiceOperationContext.ROLE_DATA_SERVICE_AUDITOR
                ),
                Set.of("open-api", "report"),
                Set.of("subject-1"),
                true
        );
        return new DataServiceDefinitionApplicationService(
                repository,
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
