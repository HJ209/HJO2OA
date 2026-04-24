package com.hjo2oa.data.openapi.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.data.openapi.OpenApiTestFixtures;
import com.hjo2oa.data.openapi.domain.ApiCredentialGrant;
import com.hjo2oa.data.openapi.domain.AuthenticatedOpenApiInvocationContext;
import com.hjo2oa.data.openapi.domain.OpenApiAuthType;
import com.hjo2oa.data.openapi.domain.OpenApiEndpoint;
import com.hjo2oa.data.openapi.domain.OpenApiHttpMethod;
import com.hjo2oa.data.openapi.infrastructure.OpenApiInvocationContextHolder;
import com.hjo2oa.data.service.application.DataServiceInvocationApplicationService;
import com.hjo2oa.data.service.infrastructure.InMemoryDataServiceDefinitionRepository;
import com.hjo2oa.data.service.infrastructure.StaticDataServiceOperationContextProvider;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OpenApiInvocationApplicationServiceTest {

    @Test
    void shouldInvokeThroughDataServiceDefinitionInsteadOfPerCallerImplementation() {
        InMemoryDataServiceDefinitionRepository repository = new InMemoryDataServiceDefinitionRepository();
        repository.save(OpenApiTestFixtures.activeDataService("employee.query"));

        OpenApiInvocationContextHolder contextHolder = new OpenApiInvocationContextHolder();
        DataServiceInvocationApplicationService dataServiceInvocationApplicationService =
                new DataServiceInvocationApplicationService(
                        repository,
                        new StaticDataServiceOperationContextProvider(),
                        new ObjectMapper()
                );
        OpenApiInvocationApplicationService applicationService = new OpenApiInvocationApplicationService(
                contextHolder,
                dataServiceInvocationApplicationService,
                new ObjectMapper()
        );

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
        contextHolder.set(new AuthenticatedOpenApiInvocationContext(
                "req-1",
                OpenApiTestFixtures.TENANT_ID.toString(),
                "partner-app",
                endpoint,
                credentialGrant,
                OpenApiTestFixtures.FIXED_TIME
        ));

        Map<String, Object> payload = applicationService.invoke(new OpenApiInvocationRequest(
                Map.of("departmentId", "1001"),
                "{\"includeDisabled\":false}"
        ));

        assertThat(payload).containsEntry("serviceCode", "employee.query");
        assertThat(payload).containsEntry("openApiCode", "employee-directory");
        assertThat(payload).containsEntry("openApiVersion", "v1");
        assertThat(payload).containsEntry("appCode", "open-api");
    }
}
