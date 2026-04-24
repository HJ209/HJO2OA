package com.hjo2oa.data.openapi;

import com.hjo2oa.data.service.domain.DataServiceDefinition;
import com.hjo2oa.data.service.domain.ServiceParameterDefinition;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OpenApiTestFixtures {

    public static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final Instant FIXED_TIME = Instant.parse("2026-04-24T02:00:00Z");

    private OpenApiTestFixtures() {
    }

    public static DataServiceDefinition activeDataService(String code) {
        UUID serviceId = UUID.randomUUID();
        DataServiceDefinition draft = DataServiceDefinition.create(
                serviceId,
                TENANT_ID,
                code,
                "Employee Query",
                DataServiceDefinition.ServiceType.QUERY,
                DataServiceDefinition.SourceMode.INTERNAL_QUERY,
                DataServiceDefinition.PermissionMode.APP_SCOPED,
                new DataServiceDefinition.PermissionBoundary(List.of("open-api"), List.of(), List.of()),
                DataServiceDefinition.CachePolicy.disabled(),
                "internal://employee/query",
                null,
                "test definition",
                "creator",
                FIXED_TIME,
                List.of(
                        new ServiceParameterDefinition(
                                UUID.randomUUID(),
                                serviceId,
                                "departmentId",
                                ServiceParameterDefinition.ParameterType.STRING,
                                false,
                                null,
                                ServiceParameterDefinition.ValidationRule.none(),
                                true,
                                "department filter",
                                0
                        ),
                        new ServiceParameterDefinition(
                                UUID.randomUUID(),
                                serviceId,
                                "includeDisabled",
                                ServiceParameterDefinition.ParameterType.BOOLEAN,
                                false,
                                "false",
                                ServiceParameterDefinition.ValidationRule.none(),
                                true,
                                "include disabled records",
                                1
                        )
                ),
                List.of()
        );
        return draft.activate("creator", FIXED_TIME);
    }
}
