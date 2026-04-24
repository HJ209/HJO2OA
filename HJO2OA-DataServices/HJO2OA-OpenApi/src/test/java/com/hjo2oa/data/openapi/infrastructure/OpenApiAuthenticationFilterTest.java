package com.hjo2oa.data.openapi.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.data.openapi.OpenApiTestFixtures;
import com.hjo2oa.data.openapi.application.OpenApiAuthenticationApplicationService;
import com.hjo2oa.data.openapi.domain.ApiCredentialGrant;
import com.hjo2oa.data.openapi.domain.ApiInvocationOutcome;
import com.hjo2oa.data.openapi.domain.OpenApiAuthType;
import com.hjo2oa.data.openapi.domain.OpenApiEndpoint;
import com.hjo2oa.data.openapi.domain.OpenApiHttpMethod;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class OpenApiAuthenticationFilterTest {

    @Test
    void shouldAuthenticateAppKeyRequestAndWriteAuditLog() throws Exception {
        Fixture fixture = new Fixture();
        MockMvc mockMvc = fixture.buildMockMvc();

        mockMvc.perform(get("/api/open/employees")
                        .header(OpenApiAuthenticationApplicationService.TENANT_HEADER, OpenApiTestFixtures.TENANT_ID.toString())
                        .header(OpenApiAuthenticationApplicationService.CLIENT_CODE_HEADER, "partner-app")
                        .header(OpenApiAuthenticationApplicationService.CLIENT_SECRET_HEADER, "secret-001")
                        .header(OpenApiAuthenticationApplicationService.API_VERSION_HEADER, "v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.ok").value(true));

        assertThat(fixture.auditLogRepository.findAllByTenant(OpenApiTestFixtures.TENANT_ID.toString()))
                .singleElement()
                .extracting("outcome")
                .isEqualTo(ApiInvocationOutcome.SUCCESS);
    }

    @Test
    void shouldRejectInvalidSecretAsUnauthorized() throws Exception {
        Fixture fixture = new Fixture();
        MockMvc mockMvc = fixture.buildMockMvc();

        mockMvc.perform(get("/api/open/employees")
                        .header(OpenApiAuthenticationApplicationService.TENANT_HEADER, OpenApiTestFixtures.TENANT_ID.toString())
                        .header(OpenApiAuthenticationApplicationService.CLIENT_CODE_HEADER, "partner-app")
                        .header(OpenApiAuthenticationApplicationService.CLIENT_SECRET_HEADER, "invalid-secret")
                        .header(OpenApiAuthenticationApplicationService.API_VERSION_HEADER, "v1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").exists());

        assertThat(fixture.auditLogRepository.findAllByTenant(OpenApiTestFixtures.TENANT_ID.toString()))
                .singleElement()
                .extracting("outcome")
                .isEqualTo(ApiInvocationOutcome.AUTH_FAILED);
    }

    private static final class Fixture {

        private final InMemoryApiInvocationAuditLogRepository auditLogRepository = new InMemoryApiInvocationAuditLogRepository();
        private final InMemoryOpenApiEndpointRepository endpointRepository = new InMemoryOpenApiEndpointRepository();

        private Fixture() {
            ApiCredentialGrant credentialGrant = ApiCredentialGrant.create(
                    "api-1",
                    OpenApiTestFixtures.TENANT_ID.toString(),
                    "partner-app",
                    "secret-001",
                    List.of("employee.read"),
                    OpenApiTestFixtures.FIXED_TIME.plusSeconds(3600),
                    OpenApiTestFixtures.FIXED_TIME
            );
            OpenApiEndpoint endpoint = OpenApiEndpoint.create(
                    OpenApiTestFixtures.TENANT_ID.toString(),
                    "employee-directory",
                    "Employee Directory",
                    UUID.randomUUID().toString(),
                    "employee.query",
                    "Employee Query",
                    "/api/open/employees",
                    OpenApiHttpMethod.GET,
                    "v1",
                    OpenApiAuthType.APP_KEY,
                    null,
                    OpenApiTestFixtures.FIXED_TIME
            ).withCredential(credentialGrant).publish(OpenApiTestFixtures.FIXED_TIME);
            endpointRepository.save(endpoint);
        }

        private MockMvc buildMockMvc() {
            ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
            OpenApiAuthenticationFilter filter = new OpenApiAuthenticationFilter(
                    new OpenApiAuthenticationApplicationService(
                            endpointRepository,
                            Clock.fixed(OpenApiTestFixtures.FIXED_TIME, ZoneOffset.UTC)
                    ),
                    new OpenApiInvocationContextHolder(),
                    auditLogRepository,
                    responseMetaFactory,
                    new ObjectMapper(),
                    Clock.fixed(OpenApiTestFixtures.FIXED_TIME, ZoneOffset.UTC)
            );
            return MockMvcBuilders.standaloneSetup(new DummyOpenApiController(responseMetaFactory))
                    .addFilters(filter)
                    .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                    .build();
        }
    }

    @RestController
    @UseSharedWebContract
    static class DummyOpenApiController {

        private final ResponseMetaFactory responseMetaFactory;

        DummyOpenApiController(ResponseMetaFactory responseMetaFactory) {
            this.responseMetaFactory = responseMetaFactory;
        }

        @GetMapping("/api/open/employees")
        ApiResponse<Map<String, Object>> employees(HttpServletRequest request) {
            return ApiResponse.success(Map.of("ok", true), responseMetaFactory.create(request));
        }
    }
}
