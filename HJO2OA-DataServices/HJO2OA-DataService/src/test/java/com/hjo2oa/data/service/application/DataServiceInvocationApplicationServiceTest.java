package com.hjo2oa.data.service.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.data.common.domain.exception.DataServicesException;
import com.hjo2oa.data.service.domain.DataServiceDefinition;
import com.hjo2oa.data.service.domain.DataServiceOperationContext;
import com.hjo2oa.data.service.domain.DataServiceOperationContextProvider;
import com.hjo2oa.data.service.domain.DataServiceViews;
import com.hjo2oa.data.service.domain.ServiceFieldMapping;
import com.hjo2oa.data.service.domain.ServiceParameterDefinition;
import com.hjo2oa.data.service.support.InMemoryDataServiceDefinitionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DataServiceInvocationApplicationServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T02:00:00Z");

    @Test
    void shouldPrepareQueryExecutionWithCacheAndNormalizedParameters() {
        InMemoryDataServiceDefinitionRepository repository = new InMemoryDataServiceDefinitionRepository();
        DataServiceInvocationApplicationService invocationService = invocationService(repository, Set.of("open-api"));
        seedQueryService(repository);

        DataServiceViews.ExecutionPlan plan = invocationService.query(new DataServiceInvocationCommands.InvocationCommand(
                "employee-query",
                "open-api",
                null,
                "idem-query-1",
                Map.of("keyword", "alice")
        ));

        assertThat(plan.cacheEnabled()).isTrue();
        assertThat(plan.cacheKey()).contains("employee-query").contains("alice");
        assertThat(plan.normalizedParameters()).containsEntry("keyword", "alice");
        assertThat(plan.normalizedParameters()).containsKey("page");
        assertThat(plan.outputFields()).containsExactly("name", "maskedPhone");
        assertThat(plan.reportReusable()).isTrue();
        assertThat(plan.openApiReusable()).isTrue();
    }

    @Test
    void shouldRejectAppScopedInvocationWithoutAuthorizedAppCode() {
        InMemoryDataServiceDefinitionRepository repository = new InMemoryDataServiceDefinitionRepository();
        DataServiceInvocationApplicationService invocationService = invocationService(repository, Set.of("report"));
        seedQueryService(repository);

        assertThatThrownBy(() -> invocationService.query(new DataServiceInvocationCommands.InvocationCommand(
                "employee-query",
                "open-api",
                null,
                null,
                Map.of("keyword", "alice")
        )))
                .isInstanceOf(DataServicesException.class)
                .hasMessageContaining("requested app scope");
    }

    @Test
    void shouldRejectQueryInterfaceForCommandService() {
        InMemoryDataServiceDefinitionRepository repository = new InMemoryDataServiceDefinitionRepository();
        DataServiceInvocationApplicationService invocationService = invocationService(repository, Set.of("open-api"));
        seedCommandService(repository);

        assertThatThrownBy(() -> invocationService.query(new DataServiceInvocationCommands.InvocationCommand(
                "employee-submit",
                "open-api",
                null,
                null,
                Map.of("employeeId", "E-1")
        )))
                .isInstanceOf(DataServicesException.class)
                .hasMessageContaining("query interface");
    }

    @Test
    void shouldPrepareSubmitExecutionForCommandService() {
        InMemoryDataServiceDefinitionRepository repository = new InMemoryDataServiceDefinitionRepository();
        DataServiceInvocationApplicationService invocationService = invocationService(repository, Set.of("open-api"));
        seedCommandService(repository);

        DataServiceViews.ExecutionPlan plan = invocationService.submit(new DataServiceInvocationCommands.InvocationCommand(
                "employee-submit",
                "open-api",
                null,
                "idem-submit-1",
                Map.of("employeeId", "E-1", "enabled", true)
        ));

        assertThat(plan.cacheEnabled()).isFalse();
        assertThat(plan.idempotencyKey()).isEqualTo("idem-submit-1");
        assertThat(plan.serviceType()).isEqualTo(DataServiceDefinition.ServiceType.COMMAND);
        assertThat(plan.normalizedParameters()).containsEntry("employeeId", "E-1");
        assertThat(plan.normalizedParameters()).containsEntry("enabled", Boolean.TRUE);
    }

    private DataServiceInvocationApplicationService invocationService(
            InMemoryDataServiceDefinitionRepository repository,
            Set<String> authorizedApps
    ) {
        DataServiceOperationContextProvider contextProvider = () -> new DataServiceOperationContext(
                TENANT_ID,
                "runtime-user",
                Set.of(DataServiceOperationContext.ROLE_DATA_SERVICE_AUDITOR),
                authorizedApps,
                Set.of("subject-1"),
                true
        );
        return new DataServiceInvocationApplicationService(
                repository,
                contextProvider,
                new ObjectMapper(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
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
                        ),
                        new ServiceFieldMapping(
                                UUID.fromString("20000000-0000-0000-0000-000000000002"),
                                serviceId,
                                "phone",
                                "maskedPhone",
                                new ServiceFieldMapping.TransformRule(
                                        ServiceFieldMapping.TransformType.CONSTANT,
                                        null,
                                        null,
                                        "***"
                                ),
                                true,
                                "Masked phone",
                                2
                        )
                )
        ).activate("data-admin", FIXED_TIME);
        repository.save(definition);
    }

    private void seedCommandService(InMemoryDataServiceDefinitionRepository repository) {
        UUID serviceId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        DataServiceDefinition definition = DataServiceDefinition.create(
                serviceId,
                TENANT_ID,
                "employee-submit",
                "Employee Submit",
                DataServiceDefinition.ServiceType.COMMAND,
                DataServiceDefinition.SourceMode.INTERNAL_QUERY,
                DataServiceDefinition.PermissionMode.APP_SCOPED,
                new DataServiceDefinition.PermissionBoundary(List.of("open-api"), List.of(), List.of()),
                DataServiceDefinition.CachePolicy.disabled(),
                "employee.submit",
                null,
                "Employee submit definition",
                "data-admin",
                FIXED_TIME,
                List.of(
                        new ServiceParameterDefinition(
                                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                                serviceId,
                                "employeeId",
                                ServiceParameterDefinition.ParameterType.STRING,
                                true,
                                null,
                                ServiceParameterDefinition.ValidationRule.none(),
                                true,
                                "Employee id",
                                1
                        ),
                        new ServiceParameterDefinition(
                                UUID.fromString("30000000-0000-0000-0000-000000000002"),
                                serviceId,
                                "enabled",
                                ServiceParameterDefinition.ParameterType.BOOLEAN,
                                true,
                                null,
                                ServiceParameterDefinition.ValidationRule.none(),
                                true,
                                "Enable flag",
                                2
                        )
                ),
                List.of()
        ).activate("data-admin", FIXED_TIME);
        repository.save(definition);
    }
}
