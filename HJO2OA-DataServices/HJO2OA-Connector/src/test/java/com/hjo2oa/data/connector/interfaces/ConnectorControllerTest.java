package com.hjo2oa.data.connector.interfaces;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.data.connector.application.ConfigureConnectorParametersCommand;
import com.hjo2oa.data.connector.application.ConnectorDefinitionApplicationService;
import com.hjo2oa.data.connector.application.ConnectorParameterValue;
import com.hjo2oa.data.connector.application.UpsertConnectorDefinitionCommand;
import com.hjo2oa.data.connector.domain.ConnectorAuthMode;
import com.hjo2oa.data.connector.domain.ConnectorContext;
import com.hjo2oa.data.connector.domain.ConnectorType;
import com.hjo2oa.data.connector.domain.TimeoutRetryConfig;
import com.hjo2oa.data.connector.infrastructure.ApiIdempotencyStore;
import com.hjo2oa.data.connector.infrastructure.ConnectorDriverRegistry;
import com.hjo2oa.data.connector.infrastructure.ConnectorSecretValueResolver;
import com.hjo2oa.data.connector.infrastructure.DatabaseConnectorDriver;
import com.hjo2oa.data.connector.infrastructure.DriverManagerJdbcConnectivityClient;
import com.hjo2oa.data.connector.infrastructure.HttpConnectorDriver;
import com.hjo2oa.data.connector.infrastructure.HttpConnectivityClient;
import com.hjo2oa.data.connector.infrastructure.HttpRequestSpec;
import com.hjo2oa.data.connector.infrastructure.InMemoryApiIdempotencyStore;
import com.hjo2oa.data.connector.infrastructure.InMemoryConnectorDefinitionRepository;
import com.hjo2oa.data.connector.infrastructure.MessageQueueConnectionSpec;
import com.hjo2oa.data.connector.infrastructure.MessageQueueConnectorDriver;
import com.hjo2oa.data.connector.infrastructure.MessageQueueConnectivityClient;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ConnectorControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T03:00:00Z");

    @Test
    void shouldUseSharedWebContractForConnectorCrudAndTestEndpoints() throws Exception {
        ConnectorDefinitionApplicationService applicationService = applicationService();
        MockMvc mockMvc = buildMockMvc(applicationService, new InMemoryApiIdempotencyStore());

        mockMvc.perform(put("/api/v1/data/connectors/http-1")
                        .header(ConnectorController.IDEMPOTENCY_KEY_HEADER, "idem-upsert-1")
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-upsert-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"http-1",
                                  "name":"HTTP One",
                                  "connectorType":"HTTP",
                                  "vendor":"demo",
                                  "protocol":"https",
                                  "authMode":"TOKEN",
                                  "timeoutConfig":{
                                    "connectTimeoutMs":2000,
                                    "readTimeoutMs":3000,
                                    "retryCount":1,
                                    "retryIntervalMs":10
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.code").value("http-1"))
                .andExpect(jsonPath("$.meta.requestId").value("req-upsert-1"));

        mockMvc.perform(put("/api/v1/data/connectors/http-1/parameters")
                        .header(ConnectorController.IDEMPOTENCY_KEY_HEADER, "idem-params-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parameters":[
                                    {"paramKey":"baseUrl","paramValueRef":"https://example.test","sensitive":false},
                                    {"paramKey":"healthPath","paramValueRef":"/health","sensitive":false},
                                    {"paramKey":"token","paramValueRef":"keyRef:http.demo.token","sensitive":true}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parameters.length()").value(3))
                .andExpect(jsonPath("$.data.parameters[2].paramKey").value("token"));

        mockMvc.perform(get("/api/v1/data/connectors/http-1/parameter-templates")
                        .param("category", "AUTH")
                        .param("sensitive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].paramKey").value("token"));

        mockMvc.perform(post("/api/v1/data/connectors/http-1/test")
                        .header(ConnectorController.IDEMPOTENCY_KEY_HEADER, "idem-test-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.healthStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.data.targetEnvironment").value("local"));

        mockMvc.perform(post("/api/v1/data/connectors/http-1/health/refresh")
                        .header(ConnectorController.IDEMPOTENCY_KEY_HEADER, "idem-health-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkType").value("HEALTH_CHECK"));

        mockMvc.perform(post("/api/v1/data/connectors/http-1/activate")
                        .header(ConnectorController.IDEMPOTENCY_KEY_HEADER, "idem-activate-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/data/connectors")
                        .param("code", "http-1")
                        .param("status", "ACTIVE")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].code").value("http-1"))
                .andExpect(jsonPath("$.data.items[0].latestTestSnapshot.healthStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.data.pagination.total").value(1))
                .andExpect(jsonPath("$.data.filters.code").value("http-1"))
                .andExpect(jsonPath("$.data.filters.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/data/connectors/http-1/tests/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkType").value("MANUAL_TEST"));

        mockMvc.perform(get("/api/v1/data/connectors/http-1/tests/history")
                        .param("status", "HEALTHY")
                        .param("checkedFrom", "2026-04-24T02:59:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].checkType").value("MANUAL_TEST"));

        mockMvc.perform(get("/api/v1/data/connectors/http-1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latestHealthSnapshot.checkType").value("HEALTH_CHECK"))
                .andExpect(jsonPath("$.data.latestHealthSnapshot.targetEnvironment").value("local"));

        mockMvc.perform(get("/api/v1/data/connectors/http-1/health/history")
                        .param("status", "HEALTHY")
                        .param("checkedFrom", "2026-04-24T02:59:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].checkType").value("HEALTH_CHECK"));
    }

    @Test
    void shouldReturnEmptyHealthOverviewBeforeAnyHealthSnapshot() throws Exception {
        ConnectorDefinitionApplicationService applicationService = applicationService();
        MockMvc mockMvc = buildMockMvc(applicationService, new InMemoryApiIdempotencyStore());

        mockMvc.perform(put("/api/v1/data/connectors/http-empty-health")
                        .header(ConnectorController.IDEMPOTENCY_KEY_HEADER, "idem-empty-health")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"http-empty-health",
                                  "name":"HTTP Empty Health",
                                  "connectorType":"HTTP",
                                  "vendor":"demo",
                                  "protocol":"https",
                                  "authMode":"NONE"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/data/connectors/http-empty-health/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.connectorId").value("http-empty-health"))
                .andExpect(jsonPath("$.data.latestHealthSnapshot").value(nullValue()))
                .andExpect(jsonPath("$.data.sampleSize").value(0));
    }

    @Test
    void shouldReturnConflictWithOriginalBodyForDuplicateIdempotencyKey() throws Exception {
        MockMvc mockMvc = buildMockMvc(applicationService(), new InMemoryApiIdempotencyStore());

        String requestBody = """
                {
                  "code":"http-dup",
                  "name":"HTTP Duplicate",
                  "connectorType":"HTTP",
                  "vendor":"demo",
                  "protocol":"https",
                  "authMode":"TOKEN"
                }
                """;

        mockMvc.perform(put("/api/v1/data/connectors/http-dup")
                        .header(ConnectorController.IDEMPOTENCY_KEY_HEADER, "same-idem-key")
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-dup-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("http-dup"));

        mockMvc.perform(put("/api/v1/data/connectors/http-dup")
                        .header(ConnectorController.IDEMPOTENCY_KEY_HEADER, "same-idem-key")
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-dup-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.meta.requestId").value("req-dup-1"));
    }

    @Test
    void shouldRejectWriteRequestWithoutIdempotencyKey() throws Exception {
        MockMvc mockMvc = buildMockMvc(applicationService(), new InMemoryApiIdempotencyStore());

        mockMvc.perform(put("/api/v1/data/connectors/http-missing-idem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"http-missing-idem",
                                  "name":"HTTP Missing Idem",
                                  "connectorType":"HTTP",
                                  "vendor":"demo",
                                  "protocol":"https",
                                  "authMode":"TOKEN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldConfirmUnhealthySnapshotThroughController() throws Exception {
        MockMvc mockMvc = buildMockMvc(
                applicationService(requestSpec -> {
                    throw new IllegalStateException("down");
                }),
                new InMemoryApiIdempotencyStore()
        );

        mockMvc.perform(put("/api/v1/data/connectors/http-fail-1")
                        .header(ConnectorController.IDEMPOTENCY_KEY_HEADER, "idem-upsert-fail-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"http-fail-1",
                                  "name":"HTTP Fail One",
                                  "connectorType":"HTTP",
                                  "vendor":"demo",
                                  "protocol":"https",
                                  "authMode":"TOKEN"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/data/connectors/http-fail-1/parameters")
                        .header(ConnectorController.IDEMPOTENCY_KEY_HEADER, "idem-params-fail-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parameters":[
                                    {"paramKey":"baseUrl","paramValueRef":"https://example.test","sensitive":false},
                                    {"paramKey":"token","paramValueRef":"keyRef:http.demo.token","sensitive":true}
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        String responseBody = mockMvc.perform(post("/api/v1/data/connectors/http-fail-1/health/refresh")
                        .header(ConnectorController.IDEMPOTENCY_KEY_HEADER, "idem-health-fail-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.healthStatus").value("DEGRADED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String snapshotId = responseBody.replaceAll(".*\"snapshotId\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/v1/data/connectors/http-fail-1/health/" + snapshotId + "/confirm")
                        .header(ConnectorController.IDEMPOTENCY_KEY_HEADER, "idem-health-confirm-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note":"已人工确认网络异常"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmedBy").value("connector-admin"))
                .andExpect(jsonPath("$.data.confirmationNote").value("已人工确认网络异常"));
    }

    private MockMvc buildMockMvc(
            ConnectorDefinitionApplicationService applicationService,
            ApiIdempotencyStore apiIdempotencyStore
    ) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(new ConnectorController(
                        applicationService,
                        responseMetaFactory,
                        apiIdempotencyStore
                ))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    private ConnectorDefinitionApplicationService applicationService() {
        return applicationService(new HttpConnectivityClient() {
            @Override
            public int execute(HttpRequestSpec requestSpec) {
                return 200;
            }
        });
    }

    private ConnectorDefinitionApplicationService applicationService(HttpConnectivityClient httpConnectivityClient) {
        ConnectorSecretValueResolver secretValueResolver = (paramValueRef, sensitive) ->
                sensitive ? "token-1" : paramValueRef;
        ConnectorDriverRegistry registry = new ConnectorDriverRegistry(List.of(
                new HttpConnectorDriver(secretValueResolver, httpConnectivityClient),
                new DatabaseConnectorDriver(secretValueResolver, new DriverManagerJdbcConnectivityClient() {
                    @Override
                    public void validate(
                            String jdbcUrl,
                            String username,
                            String password,
                            String validationQuery,
                            TimeoutRetryConfig timeoutRetryConfig
                    ) {
                    }
                }),
                new MessageQueueConnectorDriver(secretValueResolver, new MessageQueueConnectivityClient() {
                    @Override
                    public void validate(
                            MessageQueueConnectionSpec connectionSpec,
                            TimeoutRetryConfig timeoutRetryConfig
                    ) {
                    }
                })
        ));
        return new ConnectorDefinitionApplicationService(
                new InMemoryConnectorDefinitionRepository(),
                () -> new ConnectorContext("tenant-1", "connector-admin", "local"),
                registry,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
