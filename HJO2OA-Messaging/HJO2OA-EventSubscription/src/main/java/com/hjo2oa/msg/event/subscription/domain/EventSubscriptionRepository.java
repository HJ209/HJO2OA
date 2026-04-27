package com.hjo2oa.msg.event.subscription.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventSubscriptionRepository {

    Optional<SubscriptionRule> findRuleById(UUID ruleId);

    Optional<SubscriptionRule> findRuleByCode(String ruleCode, UUID tenantId);

    List<SubscriptionRule> findRules(UUID tenantId);

    SubscriptionRule saveRule(SubscriptionRule rule);

    void deleteRule(UUID ruleId);

    Optional<SubscriptionPreference> findPreference(UUID personId, NotificationCategory category, UUID tenantId);

    List<SubscriptionPreference> findPreferences(UUID personId, UUID tenantId);

    SubscriptionPreference savePreference(SubscriptionPreference preference);
}
