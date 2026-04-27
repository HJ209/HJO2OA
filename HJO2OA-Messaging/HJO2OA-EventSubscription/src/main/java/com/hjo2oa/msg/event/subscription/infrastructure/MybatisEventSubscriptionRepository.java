package com.hjo2oa.msg.event.subscription.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.msg.event.subscription.domain.ChannelType;
import com.hjo2oa.msg.event.subscription.domain.DigestMode;
import com.hjo2oa.msg.event.subscription.domain.EventSubscriptionRepository;
import com.hjo2oa.msg.event.subscription.domain.NotificationCategory;
import com.hjo2oa.msg.event.subscription.domain.NotificationPriority;
import com.hjo2oa.msg.event.subscription.domain.QuietWindow;
import com.hjo2oa.msg.event.subscription.domain.SubscriptionPreference;
import com.hjo2oa.msg.event.subscription.domain.SubscriptionRule;
import com.hjo2oa.msg.event.subscription.domain.TargetResolverType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisEventSubscriptionRepository implements EventSubscriptionRepository {

    private static final TypeReference<List<ChannelType>> CHANNEL_LIST_TYPE = new TypeReference<>() {
    };

    private final SubscriptionRuleMapper ruleMapper;
    private final SubscriptionPreferenceMapper preferenceMapper;
    private final ObjectMapper objectMapper;

    public MybatisEventSubscriptionRepository(
            SubscriptionRuleMapper ruleMapper,
            SubscriptionPreferenceMapper preferenceMapper,
            ObjectMapper objectMapper
    ) {
        this.ruleMapper = Objects.requireNonNull(ruleMapper, "ruleMapper must not be null");
        this.preferenceMapper = Objects.requireNonNull(preferenceMapper, "preferenceMapper must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public Optional<SubscriptionRule> findRuleById(UUID ruleId) {
        return Optional.ofNullable(ruleMapper.selectById(ruleId)).map(this::toRule);
    }

    @Override
    public Optional<SubscriptionRule> findRuleByCode(String ruleCode, UUID tenantId) {
        return Optional.ofNullable(ruleMapper.selectOne(Wrappers.<SubscriptionRuleEntity>lambdaQuery()
                .eq(SubscriptionRuleEntity::getRuleCode, ruleCode)
                .eq(SubscriptionRuleEntity::getTenantId, tenantId))).map(this::toRule);
    }

    @Override
    public List<SubscriptionRule> findRules(UUID tenantId) {
        return ruleMapper.selectList(Wrappers.<SubscriptionRuleEntity>lambdaQuery()
                        .eq(tenantId != null, SubscriptionRuleEntity::getTenantId, tenantId)
                        .orderByAsc(SubscriptionRuleEntity::getRuleCode))
                .stream()
                .map(this::toRule)
                .toList();
    }

    @Override
    public SubscriptionRule saveRule(SubscriptionRule rule) {
        SubscriptionRuleEntity existing = ruleMapper.selectById(rule.id());
        SubscriptionRuleEntity entity = toRuleEntity(rule, existing);
        if (existing == null) {
            ruleMapper.insert(entity);
        } else {
            ruleMapper.updateById(entity);
        }
        return toRule(ruleMapper.selectById(rule.id()));
    }

    @Override
    public void deleteRule(UUID ruleId) {
        ruleMapper.deleteById(ruleId);
    }

    @Override
    public Optional<SubscriptionPreference> findPreference(
            UUID personId,
            NotificationCategory category,
            UUID tenantId
    ) {
        return Optional.ofNullable(preferenceMapper.selectOne(Wrappers.<SubscriptionPreferenceEntity>lambdaQuery()
                .eq(SubscriptionPreferenceEntity::getPersonId, personId)
                .eq(SubscriptionPreferenceEntity::getCategory, category.name())
                .eq(SubscriptionPreferenceEntity::getTenantId, tenantId))).map(this::toPreference);
    }

    @Override
    public List<SubscriptionPreference> findPreferences(UUID personId, UUID tenantId) {
        return preferenceMapper.selectList(Wrappers.<SubscriptionPreferenceEntity>lambdaQuery()
                        .eq(SubscriptionPreferenceEntity::getPersonId, personId)
                        .eq(SubscriptionPreferenceEntity::getTenantId, tenantId)
                        .orderByAsc(SubscriptionPreferenceEntity::getCategory))
                .stream()
                .map(this::toPreference)
                .toList();
    }

    @Override
    public SubscriptionPreference savePreference(SubscriptionPreference preference) {
        SubscriptionPreferenceEntity existing = preferenceMapper.selectById(preference.id());
        SubscriptionPreferenceEntity entity = toPreferenceEntity(preference, existing);
        if (existing == null) {
            preferenceMapper.insert(entity);
        } else {
            preferenceMapper.updateById(entity);
        }
        return toPreference(preferenceMapper.selectById(preference.id()));
    }

    private SubscriptionRule toRule(SubscriptionRuleEntity entity) {
        return new SubscriptionRule(
                entity.getId(),
                entity.getRuleCode(),
                entity.getEventTypePattern(),
                NotificationCategory.valueOf(entity.getNotificationCategory()),
                TargetResolverType.valueOf(entity.getTargetResolverType()),
                entity.getTargetResolverConfig(),
                entity.getTemplateCode(),
                entity.getConditionExpr(),
                entity.getPriorityMapping(),
                NotificationPriority.valueOf(entity.getDefaultPriority()),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private SubscriptionPreference toPreference(SubscriptionPreferenceEntity entity) {
        return new SubscriptionPreference(
                entity.getId(),
                entity.getPersonId(),
                NotificationCategory.valueOf(entity.getCategory()),
                readChannels(entity.getAllowedChannels()),
                readQuietWindow(entity.getQuietWindow()),
                DigestMode.valueOf(entity.getDigestMode()),
                Boolean.TRUE.equals(entity.getEscalationOptIn()),
                Boolean.TRUE.equals(entity.getMuteNonWorkingHours()),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private SubscriptionRuleEntity toRuleEntity(SubscriptionRule rule, SubscriptionRuleEntity existing) {
        SubscriptionRuleEntity entity = existing == null ? new SubscriptionRuleEntity() : existing;
        entity.setId(rule.id());
        entity.setRuleCode(rule.ruleCode());
        entity.setEventTypePattern(rule.eventTypePattern());
        entity.setNotificationCategory(rule.notificationCategory().name());
        entity.setTargetResolverType(rule.targetResolverType().name());
        entity.setTargetResolverConfig(rule.targetResolverConfig());
        entity.setTemplateCode(rule.templateCode());
        entity.setConditionExpr(rule.conditionExpr());
        entity.setPriorityMapping(rule.priorityMapping());
        entity.setDefaultPriority(rule.defaultPriority().name());
        entity.setEnabled(rule.enabled());
        entity.setTenantId(rule.tenantId());
        entity.setCreatedAt(rule.createdAt());
        entity.setUpdatedAt(rule.updatedAt());
        return entity;
    }

    private SubscriptionPreferenceEntity toPreferenceEntity(
            SubscriptionPreference preference,
            SubscriptionPreferenceEntity existing
    ) {
        SubscriptionPreferenceEntity entity = existing == null ? new SubscriptionPreferenceEntity() : existing;
        entity.setId(preference.id());
        entity.setPersonId(preference.personId());
        entity.setCategory(preference.category().name());
        entity.setAllowedChannels(writeJson(preference.allowedChannels()));
        entity.setQuietWindow(writeJson(preference.quietWindow()));
        entity.setDigestMode(preference.digestMode().name());
        entity.setEscalationOptIn(preference.escalationOptIn());
        entity.setMuteNonWorkingHours(preference.muteNonWorkingHours());
        entity.setEnabled(preference.enabled());
        entity.setTenantId(preference.tenantId());
        entity.setCreatedAt(preference.createdAt());
        entity.setUpdatedAt(preference.updatedAt());
        return entity;
    }

    private List<ChannelType> readChannels(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, CHANNEL_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid allowedChannels JSON", ex);
        }
    }

    private QuietWindow readQuietWindow(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, QuietWindow.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid quietWindow JSON", ex);
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to write subscription JSON", ex);
        }
    }
}
