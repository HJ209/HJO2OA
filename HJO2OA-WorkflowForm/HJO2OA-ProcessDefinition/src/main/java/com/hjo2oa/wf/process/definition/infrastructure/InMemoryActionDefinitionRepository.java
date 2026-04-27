package com.hjo2oa.wf.process.definition.infrastructure;

import com.hjo2oa.wf.process.definition.domain.ActionCategory;
import com.hjo2oa.wf.process.definition.domain.ActionDefinition;
import com.hjo2oa.wf.process.definition.domain.ActionDefinitionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryActionDefinitionRepository implements ActionDefinitionRepository {

    private final Map<UUID, ActionDefinition> actions = new ConcurrentHashMap<>();

    @Override
    public Optional<ActionDefinition> findById(UUID actionId) {
        return Optional.ofNullable(actions.get(actionId));
    }

    @Override
    public Optional<ActionDefinition> findByTenantAndCode(UUID tenantId, String code) {
        return actions.values().stream()
                .filter(actionDefinition -> Objects.equals(actionDefinition.tenantId(), tenantId))
                .filter(actionDefinition -> actionDefinition.code().equals(code))
                .findFirst();
    }

    @Override
    public List<ActionDefinition> findByTenant(UUID tenantId) {
        return actions.values().stream()
                .filter(actionDefinition -> Objects.equals(actionDefinition.tenantId(), tenantId))
                .sorted(Comparator.comparing(ActionDefinition::category).thenComparing(ActionDefinition::code))
                .toList();
    }

    @Override
    public List<ActionDefinition> findByTenantAndCategory(UUID tenantId, ActionCategory category) {
        return actions.values().stream()
                .filter(actionDefinition -> Objects.equals(actionDefinition.tenantId(), tenantId))
                .filter(actionDefinition -> actionDefinition.category() == category)
                .sorted(Comparator.comparing(ActionDefinition::code))
                .toList();
    }

    @Override
    public ActionDefinition save(ActionDefinition actionDefinition) {
        actions.put(actionDefinition.id(), actionDefinition);
        return actionDefinition;
    }

    @Override
    public void delete(UUID actionId) {
        actions.remove(actionId);
    }
}
