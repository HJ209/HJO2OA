package com.hjo2oa.data.openapi.infrastructure;

import com.hjo2oa.data.openapi.domain.OpenApiEndpoint;
import com.hjo2oa.data.openapi.domain.OpenApiEndpointRepository;
import com.hjo2oa.data.openapi.domain.OpenApiHttpMethod;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryOpenApiEndpointRepository implements OpenApiEndpointRepository {

    private final Map<String, OpenApiEndpoint> endpointsById = new ConcurrentHashMap<>();

    @Override
    public Optional<OpenApiEndpoint> findByApiId(String apiId) {
        return Optional.ofNullable(endpointsById.get(apiId));
    }

    @Override
    public Optional<OpenApiEndpoint> findByCodeAndVersion(String tenantId, String code, String version) {
        return endpointsById.values().stream()
                .filter(endpoint -> endpoint.tenantId().equals(tenantId))
                .filter(endpoint -> endpoint.code().equals(code))
                .filter(endpoint -> endpoint.version().equals(version))
                .findFirst();
    }

    @Override
    public Optional<OpenApiEndpoint> findByPathMethodAndVersion(
            String tenantId,
            String path,
            OpenApiHttpMethod httpMethod,
            String version
    ) {
        return endpointsById.values().stream()
                .filter(endpoint -> endpoint.tenantId().equals(tenantId))
                .filter(endpoint -> endpoint.path().equals(path))
                .filter(endpoint -> endpoint.httpMethod() == httpMethod)
                .filter(endpoint -> endpoint.version().equals(version))
                .findFirst();
    }

    @Override
    public List<OpenApiEndpoint> findAllByTenant(String tenantId) {
        return endpointsById.values().stream()
                .filter(endpoint -> endpoint.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public OpenApiEndpoint save(OpenApiEndpoint endpoint) {
        endpointsById.put(endpoint.apiId(), endpoint);
        return endpoint;
    }

    @Override
    public void delete(String apiId) {
        endpointsById.remove(apiId);
    }
}
