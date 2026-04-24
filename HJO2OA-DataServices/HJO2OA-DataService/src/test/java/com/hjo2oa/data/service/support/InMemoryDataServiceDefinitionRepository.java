package com.hjo2oa.data.service.support;

import com.hjo2oa.data.service.domain.DataServiceDefinition;
import com.hjo2oa.data.service.domain.DataServiceDefinitionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDataServiceDefinitionRepository implements DataServiceDefinitionRepository {

    private final Map<UUID, DataServiceDefinition> definitions = new ConcurrentHashMap<>();

    @Override
    public SearchResult<DataServiceDefinition> search(
            UUID tenantId,
            String code,
            String keyword,
            DataServiceDefinition.ServiceType serviceType,
            DataServiceDefinition.Status status,
            int page,
            int size
    ) {
        List<DataServiceDefinition> filtered = definitions.values().stream()
                .filter(definition -> definition.tenantId().equals(tenantId))
                .filter(definition -> code == null || definition.code().contains(code))
                .filter(definition -> keyword == null
                        || definition.code().contains(keyword)
                        || definition.name().contains(keyword))
                .filter(definition -> serviceType == null || definition.serviceType() == serviceType)
                .filter(definition -> status == null || definition.status() == status)
                .sorted(Comparator.comparing(DataServiceDefinition::updatedAt).reversed()
                        .thenComparing(DataServiceDefinition::code))
                .toList();
        int fromIndex = Math.max(0, (page - 1) * size);
        if (fromIndex >= filtered.size()) {
            return new SearchResult<>(List.of(), filtered.size());
        }
        int toIndex = Math.min(filtered.size(), fromIndex + size);
        return new SearchResult<>(filtered.subList(fromIndex, toIndex), filtered.size());
    }

    @Override
    public List<DataServiceDefinition> findAllActiveByTenant(UUID tenantId) {
        return definitions.values().stream()
                .filter(definition -> definition.tenantId().equals(tenantId))
                .filter(DataServiceDefinition::active)
                .sorted(Comparator.comparing(DataServiceDefinition::code))
                .toList();
    }

    @Override
    public Optional<DataServiceDefinition> findById(UUID serviceId) {
        return Optional.ofNullable(definitions.get(serviceId));
    }

    @Override
    public Optional<DataServiceDefinition> findByCode(UUID tenantId, String code) {
        return definitions.values().stream()
                .filter(definition -> definition.tenantId().equals(tenantId))
                .filter(definition -> definition.code().equals(code))
                .findFirst();
    }

    @Override
    public Optional<DataServiceDefinition> findActiveByCode(UUID tenantId, String code) {
        return definitions.values().stream()
                .filter(definition -> definition.tenantId().equals(tenantId))
                .filter(definition -> definition.code().equals(code))
                .filter(DataServiceDefinition::active)
                .findFirst();
    }

    @Override
    public DataServiceDefinition save(DataServiceDefinition definition) {
        definitions.put(definition.serviceId(), definition);
        return definition;
    }

    @Override
    public void delete(UUID serviceId) {
        definitions.remove(serviceId);
    }
}
