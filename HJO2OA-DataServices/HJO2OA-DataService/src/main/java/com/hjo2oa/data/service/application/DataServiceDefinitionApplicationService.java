package com.hjo2oa.data.service.application;

import com.hjo2oa.data.common.application.audit.DataAuditLog;
import com.hjo2oa.data.common.domain.exception.DataServicesErrorCode;
import com.hjo2oa.data.common.domain.exception.DataServicesException;
import com.hjo2oa.data.service.domain.DataServiceActivatedEvent;
import com.hjo2oa.data.service.domain.DataServiceDefinition;
import com.hjo2oa.data.service.domain.DataServiceDefinitionRepository;
import com.hjo2oa.data.service.domain.DataServiceOperationContext;
import com.hjo2oa.data.service.domain.DataServiceOperationContextProvider;
import com.hjo2oa.data.service.domain.DataServiceViews;
import com.hjo2oa.data.service.domain.ServiceFieldMapping;
import com.hjo2oa.data.service.domain.ServiceParameterDefinition;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataServiceDefinitionApplicationService {

    private static final Comparator<DataServiceViews.ParameterView> PARAMETER_ORDER =
            Comparator.comparingInt(DataServiceViews.ParameterView::sortOrder)
                    .thenComparing(DataServiceViews.ParameterView::paramCode);
    private static final Comparator<DataServiceViews.FieldMappingView> FIELD_MAPPING_ORDER =
            Comparator.comparingInt(DataServiceViews.FieldMappingView::sortOrder)
                    .thenComparing(DataServiceViews.FieldMappingView::targetField)
                    .thenComparing(DataServiceViews.FieldMappingView::sourceField);

    private final DataServiceDefinitionRepository repository;
    private final DataServiceOperationContextProvider contextProvider;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;
    @Autowired
    public DataServiceDefinitionApplicationService(
            DataServiceDefinitionRepository repository,
            DataServiceOperationContextProvider contextProvider,
            DomainEventPublisher domainEventPublisher
    ) {
        this(repository, contextProvider, domainEventPublisher, Clock.systemUTC());
    }
    public DataServiceDefinitionApplicationService(
            DataServiceDefinitionRepository repository,
            DataServiceOperationContextProvider contextProvider,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.contextProvider = Objects.requireNonNull(contextProvider, "contextProvider must not be null");
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Optional<DataServiceViews.DetailView> current(UUID serviceId) {
        DataServiceOperationContext context = currentViewContext();
        return repository.findById(serviceId)
                .filter(definition -> definition.tenantId().equals(context.tenantId()))
                .map(DataServiceDefinition::toDetailView);
    }

    public DataServiceDefinitionRepository.SearchResult<DataServiceViews.SummaryView> list(
            DataServiceDefinitionCommands.ListQuery query
    ) {
        DataServiceOperationContext context = currentViewContext();
        DataServiceDefinitionRepository.SearchResult<DataServiceDefinition> result = repository.search(
                context.tenantId(),
                query.code(),
                query.keyword(),
                query.serviceType(),
                query.status(),
                query.page(),
                query.size()
        );
        List<DataServiceViews.SummaryView> items = result.items().stream()
                .map(definition -> definition.toSummaryView(0, 0, 0))
                .toList();
        return new DataServiceDefinitionRepository.SearchResult<>(items, result.total());
    }

    @Transactional
    @DataAuditLog(module = "data-service", action = "create-service-definition", targetType = "DataServiceDefinition")
    public DataServiceViews.DetailView create(DataServiceDefinitionCommands.CreateCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        DataServiceOperationContext context = currentManageContext();
        ensureCodeUniqueness(context.tenantId(), command.code(), command.serviceId());
        Instant now = now();
        DataServiceDefinition definition = DataServiceDefinition.create(
                command.serviceId(),
                context.tenantId(),
                command.code(),
                command.name(),
                command.serviceType(),
                command.sourceMode(),
                command.permissionMode(),
                command.permissionBoundary(),
                command.cachePolicy(),
                command.sourceRef(),
                command.connectorId(),
                command.description(),
                context.operatorId(),
                now,
                toParameterDefinitions(command.serviceId(), command.parameters(), List.of()),
                toFieldMappings(command.serviceId(), command.fieldMappings(), List.of())
        );
        return repository.save(definition).toDetailView();
    }

    @Transactional
    @DataAuditLog(module = "data-service", action = "update-service-definition", targetType = "DataServiceDefinition")
    public DataServiceViews.DetailView update(DataServiceDefinitionCommands.UpdateCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        DataServiceOperationContext context = currentManageContext();
        DataServiceDefinition existing = loadTenantScopedDefinition(command.serviceId(), context.tenantId());
        ensureCodeUniqueness(context.tenantId(), command.code(), command.serviceId());
        Instant now = now();
        List<ServiceParameterDefinition> parameters = command.parameters() == null
                ? existing.parameters()
                : toParameterDefinitions(command.serviceId(), command.parameters(), existing.parameters());
        List<ServiceFieldMapping> fieldMappings = command.fieldMappings() == null
                ? existing.fieldMappings()
                : toFieldMappings(command.serviceId(), command.fieldMappings(), existing.fieldMappings());
        DataServiceDefinition updated = existing.update(
                command.code(),
                command.name(),
                command.serviceType(),
                command.sourceMode(),
                command.permissionMode(),
                command.permissionBoundary(),
                command.cachePolicy(),
                command.sourceRef(),
                command.connectorId(),
                command.description(),
                context.operatorId(),
                now,
                parameters,
                fieldMappings
        );
        return repository.save(updated).toDetailView();
    }

    @Transactional
    @DataAuditLog(module = "data-service", action = "activate-service-definition", targetType = "DataServiceDefinition")
    public DataServiceViews.DetailView activate(UUID serviceId, String idempotencyKey) {
        DataServiceOperationContext context = currentManageContext();
        DataServiceDefinition existing = loadTenantScopedDefinition(serviceId, context.tenantId());
        DataServiceDefinition activated = existing.activate(context.operatorId(), now());
        if (activated == existing) {
            return existing.toDetailView();
        }
        DataServiceDefinition saved = repository.save(activated);
        domainEventPublisher.publish(DataServiceActivatedEvent.from(saved));
        return saved.toDetailView();
    }

    @Transactional
    @DataAuditLog(module = "data-service", action = "disable-service-definition", targetType = "DataServiceDefinition")
    public DataServiceViews.DetailView disable(UUID serviceId, String idempotencyKey) {
        DataServiceOperationContext context = currentManageContext();
        DataServiceDefinition existing = loadTenantScopedDefinition(serviceId, context.tenantId());
        DataServiceDefinition disabled = existing.disable(context.operatorId(), now());
        return repository.save(disabled).toDetailView();
    }

    @Transactional
    @DataAuditLog(module = "data-service", action = "delete-service-definition", targetType = "DataServiceDefinition")
    public void delete(UUID serviceId) {
        DataServiceOperationContext context = currentManageContext();
        DataServiceDefinition existing = loadTenantScopedDefinition(serviceId, context.tenantId());
        if (existing.status() == DataServiceDefinition.Status.ACTIVE) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_CONFLICT,
                    "Active data service must be disabled before deletion"
            );
        }
        repository.delete(serviceId);
    }

    public List<DataServiceViews.ParameterView> listParameters(
            UUID serviceId,
            Boolean required,
            ServiceParameterDefinition.ParameterType paramType,
            Boolean enabled
    ) {
        DataServiceOperationContext context = currentViewContext();
        return loadTenantScopedDefinition(serviceId, context.tenantId()).parameters().stream()
                .map(ServiceParameterDefinition::toView)
                .filter(view -> required == null || view.required() == required)
                .filter(view -> paramType == null || view.paramType() == paramType)
                .filter(view -> enabled == null || view.enabled() == enabled)
                .sorted(PARAMETER_ORDER)
                .toList();
    }

    @Transactional
    @DataAuditLog(module = "data-service", action = "upsert-service-parameter", targetType = "ServiceParameterDefinition")
    public DataServiceViews.ParameterView upsertParameter(
            UUID serviceId,
            DataServiceDefinitionCommands.ParameterCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        DataServiceOperationContext context = currentManageContext();
        DataServiceDefinition definition = loadTenantScopedDefinition(serviceId, context.tenantId());
        ServiceParameterDefinition existing = definition.parameters().stream()
                .filter(parameter -> parameter.paramCode().equalsIgnoreCase(command.paramCode()))
                .findFirst()
                .orElse(null);
        UUID parameterId = command.parameterId() != null
                ? command.parameterId()
                : existing == null ? UUID.randomUUID() : existing.parameterId();
        ServiceParameterDefinition parameter = new ServiceParameterDefinition(
                parameterId,
                serviceId,
                command.paramCode(),
                command.paramType(),
                command.required(),
                command.defaultValue(),
                command.validationRule(),
                command.enabled(),
                command.description(),
                command.sortOrder()
        );
        DataServiceDefinition saved = repository.save(definition.upsertParameter(parameter, context.operatorId(), now()));
        return saved.parameters().stream()
                .filter(candidate -> candidate.paramCode().equalsIgnoreCase(command.paramCode()))
                .findFirst()
                .map(ServiceParameterDefinition::toView)
                .orElseThrow(() -> new DataServicesException(
                        DataServicesErrorCode.DATA_COMMON_INTERNAL_ERROR,
                        "Parameter was not saved correctly"
                ));
    }

    public List<DataServiceViews.FieldMappingView> listFieldMappings(
            UUID serviceId,
            String sourceField,
            String targetField,
            Boolean masked
    ) {
        DataServiceOperationContext context = currentViewContext();
        return loadTenantScopedDefinition(serviceId, context.tenantId()).fieldMappings().stream()
                .map(ServiceFieldMapping::toView)
                .filter(view -> sourceField == null || view.sourceField().equalsIgnoreCase(sourceField))
                .filter(view -> targetField == null || view.targetField().equalsIgnoreCase(targetField))
                .filter(view -> masked == null || view.masked() == masked)
                .sorted(FIELD_MAPPING_ORDER)
                .toList();
    }

    @Transactional
    @DataAuditLog(module = "data-service", action = "upsert-field-mapping", targetType = "ServiceFieldMapping")
    public DataServiceViews.FieldMappingView upsertFieldMapping(
            UUID serviceId,
            DataServiceDefinitionCommands.FieldMappingCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        DataServiceOperationContext context = currentManageContext();
        DataServiceDefinition definition = loadTenantScopedDefinition(serviceId, context.tenantId());
        ServiceFieldMapping existing = definition.fieldMappings().stream()
                .filter(mapping -> command.mappingId() != null && mapping.mappingId().equals(command.mappingId()))
                .findFirst()
                .orElseGet(() -> definition.fieldMappings().stream()
                        .filter(mapping -> mapping.sourceField().equalsIgnoreCase(command.sourceField()))
                        .filter(mapping -> mapping.targetField().equalsIgnoreCase(command.targetField()))
                        .findFirst()
                        .orElse(null));
        UUID mappingId = command.mappingId() != null
                ? command.mappingId()
                : existing == null ? UUID.randomUUID() : existing.mappingId();
        ServiceFieldMapping mapping = new ServiceFieldMapping(
                mappingId,
                serviceId,
                command.sourceField(),
                command.targetField(),
                command.transformRule(),
                command.masked(),
                command.description(),
                command.sortOrder()
        );
        DataServiceDefinition saved = repository.save(definition.upsertFieldMapping(mapping, context.operatorId(), now()));
        return saved.fieldMappings().stream()
                .filter(candidate -> candidate.mappingId().equals(mappingId))
                .findFirst()
                .map(ServiceFieldMapping::toView)
                .orElseThrow(() -> new DataServicesException(
                        DataServicesErrorCode.DATA_COMMON_INTERNAL_ERROR,
                        "Field mapping was not saved correctly"
                ));
    }

    private DataServiceOperationContext currentViewContext() {
        DataServiceOperationContext context = Objects.requireNonNull(
                contextProvider.currentContext(),
                "contextProvider.currentContext() must not return null"
        );
        if (!context.canViewDefinitions()) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_FORBIDDEN,
                    "Current operator is not allowed to view data service definitions"
            );
        }
        return context;
    }

    private DataServiceOperationContext currentManageContext() {
        DataServiceOperationContext context = Objects.requireNonNull(
                contextProvider.currentContext(),
                "contextProvider.currentContext() must not return null"
        );
        if (!context.canManageDefinitions()) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_FORBIDDEN,
                    "Current operator is not allowed to manage data service definitions"
            );
        }
        return context;
    }

    private DataServiceDefinition loadTenantScopedDefinition(UUID serviceId, UUID tenantId) {
        return repository.findById(serviceId)
                .filter(definition -> definition.tenantId().equals(tenantId))
                .orElseThrow(() -> new DataServicesException(
                        DataServicesErrorCode.DATA_SERVICE_NOT_FOUND,
                        "Data service definition not found"
                ));
    }

    private void ensureCodeUniqueness(UUID tenantId, String code, UUID currentServiceId) {
        repository.findByCode(tenantId, code)
                .filter(existing -> !existing.serviceId().equals(currentServiceId))
                .ifPresent(existing -> {
                    throw new DataServicesException(
                            DataServicesErrorCode.DATA_COMMON_CONFLICT,
                            "Data service code already exists"
                    );
                });
    }

    private List<ServiceParameterDefinition> toParameterDefinitions(
            UUID serviceId,
            List<DataServiceDefinitionCommands.ParameterCommand> commands,
            List<ServiceParameterDefinition> existingDefinitions
    ) {
        if (commands == null || commands.isEmpty()) {
            return List.of();
        }
        List<ServiceParameterDefinition> definitions = new ArrayList<>(commands.size());
        for (DataServiceDefinitionCommands.ParameterCommand command : commands) {
            ServiceParameterDefinition existing = existingDefinitions.stream()
                    .filter(candidate -> candidate.paramCode().equalsIgnoreCase(command.paramCode()))
                    .findFirst()
                    .orElse(null);
            UUID parameterId = command.parameterId() != null
                    ? command.parameterId()
                    : existing == null ? UUID.randomUUID() : existing.parameterId();
            definitions.add(new ServiceParameterDefinition(
                    parameterId,
                    serviceId,
                    command.paramCode(),
                    command.paramType(),
                    command.required(),
                    command.defaultValue(),
                    command.validationRule(),
                    command.enabled(),
                    command.description(),
                    command.sortOrder()
            ));
        }
        return List.copyOf(definitions);
    }

    private List<ServiceFieldMapping> toFieldMappings(
            UUID serviceId,
            List<DataServiceDefinitionCommands.FieldMappingCommand> commands,
            List<ServiceFieldMapping> existingMappings
    ) {
        if (commands == null || commands.isEmpty()) {
            return List.of();
        }
        List<ServiceFieldMapping> mappings = new ArrayList<>(commands.size());
        for (DataServiceDefinitionCommands.FieldMappingCommand command : commands) {
            ServiceFieldMapping existing = existingMappings.stream()
                    .filter(candidate -> candidate.sourceField().equalsIgnoreCase(command.sourceField()))
                    .filter(candidate -> candidate.targetField().equalsIgnoreCase(command.targetField()))
                    .findFirst()
                    .orElse(null);
            UUID mappingId = command.mappingId() != null
                    ? command.mappingId()
                    : existing == null ? UUID.randomUUID() : existing.mappingId();
            mappings.add(new ServiceFieldMapping(
                    mappingId,
                    serviceId,
                    command.sourceField(),
                    command.targetField(),
                    command.transformRule(),
                    command.masked(),
                    command.description(),
                    command.sortOrder()
            ));
        }
        return List.copyOf(mappings);
    }

    private Instant now() {
        return clock.instant();
    }
}
