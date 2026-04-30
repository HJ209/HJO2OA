package com.hjo2oa.infra.cache.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hjo2oa.infra.cache.domain.CacheBackendType;
import com.hjo2oa.infra.cache.domain.CacheInvalidationRecord;
import com.hjo2oa.infra.cache.domain.CachePolicy;
import com.hjo2oa.infra.cache.domain.CachePolicyRepository;
import com.hjo2oa.infra.cache.domain.EvictionPolicy;
import com.hjo2oa.infra.cache.domain.InvalidationMode;
import com.hjo2oa.infra.cache.domain.InvalidationReasonType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
@ConditionalOnProperty(name = "hjo2oa.cache.type", havingValue = "database")
public class MybatisCachePolicyRepository implements CachePolicyRepository {

    private final CachePolicyMapper cachePolicyMapper;
    private final CacheInvalidationRecordMapper cacheInvalidationRecordMapper;

    public MybatisCachePolicyRepository(
            CachePolicyMapper cachePolicyMapper,
            CacheInvalidationRecordMapper cacheInvalidationRecordMapper
    ) {
        this.cachePolicyMapper = cachePolicyMapper;
        this.cacheInvalidationRecordMapper = cacheInvalidationRecordMapper;
    }

    @Override
    public Optional<CachePolicy> findById(UUID policyId) {
        return Optional.ofNullable(cachePolicyMapper.selectById(policyId)).map(this::toDomain);
    }

    @Override
    public Optional<CachePolicy> findByNamespace(String namespace) {
        LambdaQueryWrapper<CachePolicyEntity> query = new LambdaQueryWrapper<CachePolicyEntity>()
                .eq(CachePolicyEntity::getNamespace, namespace);
        return Optional.ofNullable(cachePolicyMapper.selectOne(query)).map(this::toDomain);
    }

    @Override
    public List<CachePolicy> findAll() {
        LambdaQueryWrapper<CachePolicyEntity> query = new LambdaQueryWrapper<CachePolicyEntity>()
                .orderByAsc(CachePolicyEntity::getNamespace)
                .orderByDesc(CachePolicyEntity::getUpdatedAt);
        return cachePolicyMapper.selectList(query).stream().map(this::toDomain).toList();
    }

    @Override
    public CachePolicy save(CachePolicy cachePolicy) {
        CachePolicyEntity entity = toEntity(cachePolicy);
        if (cachePolicyMapper.selectById(cachePolicy.id()) == null) {
            cachePolicyMapper.insert(entity);
        } else {
            cachePolicyMapper.updateById(entity);
        }
        return cachePolicy;
    }

    @Override
    public CacheInvalidationRecord saveInvalidationRecord(CacheInvalidationRecord invalidationRecord) {
        cacheInvalidationRecordMapper.insert(toEntity(invalidationRecord));
        return invalidationRecord;
    }

    @Override
    public List<CacheInvalidationRecord> findInvalidationRecords(UUID cachePolicyId, int limit) {
        LambdaQueryWrapper<CacheInvalidationRecordEntity> query =
                new LambdaQueryWrapper<CacheInvalidationRecordEntity>()
                        .orderByDesc(CacheInvalidationRecordEntity::getInvalidatedAt);
        if (cachePolicyId != null) {
            query.eq(CacheInvalidationRecordEntity::getCachePolicyId, cachePolicyId);
        }
        query.last("OFFSET 0 ROWS FETCH NEXT " + Math.max(limit, 0) + " ROWS ONLY");
        return cacheInvalidationRecordMapper.selectList(query).stream()
                .map(this::toDomain)
                .toList();
    }

    private CachePolicy toDomain(CachePolicyEntity entity) {
        return new CachePolicy(
                entity.getId(),
                entity.getNamespace(),
                CacheBackendType.valueOf(entity.getBackendType()),
                entity.getTtlSeconds(),
                entity.getMaxCapacity(),
                EvictionPolicy.valueOf(entity.getEvictionPolicy()),
                InvalidationMode.valueOf(entity.getInvalidationMode()),
                Boolean.TRUE.equals(entity.getMetricsEnabled()),
                Boolean.TRUE.equals(entity.getActive()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private CachePolicyEntity toEntity(CachePolicy cachePolicy) {
        CachePolicyEntity entity = new CachePolicyEntity();
        entity.setId(cachePolicy.id());
        entity.setNamespace(cachePolicy.namespace());
        entity.setBackendType(cachePolicy.backendType().name());
        entity.setTtlSeconds(cachePolicy.ttlSeconds());
        entity.setMaxCapacity(cachePolicy.maxCapacity());
        entity.setEvictionPolicy(cachePolicy.evictionPolicy().name());
        entity.setInvalidationMode(cachePolicy.invalidationMode().name());
        entity.setMetricsEnabled(cachePolicy.metricsEnabled());
        entity.setActive(cachePolicy.active());
        entity.setCreatedAt(cachePolicy.createdAt());
        entity.setUpdatedAt(cachePolicy.updatedAt());
        return entity;
    }

    private CacheInvalidationRecordEntity toEntity(CacheInvalidationRecord invalidationRecord) {
        CacheInvalidationRecordEntity entity = new CacheInvalidationRecordEntity();
        entity.setId(invalidationRecord.id());
        entity.setCachePolicyId(invalidationRecord.cachePolicyId());
        entity.setInvalidateKey(invalidationRecord.invalidateKey());
        entity.setReasonType(invalidationRecord.reasonType().name());
        entity.setReasonRef(invalidationRecord.reasonRef());
        entity.setInvalidatedAt(invalidationRecord.invalidatedAt());
        return entity;
    }

    private CacheInvalidationRecord toDomain(CacheInvalidationRecordEntity entity) {
        return new CacheInvalidationRecord(
                entity.getId(),
                entity.getCachePolicyId(),
                entity.getInvalidateKey(),
                InvalidationReasonType.valueOf(entity.getReasonType()),
                entity.getReasonRef(),
                entity.getInvalidatedAt()
        );
    }
}
