package com.hjo2oa.data.openapi.domain;

import java.util.List;
import java.util.Optional;

public interface OpenApiEndpointRepository {

    Optional<OpenApiEndpoint> findByApiId(String apiId);

    Optional<OpenApiEndpoint> findByCodeAndVersion(String tenantId, String code, String version);

    Optional<OpenApiEndpoint> findByPathMethodAndVersion(
            String tenantId,
            String path,
            OpenApiHttpMethod httpMethod,
            String version
    );

    List<OpenApiEndpoint> findAllByTenant(String tenantId);

    OpenApiEndpoint save(OpenApiEndpoint endpoint);

    void delete(String apiId);
}
