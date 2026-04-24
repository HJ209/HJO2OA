package com.hjo2oa.infra.config.interfaces;

import com.hjo2oa.infra.config.domain.ConfigEntryView;
import com.hjo2oa.infra.config.domain.ConfigOverrideView;
import com.hjo2oa.infra.config.domain.FeatureRuleView;
import com.hjo2oa.infra.config.domain.ResolvedConfigValueView;
import org.springframework.stereotype.Component;

@Component
public class ConfigEntryDtoMapper {

    public ConfigEntryDtos.ConfigEntryResponse toResponse(ConfigEntryView view) {
        return new ConfigEntryDtos.ConfigEntryResponse(
                view.id(),
                view.configKey(),
                view.name(),
                view.configType(),
                view.defaultValue(),
                view.validationRule(),
                view.mutableAtRuntime(),
                view.status(),
                view.tenantAware(),
                view.createdAt(),
                view.updatedAt(),
                view.overrides().stream().map(this::toResponse).toList(),
                view.featureRules().stream().map(this::toResponse).toList()
        );
    }

    public ConfigEntryDtos.ConfigOverrideResponse toResponse(ConfigOverrideView view) {
        return new ConfigEntryDtos.ConfigOverrideResponse(
                view.id(),
                view.configEntryId(),
                view.scopeType(),
                view.scopeId(),
                view.overrideValue(),
                view.active()
        );
    }

    public ConfigEntryDtos.FeatureRuleResponse toResponse(FeatureRuleView view) {
        return new ConfigEntryDtos.FeatureRuleResponse(
                view.id(),
                view.configEntryId(),
                view.ruleType(),
                view.ruleValue(),
                view.sortOrder(),
                view.active()
        );
    }

    public ConfigEntryDtos.ResolvedConfigValueResponse toResponse(ResolvedConfigValueView view) {
        return new ConfigEntryDtos.ResolvedConfigValueResponse(
                view.entryId(),
                view.configKey(),
                view.configType(),
                view.status(),
                view.resolvedValue(),
                view.sourceType(),
                view.overrideId(),
                view.featureRuleId(),
                view.tenantId(),
                view.orgId(),
                view.roleId(),
                view.userId(),
                view.trace()
        );
    }
}
