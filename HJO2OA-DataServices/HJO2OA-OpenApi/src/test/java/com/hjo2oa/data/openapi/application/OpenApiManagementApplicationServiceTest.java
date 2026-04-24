package com.hjo2oa.data.openapi.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.data.openapi.OpenApiTestFixtures;
import com.hjo2oa.data.openapi.domain.ApiInvocationAuditLog;
import com.hjo2oa.data.openapi.domain.ApiInvocationOutcome;
import com.hjo2oa.data.openapi.domain.OpenApiAuthType;
import com.hjo2oa.data.openapi.domain.OpenApiOperatorContext;
import com.hjo2oa.data.openapi.domain.OpenApiOperatorPermission;
import com.hjo2oa.data.openapi.domain.OpenApiStatus;
import com.hjo2oa.data.openapi.infrastructure.InMemoryApiInvocationAuditLogRepository;
import com.hjo2oa.data.openapi.infrastructure.InMemoryApiQuotaUsageCounterRepository;
import com.hjo2oa.data.openapi.infrastructure.InMemoryOpenApiEndpointRepository;
import com.hjo2oa.data.openapi.infrastructure.StaticOpenApiOperatorContextProvider;
import com.hjo2oa.data.service.domain.DataServiceDefinition;
import com.hjo2oa.data.service.domain.DataServiceDefinitionRepository;
import com.hjo2oa.data.service.infrastructure.InMemoryDataServiceDefinitionRepository;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenApiManagementApplicationServiceTest {

    @Test
    void shouldUpsertPublishAndDeprecateEndpointWithDataServiceReuse() {
        Fixture fixture = new Fixture();

        var draft = fixture.applicationService.upsertEndpoint(
                "employee-directory",
                "v1",
                "Employee Directory",
                "employee.query",
                "/api/open/employees",
                com.hjo2oa.data.openapi.domain.OpenApiHttpMethod.GET,
                OpenApiAuthType.APP_KEY,
                "initial public release"
        );
        assertThat(draft.status()).isEqualTo(OpenApiStatus.DRAFT);
        assertThat(draft.dataServiceCode()).isEqualTo("employee.query");

        fixture.applicationService.upsertCredential(
                "employee-directory",
                "v1",
                "partner-app",
                "secret-001",
                List.of("employee.read"),
                OpenApiTestFixtures.FIXED_TIME.plusSeconds(3600)
        );

        var published = fixture.applicationService.publishEndpoint("employee-directory", "v1");
        assertThat(published.status()).isEqualTo(OpenApiStatus.ACTIVE);

        var deprecated = fixture.applicationService.deprecateEndpoint(
                "employee-directory",
                "v1",
                OpenApiTestFixtures.FIXED_TIME.plusSeconds(86400)
        );
        assertThat(deprecated.status()).isEqualTo(OpenApiStatus.DEPRECATED);
        assertThat(deprecated.versions()).hasSize(1);
        assertThat(fixture.events).extracting(DomainEvent::eventType)
                .containsExactly("data.api.published", "data.api.deprecated");
    }

    @Test
    void shouldReviewAuditLog() {
        Fixture fixture = new Fixture();
        ApiInvocationAuditLog log = ApiInvocationAuditLog.create(
                "req-1",
                OpenApiTestFixtures.TENANT_ID.toString(),
                "api-1",
                "employee-directory",
                "v1",
                "/api/open/employees",
                com.hjo2oa.data.openapi.domain.OpenApiHttpMethod.GET,
                "partner-app",
                OpenApiAuthType.APP_KEY,
                ApiInvocationOutcome.ERROR,
                500,
                "INTERNAL_ERROR",
                18,
                "digest",
                "127.0.0.1",
                OpenApiTestFixtures.FIXED_TIME
        );
        fixture.auditLogRepository.save(log);

        ApiInvocationAuditLog reviewed = fixture.applicationService.reviewAuditLog(
                log.logId(),
                true,
                "confirmed runtime issue",
                "needs rollback"
        );

        assertThat(reviewed.abnormalFlag()).isTrue();
        assertThat(reviewed.reviewConclusion()).isEqualTo("confirmed runtime issue");
        assertThat(reviewed.note()).isEqualTo("needs rollback");
        assertThat(reviewed.reviewedBy()).isEqualTo("open-api-admin");
    }

    @Test
    void shouldRejectEndpointWhenReusableDataServiceMissing() {
        InMemoryDataServiceDefinitionRepository repository = new InMemoryDataServiceDefinitionRepository();
        OpenApiManagementApplicationService applicationService = new OpenApiManagementApplicationService(
                new InMemoryOpenApiEndpointRepository(),
                new InMemoryApiInvocationAuditLogRepository(),
                        new InMemoryApiQuotaUsageCounterRepository(),
                        repository,
                        new StaticOpenApiOperatorContextProvider(new OpenApiOperatorContext(
                                OpenApiTestFixtures.TENANT_ID.toString(),
                                "open-api-admin",
                                java.util.EnumSet.allOf(OpenApiOperatorPermission.class)
                        )),
                        event -> {
                        },
                        Clock.fixed(OpenApiTestFixtures.FIXED_TIME, ZoneOffset.UTC)
                );

        assertThatThrownBy(() -> applicationService.upsertEndpoint(
                "employee-directory",
                "v1",
                "Employee Directory",
                "missing.service",
                "/api/open/employees",
                com.hjo2oa.data.openapi.domain.OpenApiHttpMethod.GET,
                OpenApiAuthType.APP_KEY,
                null
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Active DataServiceDefinition not found");
    }

    private static final class Fixture {

        private final InMemoryApiInvocationAuditLogRepository auditLogRepository = new InMemoryApiInvocationAuditLogRepository();
        private final List<DomainEvent> events = new ArrayList<>();
        private final OpenApiManagementApplicationService applicationService;

        private Fixture() {
            DataServiceDefinitionRepository dataServiceRepository = new InMemoryDataServiceDefinitionRepository();
            dataServiceRepository.save(OpenApiTestFixtures.activeDataService("employee.query"));
            this.applicationService = new OpenApiManagementApplicationService(
                    new InMemoryOpenApiEndpointRepository(),
                    auditLogRepository,
                    new InMemoryApiQuotaUsageCounterRepository(),
                    dataServiceRepository,
                    new StaticOpenApiOperatorContextProvider(new OpenApiOperatorContext(
                            OpenApiTestFixtures.TENANT_ID.toString(),
                            "open-api-admin",
                            java.util.EnumSet.allOf(OpenApiOperatorPermission.class)
                    )),
                    events::add,
                    Clock.fixed(OpenApiTestFixtures.FIXED_TIME, ZoneOffset.UTC)
            );
        }
    }
}
