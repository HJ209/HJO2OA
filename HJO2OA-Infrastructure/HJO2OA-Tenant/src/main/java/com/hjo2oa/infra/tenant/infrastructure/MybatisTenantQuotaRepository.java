package com.hjo2oa.infra.tenant.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.infra.tenant.domain.QuotaType;
import com.hjo2oa.infra.tenant.domain.TenantQuota;
import com.hjo2oa.infra.tenant.domain.TenantQuotaRepository;
import com.hjo2oa.infra.tenant.infrastructure.persistence.TenantQuotaEntity;
import com.hjo2oa.infra.tenant.infrastructure.persistence.TenantQuotaMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisTenantQuotaRepository implements TenantQuotaRepository {

    private final TenantQuotaMapper tenantQuotaMapper;

    public MybatisTenantQuotaRepository(TenantQuotaMapper tenantQuotaMapper) {
        this.tenantQuotaMapper = tenantQuotaMapper;
    }

    @Override
    public Optional<TenantQuota> findByTenantProfileIdAndQuotaType(UUID tenantProfileId, QuotaType quotaType) {
        TenantQuotaEntity entity = tenantQuotaMapper.selectOne(Wrappers.<TenantQuotaEntity>lambdaQuery()
                .eq(TenantQuotaEntity::getTenantProfileId, tenantProfileId.toString())
                .eq(TenantQuotaEntity::getQuotaType, quotaType.name()));
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public List<TenantQuota> findAllByTenantProfileId(UUID tenantProfileId) {
        return tenantQuotaMapper.selectList(Wrappers.<TenantQuotaEntity>lambdaQuery()
                        .eq(TenantQuotaEntity::getTenantProfileId, tenantProfileId.toString()))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public TenantQuota save(TenantQuota quota) {
        TenantQuotaEntity entity = toEntity(quota);
        if (tenantQuotaMapper.selectById(entity.getId()) == null) {
            tenantQuotaMapper.insert(entity);
        } else {
            tenantQuotaMapper.updateById(entity);
        }
        return quota;
    }

    private TenantQuota toDomain(TenantQuotaEntity entity) {
        return new TenantQuota(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getTenantProfileId()),
                QuotaType.valueOf(entity.getQuotaType()),
                entity.getLimitValue(),
                entity.getUsedValue(),
                entity.getWarningThreshold()
        );
    }

    private TenantQuotaEntity toEntity(TenantQuota quota) {
        TenantQuotaEntity entity = new TenantQuotaEntity();
        entity.setId(quota.id().toString());
        entity.setTenantProfileId(quota.tenantProfileId().toString());
        entity.setQuotaType(quota.quotaType().name());
        entity.setLimitValue(quota.limitValue());
        entity.setUsedValue(quota.usedValue());
        entity.setWarningThreshold(quota.warningThreshold());
        return entity;
    }
}
