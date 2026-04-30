package com.hjo2oa.infra.timezone.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hjo2oa.infra.timezone.domain.TimezoneScopeType;
import com.hjo2oa.infra.timezone.domain.TimezoneSetting;
import com.hjo2oa.infra.timezone.domain.TimezoneSettingRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisTimezoneSettingRepository implements TimezoneSettingRepository {

    private final TimezoneSettingMapper mapper;

    public MybatisTimezoneSettingRepository(TimezoneSettingMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Optional<TimezoneSetting> findByScope(TimezoneScopeType scopeType, UUID scopeId) {
        Objects.requireNonNull(scopeType, "scopeType must not be null");
        LambdaQueryWrapper<TimezoneSettingEntity> wrapper = baseScopeWrapper(scopeType, scopeId)
                .orderByDesc(TimezoneSettingEntity::getUpdatedAt)
                .orderByDesc(TimezoneSettingEntity::getEffectiveFrom);
        return mapper.selectList(wrapper).stream().map(this::toDomain).findFirst();
    }

    @Override
    public Optional<TimezoneSetting> findDefault() {
        LambdaQueryWrapper<TimezoneSettingEntity> wrapper = currentScopeWrapper(TimezoneScopeType.SYSTEM, null)
                .eq(TimezoneSettingEntity::getDefaultSetting, Boolean.TRUE);
        return mapper.selectList(wrapper).stream().map(this::toDomain).findFirst();
    }

    @Override
    public Optional<TimezoneSetting> findEffectiveForTenant(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        return mapper.selectList(currentScopeWrapper(TimezoneScopeType.TENANT, tenantId)).stream()
                .map(this::toDomain)
                .findFirst();
    }

    @Override
    public Optional<TimezoneSetting> findEffectiveForPerson(UUID personId) {
        Objects.requireNonNull(personId, "personId must not be null");
        return mapper.selectList(currentScopeWrapper(TimezoneScopeType.PERSON, personId)).stream()
                .map(this::toDomain)
                .findFirst();
    }

    @Override
    public List<TimezoneSetting> findAll() {
        LambdaQueryWrapper<TimezoneSettingEntity> wrapper = new LambdaQueryWrapper<TimezoneSettingEntity>()
                .orderByAsc(TimezoneSettingEntity::getScopeType)
                .orderByAsc(TimezoneSettingEntity::getScopeId);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(TimezoneSetting::scopeType)
                        .thenComparing(setting -> setting.scopeId() == null ? "" : setting.scopeId().toString()))
                .toList();
    }

    @Override
    public TimezoneSetting save(TimezoneSetting setting) {
        TimezoneSettingEntity entity = toEntity(setting);
        if (mapper.selectById(setting.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return setting;
    }

    private LambdaQueryWrapper<TimezoneSettingEntity> currentScopeWrapper(TimezoneScopeType scopeType, UUID scopeId) {
        LambdaQueryWrapper<TimezoneSettingEntity> wrapper = baseScopeWrapper(scopeType, scopeId)
                .eq(TimezoneSettingEntity::getActive, Boolean.TRUE);
        LocalDateTime now = toLocalDateTime(Instant.now());
        wrapper.and(query -> query.isNull(TimezoneSettingEntity::getEffectiveFrom)
                .or()
                .le(TimezoneSettingEntity::getEffectiveFrom, now));
        wrapper.orderByDesc(TimezoneSettingEntity::getEffectiveFrom)
                .orderByDesc(TimezoneSettingEntity::getUpdatedAt);
        return wrapper;
    }

    private LambdaQueryWrapper<TimezoneSettingEntity> baseScopeWrapper(TimezoneScopeType scopeType, UUID scopeId) {
        LambdaQueryWrapper<TimezoneSettingEntity> wrapper = new LambdaQueryWrapper<TimezoneSettingEntity>()
                .eq(TimezoneSettingEntity::getScopeType, scopeType.name());
        if (scopeId == null) {
            wrapper.isNull(TimezoneSettingEntity::getScopeId);
        } else {
            wrapper.eq(TimezoneSettingEntity::getScopeId, scopeId);
        }
        return wrapper;
    }

    private TimezoneSetting toDomain(TimezoneSettingEntity entity) {
        return new TimezoneSetting(
                entity.getId(),
                TimezoneScopeType.valueOf(entity.getScopeType()),
                entity.getScopeId(),
                entity.getTimezoneId(),
                Boolean.TRUE.equals(entity.getDefaultSetting()),
                toInstant(entity.getEffectiveFrom()),
                Boolean.TRUE.equals(entity.getActive()),
                entity.getTenantId(),
                toRequiredInstant(entity.getCreatedAt(), "createdAt"),
                toRequiredInstant(entity.getUpdatedAt(), "updatedAt")
        );
    }

    private TimezoneSettingEntity toEntity(TimezoneSetting setting) {
        TimezoneSettingEntity entity = new TimezoneSettingEntity();
        entity.setId(setting.id());
        entity.setScopeType(setting.scopeType().name());
        entity.setScopeId(setting.scopeId());
        entity.setTimezoneId(setting.timezoneId());
        entity.setDefaultSetting(setting.isDefault());
        entity.setEffectiveFrom(toLocalDateTime(setting.effectiveFrom()));
        entity.setActive(setting.active());
        entity.setTenantId(setting.tenantId());
        entity.setCreatedAt(toLocalDateTime(setting.createdAt()));
        entity.setUpdatedAt(toLocalDateTime(setting.updatedAt()));
        return entity;
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(ZoneOffset.UTC);
    }

    private Instant toRequiredInstant(LocalDateTime localDateTime, String fieldName) {
        return Optional.ofNullable(localDateTime)
                .map(this::toInstant)
                .orElseThrow(() -> new IllegalStateException(fieldName + " must not be null"));
    }
}
