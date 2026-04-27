package com.hjo2oa.msg.event.subscription.infrastructure;

import com.hjo2oa.msg.event.subscription.domain.EventSubscriptionRepository;
import com.hjo2oa.msg.event.subscription.domain.NotificationCategory;
import com.hjo2oa.msg.event.subscription.domain.SubscriptionPreference;
import com.hjo2oa.msg.event.subscription.domain.SubscriptionRule;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryEventSubscriptionRepository implements EventSubscriptionRepository {

    private final Map<UUID, SubscriptionRule> rules = new ConcurrentHashMap<>();
    private final Map<UUID, SubscriptionPreference> preferences = new ConcurrentHashMap<>();

    @Override
    public Optional<SubscriptionRule> findRuleById(UUID ruleId) {
        return Optional.ofNullable(rules.get(ruleId));
    }

    @Override
    public Optional<SubscriptionRule> findRuleByCode(String ruleCode, UUID tenantId) {
        return rules.values().stream()
                .filter(rule -> rule.ruleCode().equals(ruleCode))
                .filter(rule -> Objects.equals(rule.tenantId(), tenantId))
                .findFirst();
    }

    @Override
    public List<SubscriptionRule> findRules(UUID tenantId) {
        return rules.values().stream()
                .filter(rule -> tenantId == null || tenantId.equals(rule.tenantId()))
                .sorted(Comparator.comparing(SubscriptionRule::ruleCode))
                .toList();
    }

    @Override
    public SubscriptionRule saveRule(SubscriptionRule rule) {
        rules.put(rule.id(), rule);
        return rule;
    }

    @Override
    public void deleteRule(UUID ruleId) {
        rules.remove(ruleId);
    }

    @Override
    public Optional<SubscriptionPreference> findPreference(
            UUID personId,
            NotificationCategory category,
            UUID tenantId
    ) {
        return preferences.values().stream()
                .filter(preference -> preference.personId().equals(personId))
                .filter(preference -> preference.category() == category)
                .filter(preference -> Objects.equals(preference.tenantId(), tenantId))
                .findFirst();
    }

    @Override
    public List<SubscriptionPreference> findPreferences(UUID personId, UUID tenantId) {
        return preferences.values().stream()
                .filter(preference -> preference.personId().equals(personId))
                .filter(preference -> Objects.equals(preference.tenantId(), tenantId))
                .sorted(Comparator.comparing(preference -> preference.category().name()))
                .toList();
    }

    @Override
    public SubscriptionPreference savePreference(SubscriptionPreference preference) {
        List<UUID> duplicates = new ArrayList<>();
        preferences.forEach((id, existing) -> {
            if (!id.equals(preference.id())
                    && existing.personId().equals(preference.personId())
                    && existing.category() == preference.category()
                    && Objects.equals(existing.tenantId(), preference.tenantId())) {
                duplicates.add(id);
            }
        });
        duplicates.forEach(preferences::remove);
        preferences.put(preference.id(), preference);
        return preference;
    }
}
