package com.hjo2oa.wf.process.definition.infrastructure;

import com.hjo2oa.wf.process.definition.domain.DefinitionStatus;
import com.hjo2oa.wf.process.definition.domain.ProcessDefinition;
import com.hjo2oa.wf.process.definition.domain.ProcessDefinitionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryProcessDefinitionRepository implements ProcessDefinitionRepository {

    private final Map<UUID, ProcessDefinition> definitions = new ConcurrentHashMap<>();

    @Override
    public Optional<ProcessDefinition> findById(UUID definitionId) {
        return Optional.ofNullable(definitions.get(definitionId));
    }

    @Override
    public List<ProcessDefinition> findByCode(UUID tenantId, String code) {
        return definitions.values().stream()
                .filter(definition -> Objects.equals(definition.tenantId(), tenantId))
                .filter(definition -> definition.code().equals(code))
                .sorted(Comparator.comparing(ProcessDefinition::version).reversed())
                .toList();
    }

    @Override
    public Optional<ProcessDefinition> findByCodeAndVersion(UUID tenantId, String code, int version) {
        return definitions.values().stream()
                .filter(definition -> Objects.equals(definition.tenantId(), tenantId))
                .filter(definition -> definition.code().equals(code))
                .filter(definition -> definition.version() == version)
                .findFirst();
    }

    @Override
    public List<ProcessDefinition> findByTenantCategoryAndStatus(
            UUID tenantId,
            String category,
            DefinitionStatus status
    ) {
        return definitions.values().stream()
                .filter(definition -> Objects.equals(definition.tenantId(), tenantId))
                .filter(definition -> category == null || category.isBlank() || category.equals(definition.category()))
                .filter(definition -> status == null || status == definition.status())
                .sorted(Comparator.comparing(ProcessDefinition::code).thenComparing(ProcessDefinition::version))
                .toList();
    }

    @Override
    public ProcessDefinition save(ProcessDefinition definition) {
        definitions.put(definition.id(), definition);
        return definition;
    }

    @Override
    public void delete(UUID definitionId) {
        definitions.remove(definitionId);
    }
}
