package com.hjo2oa.infra.tenant.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.infra.tenant.domain.IsolationMode;
import com.hjo2oa.infra.tenant.domain.TenantProfile;
import com.hjo2oa.infra.tenant.domain.TenantProfileRepository;
import com.hjo2oa.infra.tenant.domain.TenantStatus;
import com.hjo2oa.infra.tenant.infrastructure.persistence.TenantProfileEntity;
import com.hjo2oa.infra.tenant.infrastructure.persistence.TenantProfileMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisTenantProfileRepository implements TenantProfileRepository {

    private final TenantProfileMapper tenantProfileMapper;

    public MybatisTenantProfileRepository(TenantProfileMapper tenantProfileMapper) {
        this.tenantProfileMapper = tenantProfileMapper;
    }

    @Override
    public Optional<TenantProfile> findByCode(UUID tenantId, String code) {
        TenantProfileEntity entity = tenantProfileMapper.selectOne(Wrappers.<TenantProfileEntity>lambdaQuery()
                .eq(TenantProfileEntity::getTenantCode, code));
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public Optional<TenantProfile> findByTenantId(UUID tenantId) {
        return Optional.ofNullable(tenantProfileMapper.selectById(tenantId.toString())).map(this::toDomain);
    }

    @Override
    public TenantProfile save(TenantProfile profile) {
        TenantProfileEntity entity = toEntity(profile);
        if (tenantProfileMapper.selectById(entity.getId()) == null) {
            tenantProfileMapper.insert(entity);
        } else {
            tenantProfileMapper.updateById(entity);
        }
        return profile;
    }

    @Override
    public List<TenantProfile> findAll() {
        return tenantProfileMapper.selectList(Wrappers.<TenantProfileEntity>lambdaQuery())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<TenantProfile> findAllActive() {
        return tenantProfileMapper.selectList(Wrappers.<TenantProfileEntity>lambdaQuery()
                        .eq(TenantProfileEntity::getStatus, TenantStatus.ACTIVE.name()))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private TenantProfile toDomain(TenantProfileEntity entity) {
        return new TenantProfile(
                UUID.fromString(entity.getId()),
                entity.getTenantCode(),
                entity.getName(),
                TenantStatus.valueOf(entity.getStatus()),
                toIsolationMode(entity.getIsolationMode()),
                entity.getPackageCode(),
                entity.getDefaultLocale(),
                entity.getDefaultTimezone(),
                toUuid(entity.getAdminAccountId()),
                toUuid(entity.getAdminPersonId()),
                Boolean.TRUE.equals(entity.getInitialized()),
                toInstant(entity.getCreatedAt()),
                toInstant(entity.getUpdatedAt())
        );
    }

    private TenantProfileEntity toEntity(TenantProfile profile) {
        TenantProfileEntity entity = new TenantProfileEntity();
        entity.setId(profile.id().toString());
        entity.setTenantCode(profile.tenantCode());
        entity.setName(profile.name());
        entity.setStatus(profile.status().name());
        entity.setIsolationMode(profile.isolationMode().name());
        entity.setPackageCode(profile.packageCode());
        entity.setDefaultLocale(profile.defaultLocale());
        entity.setDefaultTimezone(profile.defaultTimezone());
        entity.setAdminAccountId(toValue(profile.adminAccountId()));
        entity.setAdminPersonId(toValue(profile.adminPersonId()));
        entity.setInitialized(profile.initialized());
        entity.setCreatedAt(toLocalDateTime(profile.createdAt()));
        entity.setUpdatedAt(toLocalDateTime(profile.updatedAt()));
        return entity;
    }

    private UUID toUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }

    private IsolationMode toIsolationMode(String value) {
        if ("SHARED_SCHEMA".equalsIgnoreCase(value)) {
            return IsolationMode.SHARED_DB;
        }
        if ("DEDICATED_SCHEMA".equalsIgnoreCase(value)) {
            return IsolationMode.DEDICATED_DB;
        }
        return IsolationMode.valueOf(value);
    }

    private String toValue(UUID value) {
        return value == null ? null : value.toString();
    }

    private Instant toInstant(LocalDateTime value) {
        return value.toInstant(ZoneOffset.UTC);
    }

    private LocalDateTime toLocalDateTime(Instant value) {
        return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }
}
