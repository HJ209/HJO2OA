package com.hjo2oa.data.service.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.data.service.domain.DataServiceDefinition;
import com.hjo2oa.data.service.support.InMemoryDataServiceDefinitionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DataServiceReuseApplicationServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T02:30:00Z");

    @Test
    void shouldExposeActivatedDefinitionsForOpenApiAndReportReuse() {
        InMemoryDataServiceDefinitionRepository repository = new InMemoryDataServiceDefinitionRepository();
        repository.save(DataServiceDefinition.create(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                TENANT_ID,
                "query-service",
                "Query Service",
                DataServiceDefinition.ServiceType.QUERY,
                DataServiceDefinition.SourceMode.INTERNAL_QUERY,
                DataServiceDefinition.PermissionMode.PUBLIC_INTERNAL,
                DataServiceDefinition.PermissionBoundary.none(),
                DataServiceDefinition.CachePolicy.disabled(),
                "query.ref",
                null,
                "query",
                "data-admin",
                FIXED_TIME,
                List.of(),
                List.of()
        ).activate("data-admin", FIXED_TIME));
        repository.save(DataServiceDefinition.create(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                TENANT_ID,
                "command-service",
                "Command Service",
                DataServiceDefinition.ServiceType.COMMAND,
                DataServiceDefinition.SourceMode.INTERNAL_QUERY,
                DataServiceDefinition.PermissionMode.PUBLIC_INTERNAL,
                DataServiceDefinition.PermissionBoundary.none(),
                DataServiceDefinition.CachePolicy.disabled(),
                "command.ref",
                null,
                "command",
                "data-admin",
                FIXED_TIME,
                List.of(),
                List.of()
        ).activate("data-admin", FIXED_TIME));
        DataServiceReuseApplicationService reuseApplicationService = new DataServiceReuseApplicationService(repository);

        assertThat(reuseApplicationService.listActivatedForOpenApi(TENANT_ID))
                .extracting(com.hjo2oa.data.service.domain.DataServiceViews.ReusableView::code)
                .containsExactly("command-service", "query-service");
        assertThat(reuseApplicationService.listActivatedForReport(TENANT_ID))
                .singleElement()
                .satisfies(view -> assertThat(view.code()).isEqualTo("query-service"));
        assertThat(reuseApplicationService.findActivated(TENANT_ID, "query-service")).isPresent();
    }
}
