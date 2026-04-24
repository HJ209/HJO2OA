package com.hjo2oa.data.service.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.data.common.domain.exception.DataServicesException;
import com.hjo2oa.data.service.domain.DataServiceActivatedEvent;
import com.hjo2oa.data.service.domain.DataServiceDefinition;
import com.hjo2oa.data.service.domain.DataServiceOperationContext;
import com.hjo2oa.data.service.domain.DataServiceOperationContextProvider;
import com.hjo2oa.data.service.domain.DataServiceViews;
import com.hjo2oa.data.service.domain.ServiceFieldMapping;
import com.hjo2oa.data.service.domain.ServiceParameterDefinition;
import com.hjo2oa.data.service.support.InMemoryDataServiceDefinitionRepository;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DataServiceDefinitionApplicationServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T01:30:00Z");

    @Test
    void shouldCreateActivateAndPublishActivatedEvent() {
        InMemoryDataServiceDefinitionRepository repository = new InMemoryDataServiceDefinitionRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        DataServiceDefinitionApplicationService applicationService = applicationService(repository, publishedEvents);
        UUID serviceId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        DataServiceViews.DetailView created = applicationService.create(sampleCreateCommand(serviceId));
        DataServiceViews.DetailView activated = applicationService.activate(serviceId, "idem-activate-1");

        assertThat(created.status()).isEqualTo(DataServiceDefinition.Status.DRAFT);
        assertThat(created.parameters()).hasSize(2);
        assertThat(activated.status()).isEqualTo(DataServiceDefinition.Status.ACTIVE);
        assertThat(activated.cachePolicy().enabled()).isTrue();
        assertThat(publishedEvents).singleElement().isInstanceOf(DataServiceActivatedEvent.class);
        DataServiceActivatedEvent event = (DataServiceActivatedEvent) publishedEvents.get(0);
        assertThat(event.eventType()).isEqualTo(DataServiceActivatedEvent.EVENT_TYPE);
        assertThat(event.serviceId()).isEqualTo(serviceId);
        assertThat(event.code()).isEqualTo("todo-query");
    }

    @Test
    void shouldRejectDuplicateServiceCodeWithinSameTenant() {
        DataServiceDefinitionApplicationService applicationService = applicationService(
                new InMemoryDataServiceDefinitionRepository(),
                new ArrayList<>()
        );
        applicationService.create(sampleCreateCommand(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")));

        assertThatThrownBy(() -> applicationService.create(sampleCreateCommand(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        )))
                .isInstanceOf(DataServicesException.class)
                .hasMessageContaining("Data service code already exists");
    }

    @Test
    void shouldUpsertParameterAndFieldMapping() {
        InMemoryDataServiceDefinitionRepository repository = new InMemoryDataServiceDefinitionRepository();
        DataServiceDefinitionApplicationService applicationService = applicationService(repository, new ArrayList<>());
        UUID serviceId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        applicationService.create(sampleCreateCommand(serviceId));

        DataServiceViews.ParameterView parameter = applicationService.upsertParameter(
                serviceId,
                new DataServiceDefinitionCommands.ParameterCommand(
                        null,
                        "priority",
                        ServiceParameterDefinition.ParameterType.STRING,
                        false,
                        "NORMAL",
                        new ServiceParameterDefinition.ValidationRule(
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of("LOW", "NORMAL", "HIGH"),
                                null
                        ),
                        true,
                        "Priority filter",
                        3
                )
        );
        DataServiceViews.FieldMappingView fieldMapping = applicationService.upsertFieldMapping(
                serviceId,
                new DataServiceDefinitionCommands.FieldMappingCommand(
                        UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                        "priority",
                        "priorityLabel",
                        new ServiceFieldMapping.TransformRule(
                                ServiceFieldMapping.TransformType.UPPERCASE,
                                null,
                                null,
                                null
                        ),
                        false,
                        "Priority label mapping",
                        3
                )
        );

        assertThat(parameter.paramCode()).isEqualTo("priority");
        assertThat(applicationService.listParameters(serviceId, null, null, null)).hasSize(3);
        assertThat(fieldMapping.targetField()).isEqualTo("priorityLabel");
        assertThat(applicationService.listFieldMappings(serviceId, null, null, null)).hasSize(3);
    }

    @Test
    void shouldRequireActiveServiceToBeDisabledBeforeDelete() {
        InMemoryDataServiceDefinitionRepository repository = new InMemoryDataServiceDefinitionRepository();
        DataServiceDefinitionApplicationService applicationService = applicationService(repository, new ArrayList<>());
        UUID serviceId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        applicationService.create(sampleCreateCommand(serviceId));
        applicationService.activate(serviceId, "idem-activate-1");

        assertThatThrownBy(() -> applicationService.delete(serviceId))
                .isInstanceOf(DataServicesException.class)
                .hasMessageContaining("disabled before deletion");

        applicationService.disable(serviceId, "idem-disable-1");
        applicationService.delete(serviceId);

        assertThat(applicationService.current(serviceId)).isEmpty();
    }

    private DataServiceDefinitionApplicationService applicationService(
            InMemoryDataServiceDefinitionRepository repository,
            List<DomainEvent> publishedEvents
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
                Set.of("subject-1", "subject-2"),
                true
        );
        return new DataServiceDefinitionApplicationService(
                repository,
                contextProvider,
                publishedEvents::add,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    private DataServiceDefinitionCommands.CreateCommand sampleCreateCommand(UUID serviceId) {
        return new DataServiceDefinitionCommands.CreateCommand(
                serviceId,
                "todo-query",
                "Todo Query",
                DataServiceDefinition.ServiceType.QUERY,
                DataServiceDefinition.SourceMode.INTERNAL_QUERY,
                DataServiceDefinition.PermissionMode.PUBLIC_INTERNAL,
                DataServiceDefinition.PermissionBoundary.none(),
                new DataServiceDefinition.CachePolicy(
                        true,
                        300L,
                        "{tenantId}:{serviceCode}:{param.keyword}",
                        DataServiceDefinition.CacheScope.TENANT,
                        false,
                        List.of("process.updated")
                ),
                "todo-center.pending",
                null,
                "Todo query definition",
                List.of(
                        new DataServiceDefinitionCommands.ParameterCommand(
                                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                                "keyword",
                                ServiceParameterDefinition.ParameterType.STRING,
                                true,
                                null,
                                new ServiceParameterDefinition.ValidationRule(1, 64, null, null, null, List.of(), null),
                                true,
                                "Keyword filter",
                                1
                        ),
                        new DataServiceDefinitionCommands.ParameterCommand(
                                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                                "page",
                                ServiceParameterDefinition.ParameterType.PAGEABLE,
                                false,
                                "{\"page\":1,\"size\":20}",
                                new ServiceParameterDefinition.ValidationRule(null, null, null, null, null, List.of(), 100),
                                true,
                                "Page request",
                                2
                        )
                ),
                List.of(
                        new DataServiceDefinitionCommands.FieldMappingCommand(
                                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                                "todoTitle",
                                "title",
                                new ServiceFieldMapping.TransformRule(
                                        ServiceFieldMapping.TransformType.TRIM,
                                        null,
                                        null,
                                        null
                                ),
                                false,
                                "Title field",
                                1
                        ),
                        new DataServiceDefinitionCommands.FieldMappingCommand(
                                UUID.fromString("20000000-0000-0000-0000-000000000002"),
                                "assigneeName",
                                "assignee",
                                ServiceFieldMapping.TransformRule.none(),
                                true,
                                "Assignee field",
                                2
                        )
                )
        );
    }
}
