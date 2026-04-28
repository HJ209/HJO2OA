package com.hjo2oa.data.openapi.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hjo2oa.data.openapi.domain.ApiQuotaUsageCounter;
import com.hjo2oa.data.openapi.domain.ApiQuotaUsageCounterRepository;
import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class MybatisPlusApiQuotaUsageCounterRepository implements ApiQuotaUsageCounterRepository {

    private final ApiQuotaUsageCounterMapper mapper;

    public MybatisPlusApiQuotaUsageCounterRepository(ApiQuotaUsageCounterMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ApiQuotaUsageCounter> findByWindow(
            String tenantId,
            String policyId,
            String clientCode,
            Instant windowStartedAt
    ) {
        LambdaQueryWrapper<ApiQuotaUsageCounterEntity> query = new LambdaQueryWrapper<ApiQuotaUsageCounterEntity>()
                .eq(ApiQuotaUsageCounterEntity::getTenantId, tenantId)
                .eq(ApiQuotaUsageCounterEntity::getPolicyId, policyId)
                .eq(ApiQuotaUsageCounterEntity::getClientCode, clientCode)
                .eq(ApiQuotaUsageCounterEntity::getWindowStartedAt, windowStartedAt);
        return mapper.selectList(query).stream().findFirst().map(this::toDomain);
    }

    @Override
    public List<ApiQuotaUsageCounter> findAllByTenant(String tenantId) {
        return mapper.selectList(new LambdaQueryWrapper<ApiQuotaUsageCounterEntity>()
                        .eq(ApiQuotaUsageCounterEntity::getTenantId, tenantId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public ApiQuotaUsageCounter save(ApiQuotaUsageCounter counter) {
        ApiQuotaUsageCounterEntity entity = toEntity(counter);
        if (mapper.selectById(entity.getId()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return counter;
    }

    private ApiQuotaUsageCounter toDomain(ApiQuotaUsageCounterEntity entity) {
        return new ApiQuotaUsageCounter(
                entity.getId(),
                entity.getTenantId(),
                entity.getPolicyId(),
                entity.getApiId(),
                entity.getClientCode(),
                entity.getWindowStartedAt(),
                entity.getUsedCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ApiQuotaUsageCounterEntity toEntity(ApiQuotaUsageCounter counter) {
        ApiQuotaUsageCounterEntity entity = new ApiQuotaUsageCounterEntity();
        entity.setId(counter.counterId());
        entity.setTenantId(counter.tenantId());
        entity.setPolicyId(counter.policyId());
        entity.setApiId(counter.apiId());
        entity.setClientCode(counter.clientCode());
        entity.setWindowStartedAt(counter.windowStartedAt());
        entity.setUsedCount(counter.usedCount());
        entity.setCreatedAt(counter.createdAt());
        entity.setUpdatedAt(counter.updatedAt());
        return entity;
    }
}
