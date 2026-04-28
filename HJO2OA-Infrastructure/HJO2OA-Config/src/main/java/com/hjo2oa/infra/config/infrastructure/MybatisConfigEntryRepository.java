package com.hjo2oa.infra.config.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hjo2oa.infra.config.domain.ConfigEntry;
import com.hjo2oa.infra.config.domain.ConfigEntryRepository;
import com.hjo2oa.infra.config.domain.ConfigOverride;
import com.hjo2oa.infra.config.domain.ConfigStatus;
import com.hjo2oa.infra.config.domain.ConfigType;
import com.hjo2oa.infra.config.domain.FeatureRule;
import com.hjo2oa.infra.config.domain.FeatureRuleType;
import com.hjo2oa.infra.config.domain.OverrideScopeType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisConfigEntryRepository implements ConfigEntryRepository {

    private final ConfigEntryMapper configEntryMapper;
    private final ConfigOverrideMapper configOverrideMapper;
    private final FeatureRuleMapper featureRuleMapper;

    public MybatisConfigEntryRepository(
            ConfigEntryMapper configEntryMapper,
            ConfigOverrideMapper configOverrideMapper,
            FeatureRuleMapper featureRuleMapper
    ) {
        this.configEntryMapper = configEntryMapper;
        this.configOverrideMapper = configOverrideMapper;
        this.featureRuleMapper = featureRuleMapper;
    }

    @Override
    public Optional<ConfigEntry> findById(UUID id) {
        return Optional.ofNullable(configEntryMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<ConfigEntry> findByKey(String configKey) {
        LambdaQueryWrapper<ConfigEntryEntity> queryWrapper = new LambdaQueryWrapper<ConfigEntryEntity>()
                .eq(ConfigEntryEntity::getConfigKey, configKey);
        return Optional.ofNullable(configEntryMapper.selectOne(queryWrapper)).map(this::toDomain);
    }

    @Override
    public List<ConfigEntry> findAll() {
        return configEntryMapper.selectList(new LambdaQueryWrapper<ConfigEntryEntity>()
                        .orderByDesc(ConfigEntryEntity::getUpdatedAt)
                        .orderByAsc(ConfigEntryEntity::getConfigKey))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public ConfigEntry save(ConfigEntry configEntry) {
        ConfigEntryEntity entity = toEntity(configEntry);
        if (configEntryMapper.selectById(configEntry.id()) == null) {
            configEntryMapper.insert(entity);
        } else {
            configEntryMapper.updateById(entity);
        }
        configOverrideMapper.delete(new LambdaQueryWrapper<ConfigOverrideEntity>()
                .eq(ConfigOverrideEntity::getConfigEntryId, configEntry.id()));
        featureRuleMapper.delete(new LambdaQueryWrapper<FeatureRuleEntity>()
                .eq(FeatureRuleEntity::getConfigEntryId, configEntry.id()));
        for (ConfigOverride configOverride : configEntry.overrides()) {
            configOverrideMapper.insert(toEntity(configOverride));
        }
        for (FeatureRule featureRule : configEntry.featureRules()) {
            featureRuleMapper.insert(toEntity(featureRule));
        }
        return configEntry;
    }

    private ConfigEntry toDomain(ConfigEntryEntity entity) {
        List<ConfigOverride> overrides = configOverrideMapper.selectList(
                        new LambdaQueryWrapper<ConfigOverrideEntity>()
                                .eq(ConfigOverrideEntity::getConfigEntryId, entity.getId())
                                .orderByAsc(ConfigOverrideEntity::getScopeType)
                                .orderByAsc(ConfigOverrideEntity::getScopeId)
                ).stream()
                .map(this::toDomain)
                .toList();
        List<FeatureRule> featureRules = featureRuleMapper.selectList(
                        new LambdaQueryWrapper<FeatureRuleEntity>()
                                .eq(FeatureRuleEntity::getConfigEntryId, entity.getId())
                                .orderByAsc(FeatureRuleEntity::getSortOrder)
                                .orderByAsc(FeatureRuleEntity::getId)
                ).stream()
                .map(this::toDomain)
                .toList();
        return new ConfigEntry(
                entity.getId(),
                entity.getConfigKey(),
                entity.getName(),
                ConfigType.valueOf(entity.getConfigType()),
                entity.getDefaultValue(),
                entity.getValidationRule(),
                Boolean.TRUE.equals(entity.getMutableAtRuntime()),
                ConfigStatus.valueOf(entity.getStatus()),
                Boolean.TRUE.equals(entity.getTenantAware()),
                toInstant(entity.getCreatedAt()),
                toInstant(entity.getUpdatedAt()),
                overrides,
                featureRules
        );
    }

    private ConfigOverride toDomain(ConfigOverrideEntity entity) {
        return new ConfigOverride(
                entity.getId(),
                entity.getConfigEntryId(),
                OverrideScopeType.valueOf(entity.getScopeType()),
                entity.getScopeId(),
                entity.getOverrideValue(),
                Boolean.TRUE.equals(entity.getActive())
        );
    }

    private FeatureRule toDomain(FeatureRuleEntity entity) {
        return new FeatureRule(
                entity.getId(),
                entity.getConfigEntryId(),
                FeatureRuleType.valueOf(entity.getRuleType()),
                entity.getRuleValue(),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder(),
                Boolean.TRUE.equals(entity.getActive())
        );
    }

    private ConfigEntryEntity toEntity(ConfigEntry configEntry) {
        ConfigEntryEntity entity = new ConfigEntryEntity();
        entity.setId(configEntry.id());
        entity.setConfigKey(configEntry.configKey());
        entity.setName(configEntry.name());
        entity.setConfigType(configEntry.configType().name());
        entity.setDefaultValue(configEntry.defaultValue());
        entity.setValidationRule(configEntry.validationRule());
        entity.setMutableAtRuntime(configEntry.mutableAtRuntime());
        entity.setStatus(configEntry.status().name());
        entity.setTenantAware(configEntry.tenantAware());
        entity.setCreatedAt(toLocalDateTime(configEntry.createdAt()));
        entity.setUpdatedAt(toLocalDateTime(configEntry.updatedAt()));
        return entity;
    }

    private ConfigOverrideEntity toEntity(ConfigOverride configOverride) {
        ConfigOverrideEntity entity = new ConfigOverrideEntity();
        entity.setId(configOverride.id());
        entity.setConfigEntryId(configOverride.configEntryId());
        entity.setScopeType(configOverride.scopeType().name());
        entity.setScopeId(configOverride.scopeId());
        entity.setOverrideValue(configOverride.overrideValue());
        entity.setActive(configOverride.active());
        return entity;
    }

    private FeatureRuleEntity toEntity(FeatureRule featureRule) {
        FeatureRuleEntity entity = new FeatureRuleEntity();
        entity.setId(featureRule.id());
        entity.setConfigEntryId(featureRule.configEntryId());
        entity.setRuleType(featureRule.ruleType().name());
        entity.setRuleValue(featureRule.ruleValue());
        entity.setSortOrder(featureRule.sortOrder());
        entity.setActive(featureRule.active());
        return entity;
    }

    private Instant toInstant(LocalDateTime value) {
        return value.toInstant(ZoneOffset.UTC);
    }

    private LocalDateTime toLocalDateTime(Instant value) {
        return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }
}
