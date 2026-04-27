package com.hjo2oa.wf.process.definition.application;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.wf.process.definition.domain.ActionDefinition;
import com.hjo2oa.wf.process.definition.domain.ActionDefinitionRepository;
import com.hjo2oa.wf.process.definition.domain.ActionDefinitionView;
import com.hjo2oa.wf.process.definition.domain.DefinitionStatus;
import com.hjo2oa.wf.process.definition.domain.ProcessDefinition;
import com.hjo2oa.wf.process.definition.domain.ProcessDefinitionRepository;
import com.hjo2oa.wf.process.definition.domain.ProcessDefinitionView;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProcessDefinitionApplicationService {

    private static final Comparator<ProcessDefinition> DEFINITION_ORDER = Comparator
            .comparing(ProcessDefinition::code)
            .thenComparing(ProcessDefinition::version)
            .reversed();

    private static final Comparator<ActionDefinition> ACTION_ORDER = Comparator
            .comparing(ActionDefinition::category)
            .thenComparing(ActionDefinition::code);

    private final ProcessDefinitionRepository definitionRepository;
    private final ActionDefinitionRepository actionRepository;
    private final Clock clock;

    public ProcessDefinitionApplicationService(
            ProcessDefinitionRepository definitionRepository,
            ActionDefinitionRepository actionRepository
    ) {
        this(definitionRepository, actionRepository, Clock.systemUTC());
    }

    public ProcessDefinitionApplicationService(
            ProcessDefinitionRepository definitionRepository,
            ActionDefinitionRepository actionRepository,
            Clock clock
    ) {
        this.definitionRepository = Objects.requireNonNull(definitionRepository, "definitionRepository must not be null");
        this.actionRepository = Objects.requireNonNull(actionRepository, "actionRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ProcessDefinitionView createDefinition(ProcessDefinitionCommands.SaveDefinitionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ensureCodeAvailable(command.tenantId(), command.code(), nextVersion(command.tenantId(), command.code()));
        Instant now = now();
        ProcessDefinition definition = ProcessDefinition.create(
                UUID.randomUUID(),
                command.code(),
                command.name(),
                command.category(),
                nextVersion(command.tenantId(), command.code()),
                command.formMetadataId(),
                command.startNodeId(),
                command.endNodeId(),
                command.nodes(),
                command.routes(),
                command.tenantId(),
                now
        );
        return definitionRepository.save(definition).toView();
    }

    public ProcessDefinitionView updateDefinition(ProcessDefinitionCommands.SaveDefinitionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ProcessDefinition definition = loadRequiredDefinition(command.definitionId());
        ProcessDefinition updated = definition.updateDraft(
                command.name(),
                command.category(),
                command.formMetadataId(),
                command.startNodeId(),
                command.endNodeId(),
                command.nodes(),
                command.routes(),
                now()
        );
        return definitionRepository.save(updated).toView();
    }

    public ProcessDefinitionView createNextVersion(UUID definitionId) {
        ProcessDefinition source = loadRequiredDefinition(definitionId);
        int nextVersion = nextVersion(source.tenantId(), source.code());
        ensureCodeAvailable(source.tenantId(), source.code(), nextVersion);
        return definitionRepository.save(source.copyAsDraft(UUID.randomUUID(), nextVersion, now())).toView();
    }

    public ProcessDefinitionView publishDefinition(ProcessDefinitionCommands.PublishDefinitionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ProcessDefinition target = loadRequiredDefinition(command.definitionId());
        ProcessDefinition active = definitionRepository.save(target.publish(command.publishedBy(), now()));
        definitionRepository.findByCode(active.tenantId(), active.code()).stream()
                .filter(definition -> definition.status() == DefinitionStatus.ACTIVE)
                .filter(definition -> !definition.id().equals(active.id()))
                .forEach(definition -> definitionRepository.save(definition.deprecate(now())));
        return active.toView();
    }

    public ProcessDefinitionView deprecateDefinition(UUID definitionId) {
        ProcessDefinition definition = loadRequiredDefinition(definitionId);
        return definitionRepository.save(definition.deprecate(now())).toView();
    }

    public void deleteDefinition(UUID definitionId) {
        ProcessDefinition definition = loadRequiredDefinition(definitionId);
        definition.ensureDraft();
        definitionRepository.delete(definition.id());
    }

    public ProcessDefinitionView getDefinition(UUID definitionId) {
        return loadRequiredDefinition(definitionId).toView();
    }

    public List<ProcessDefinitionView> queryDefinitions(ProcessDefinitionCommands.DefinitionQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        if (query.code() != null && !query.code().isBlank()) {
            return definitionRepository.findByCode(query.tenantId(), query.code()).stream()
                    .sorted(DEFINITION_ORDER)
                    .map(ProcessDefinition::toView)
                    .toList();
        }
        return definitionRepository.findByTenantCategoryAndStatus(query.tenantId(), query.category(), query.status()).stream()
                .sorted(DEFINITION_ORDER)
                .map(ProcessDefinition::toView)
                .toList();
    }

    public ActionDefinitionView createAction(ProcessDefinitionCommands.SaveActionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ensureActionCodeAvailable(command.tenantId(), command.code(), null);
        ActionDefinition actionDefinition = ActionDefinition.create(
                UUID.randomUUID(),
                command.code(),
                command.name(),
                command.category(),
                command.routeTarget(),
                command.requireOpinion(),
                command.requireTarget(),
                command.uiConfig(),
                command.tenantId(),
                now()
        );
        return actionRepository.save(actionDefinition).toView();
    }

    public ActionDefinitionView updateAction(ProcessDefinitionCommands.SaveActionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ActionDefinition actionDefinition = loadRequiredAction(command.actionId());
        ensureActionCodeAvailable(command.tenantId(), command.code(), actionDefinition.id());
        ActionDefinition updated = actionDefinition.update(
                command.name(),
                command.category(),
                command.routeTarget(),
                command.requireOpinion(),
                command.requireTarget(),
                command.uiConfig(),
                now()
        );
        return actionRepository.save(updated).toView();
    }

    public void deleteAction(UUID actionId) {
        loadRequiredAction(actionId);
        actionRepository.delete(actionId);
    }

    public ActionDefinitionView getAction(UUID actionId) {
        return loadRequiredAction(actionId).toView();
    }

    public List<ActionDefinitionView> queryActions(ProcessDefinitionCommands.ActionQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        List<ActionDefinition> actions = query.category() == null
                ? actionRepository.findByTenant(query.tenantId())
                : actionRepository.findByTenantAndCategory(query.tenantId(), query.category());
        return actions.stream()
                .sorted(ACTION_ORDER)
                .map(ActionDefinition::toView)
                .toList();
    }

    private ProcessDefinition loadRequiredDefinition(UUID definitionId) {
        return definitionRepository.findById(definitionId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Process definition not found"
                ));
    }

    private ActionDefinition loadRequiredAction(UUID actionId) {
        return actionRepository.findById(actionId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Action definition not found"
                ));
    }

    private int nextVersion(UUID tenantId, String code) {
        return definitionRepository.findByCode(tenantId, code).stream()
                .mapToInt(ProcessDefinition::version)
                .max()
                .orElse(0) + 1;
    }

    private void ensureCodeAvailable(UUID tenantId, String code, int version) {
        if (definitionRepository.findByCodeAndVersion(tenantId, code, version).isPresent()) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Process definition version already exists");
        }
    }

    private void ensureActionCodeAvailable(UUID tenantId, String code, UUID currentActionId) {
        actionRepository.findByTenantAndCode(tenantId, code)
                .filter(actionDefinition -> !actionDefinition.id().equals(currentActionId))
                .ifPresent(actionDefinition -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Action definition already exists");
                });
    }

    private Instant now() {
        return clock.instant();
    }
}
