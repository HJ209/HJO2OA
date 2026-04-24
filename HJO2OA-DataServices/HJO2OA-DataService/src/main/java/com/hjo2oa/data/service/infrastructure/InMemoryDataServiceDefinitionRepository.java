package com.hjo2oa.data.service.infrastructure;

import com.hjo2oa.data.service.domain.DataServiceDefinition;
import com.hjo2oa.data.service.domain.DataServiceDefinitionRepository;
import javax.sql.DataSource;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryDataServiceDefinitionRepository implements DataServiceDefinitionRepository {

    private final Map<UUID, DataServiceDefinition> definitionsById = new ConcurrentHashMap<>();

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
        List<DataServiceDefinition> matched = definitionsById.values().stream()
                .filter(definition -> definition.tenantId().equals(tenantId))
                .filter(definition -> code == null || code.isBlank() || definition.code().equalsIgnoreCase(code))
                .filter(definition -> keyword == null || keyword.isBlank() || matchesKeyword(definition, keyword))
                .filter(definition -> serviceType == null || definition.serviceType() == serviceType)
                .filter(definition -> status == null || definition.status() == status)
                .sorted(Comparator.comparing(DataServiceDefinition::updatedAt).reversed())
                .toList();
        int fromIndex = Math.min((page - 1) * size, matched.size());
        int toIndex = Math.min(fromIndex + size, matched.size());
        return new SearchResult<>(matched.subList(fromIndex, toIndex), matched.size());
    }

    @Override
    public List<DataServiceDefinition> findAllActiveByTenant(UUID tenantId) {
        return definitionsById.values().stream()
                .filter(definition -> definition.tenantId().equals(tenantId))
                .filter(DataServiceDefinition::active)
                .toList();
    }

    @Override
    public Optional<DataServiceDefinition> findById(UUID serviceId) {
        return Optional.ofNullable(definitionsById.get(serviceId));
    }

    @Override
    public Optional<DataServiceDefinition> findByCode(UUID tenantId, String code) {
        return definitionsById.values().stream()
                .filter(definition -> definition.tenantId().equals(tenantId))
                .filter(definition -> definition.code().equalsIgnoreCase(code))
                .findFirst();
    }

    @Override
    public Optional<DataServiceDefinition> findActiveByCode(UUID tenantId, String code) {
        return findByCode(tenantId, code).filter(DataServiceDefinition::active);
    }

    @Override
    public DataServiceDefinition save(DataServiceDefinition definition) {
        definitionsById.put(definition.serviceId(), definition);
        return definition;
    }

    @Override
    public void delete(UUID serviceId) {
        definitionsById.remove(serviceId);
    }

    private boolean matchesKeyword(DataServiceDefinition definition, String keyword) {
        String normalizedKeyword = keyword.trim().toLowerCase();
        return definition.code().toLowerCase().contains(normalizedKeyword)
                || definition.name().toLowerCase().contains(normalizedKeyword)
                || (definition.description() != null && definition.description().toLowerCase().contains(normalizedKeyword));
    }
}
