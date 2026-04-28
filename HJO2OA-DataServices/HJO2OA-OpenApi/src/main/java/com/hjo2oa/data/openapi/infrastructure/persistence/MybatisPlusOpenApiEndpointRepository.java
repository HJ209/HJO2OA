package com.hjo2oa.data.openapi.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hjo2oa.data.openapi.domain.ApiCredentialGrant;
import com.hjo2oa.data.openapi.domain.ApiRateLimitPolicy;
import com.hjo2oa.data.openapi.domain.OpenApiEndpoint;
import com.hjo2oa.data.openapi.domain.OpenApiEndpointRepository;
import com.hjo2oa.data.openapi.domain.OpenApiHttpMethod;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class MybatisPlusOpenApiEndpointRepository implements OpenApiEndpointRepository {

    private final OpenApiEndpointMapper endpointMapper;
    private final ApiCredentialGrantMapper credentialGrantMapper;
    private final ApiRateLimitPolicyMapper rateLimitPolicyMapper;

    public MybatisPlusOpenApiEndpointRepository(
            OpenApiEndpointMapper endpointMapper,
            ApiCredentialGrantMapper credentialGrantMapper,
            ApiRateLimitPolicyMapper rateLimitPolicyMapper
    ) {
        this.endpointMapper = endpointMapper;
        this.credentialGrantMapper = credentialGrantMapper;
        this.rateLimitPolicyMapper = rateLimitPolicyMapper;
    }

    @Override
    public java.util.Optional<OpenApiEndpoint> findByApiId(String apiId) {
        return java.util.Optional.ofNullable(endpointMapper.selectById(apiId)).map(this::toDomain);
    }

    @Override
    public java.util.Optional<OpenApiEndpoint> findByCodeAndVersion(String tenantId, String code, String version) {
        LambdaQueryWrapper<OpenApiEndpointEntity> query = new LambdaQueryWrapper<OpenApiEndpointEntity>()
                .eq(OpenApiEndpointEntity::getTenantId, tenantId)
                .eq(OpenApiEndpointEntity::getCode, code)
                .eq(OpenApiEndpointEntity::getVersion, version);
        return endpointMapper.selectList(query).stream().findFirst().map(this::toDomain);
    }

    @Override
    public java.util.Optional<OpenApiEndpoint> findByPathMethodAndVersion(
            String tenantId,
            String path,
            OpenApiHttpMethod httpMethod,
            String version
    ) {
        LambdaQueryWrapper<OpenApiEndpointEntity> query = new LambdaQueryWrapper<OpenApiEndpointEntity>()
                .eq(OpenApiEndpointEntity::getTenantId, tenantId)
                .eq(OpenApiEndpointEntity::getPath, path)
                .eq(OpenApiEndpointEntity::getHttpMethod, httpMethod)
                .eq(OpenApiEndpointEntity::getVersion, version);
        return endpointMapper.selectList(query).stream().findFirst().map(this::toDomain);
    }

    @Override
    public List<OpenApiEndpoint> findAllByTenant(String tenantId) {
        LambdaQueryWrapper<OpenApiEndpointEntity> query = new LambdaQueryWrapper<OpenApiEndpointEntity>()
                .eq(OpenApiEndpointEntity::getTenantId, tenantId);
        return endpointMapper.selectList(query).stream().map(this::toDomain).toList();
    }

    @Override
    public OpenApiEndpoint save(OpenApiEndpoint endpoint) {
        OpenApiEndpointEntity entity = toEntity(endpoint);
        if (endpointMapper.selectById(entity.getId()) == null) {
            endpointMapper.insert(entity);
        } else {
            endpointMapper.updateById(entity);
        }

        credentialGrantMapper.delete(new LambdaUpdateWrapper<ApiCredentialGrantEntity>()
                .eq(ApiCredentialGrantEntity::getOpenApiId, endpoint.apiId()));
        endpoint.credentialGrants().stream()
                .map(this::toEntity)
                .forEach(credentialGrantMapper::insert);

        rateLimitPolicyMapper.delete(new LambdaUpdateWrapper<ApiRateLimitPolicyEntity>()
                .eq(ApiRateLimitPolicyEntity::getOpenApiId, endpoint.apiId()));
        endpoint.rateLimitPolicies().stream()
                .map(this::toEntity)
                .forEach(rateLimitPolicyMapper::insert);
        return endpoint;
    }

    @Override
    public void delete(String apiId) {
        credentialGrantMapper.delete(new LambdaUpdateWrapper<ApiCredentialGrantEntity>()
                .eq(ApiCredentialGrantEntity::getOpenApiId, apiId));
        rateLimitPolicyMapper.delete(new LambdaUpdateWrapper<ApiRateLimitPolicyEntity>()
                .eq(ApiRateLimitPolicyEntity::getOpenApiId, apiId));
        endpointMapper.deleteById(apiId);
    }

    private OpenApiEndpoint toDomain(OpenApiEndpointEntity entity) {
        List<ApiCredentialGrant> credentials = credentialGrantMapper.selectList(new LambdaQueryWrapper<ApiCredentialGrantEntity>()
                        .eq(ApiCredentialGrantEntity::getOpenApiId, entity.getId()))
                .stream()
                .map(this::toDomain)
                .toList();
        List<ApiRateLimitPolicy> policies = rateLimitPolicyMapper.selectList(new LambdaQueryWrapper<ApiRateLimitPolicyEntity>()
                        .eq(ApiRateLimitPolicyEntity::getOpenApiId, entity.getId()))
                .stream()
                .map(this::toDomain)
                .toList();
        return new OpenApiEndpoint(
                entity.getId(),
                entity.getTenantId(),
                entity.getCode(),
                entity.getName(),
                entity.getDataServiceId(),
                entity.getDataServiceCode(),
                entity.getDataServiceName(),
                entity.getPath(),
                entity.getHttpMethod(),
                entity.getVersion(),
                entity.getAuthType(),
                entity.getCompatibilityNotes(),
                entity.getStatus(),
                entity.getPublishedAt(),
                entity.getDeprecatedAt(),
                entity.getSunsetAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                credentials,
                policies
        );
    }

    private ApiCredentialGrant toDomain(ApiCredentialGrantEntity entity) {
        return new ApiCredentialGrant(
                entity.getId(),
                entity.getOpenApiId(),
                entity.getTenantId(),
                entity.getClientCode(),
                entity.getSecretRef(),
                entity.getScopes() == null || entity.getScopes().isBlank()
                        ? List.of()
                        : Arrays.stream(entity.getScopes().split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList(),
                entity.getExpiresAt(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ApiRateLimitPolicy toDomain(ApiRateLimitPolicyEntity entity) {
        return new ApiRateLimitPolicy(
                entity.getId(),
                entity.getOpenApiId(),
                entity.getTenantId(),
                entity.getPolicyCode(),
                entity.getClientCode(),
                entity.getPolicyType(),
                entity.getWindowValue(),
                entity.getWindowUnit(),
                entity.getThreshold(),
                entity.getStatus(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private OpenApiEndpointEntity toEntity(OpenApiEndpoint endpoint) {
        OpenApiEndpointEntity entity = new OpenApiEndpointEntity();
        entity.setId(endpoint.apiId());
        entity.setTenantId(endpoint.tenantId());
        entity.setCode(endpoint.code());
        entity.setName(endpoint.name());
        entity.setDataServiceId(endpoint.dataServiceId());
        entity.setDataServiceCode(endpoint.dataServiceCode());
        entity.setDataServiceName(endpoint.dataServiceName());
        entity.setPath(endpoint.path());
        entity.setHttpMethod(endpoint.httpMethod());
        entity.setVersion(endpoint.version());
        entity.setAuthType(endpoint.authType());
        entity.setCompatibilityNotes(endpoint.compatibilityNotes());
        entity.setStatus(endpoint.status());
        entity.setPublishedAt(endpoint.publishedAt());
        entity.setDeprecatedAt(endpoint.deprecatedAt());
        entity.setSunsetAt(endpoint.sunsetAt());
        entity.setCreatedAt(endpoint.createdAt());
        entity.setUpdatedAt(endpoint.updatedAt());
        return entity;
    }

    private ApiCredentialGrantEntity toEntity(ApiCredentialGrant credentialGrant) {
        ApiCredentialGrantEntity entity = new ApiCredentialGrantEntity();
        entity.setId(credentialGrant.grantId());
        entity.setOpenApiId(credentialGrant.openApiId());
        entity.setTenantId(credentialGrant.tenantId());
        entity.setClientCode(credentialGrant.clientCode());
        entity.setSecretRef(credentialGrant.secretRef());
        entity.setScopes(String.join(",", credentialGrant.scopes()));
        entity.setExpiresAt(credentialGrant.expiresAt());
        entity.setStatus(credentialGrant.status());
        entity.setCreatedAt(credentialGrant.createdAt());
        entity.setUpdatedAt(credentialGrant.updatedAt());
        return entity;
    }

    private ApiRateLimitPolicyEntity toEntity(ApiRateLimitPolicy policy) {
        ApiRateLimitPolicyEntity entity = new ApiRateLimitPolicyEntity();
        entity.setId(policy.policyId());
        entity.setOpenApiId(policy.openApiId());
        entity.setTenantId(policy.tenantId());
        entity.setPolicyCode(policy.policyCode());
        entity.setClientCode(policy.clientCode());
        entity.setPolicyType(policy.policyType());
        entity.setWindowValue(policy.windowValue());
        entity.setWindowUnit(policy.windowUnit());
        entity.setThreshold(policy.threshold());
        entity.setStatus(policy.status());
        entity.setDescription(policy.description());
        entity.setCreatedAt(policy.createdAt());
        entity.setUpdatedAt(policy.updatedAt());
        return entity;
    }
}
