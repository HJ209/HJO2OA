package com.hjo2oa.data.service.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public interface DataServiceDefinitionRepository {

    SearchResult<DataServiceDefinition> search(
            UUID tenantId,
            String code,
            String keyword,
            DataServiceDefinition.ServiceType serviceType,
            DataServiceDefinition.Status status,
            int page,
            int size
    );

    List<DataServiceDefinition> findAllActiveByTenant(UUID tenantId);

    java.util.Optional<DataServiceDefinition> findById(UUID serviceId);

    java.util.Optional<DataServiceDefinition> findByCode(UUID tenantId, String code);

    java.util.Optional<DataServiceDefinition> findActiveByCode(UUID tenantId, String code);

    DataServiceDefinition save(DataServiceDefinition definition);

    void delete(UUID serviceId);

    record SearchResult<T>(List<T> items, long total) {

        public SearchResult {
            items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
            if (total < 0) {
                throw new IllegalArgumentException("total must not be negative");
            }
        }
    }
}
