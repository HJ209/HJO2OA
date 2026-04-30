package com.hjo2oa.msg.event.subscription.application;

import com.hjo2oa.msg.event.subscription.domain.ChannelType;
import com.hjo2oa.msg.event.subscription.domain.DigestMode;
import com.hjo2oa.msg.event.subscription.domain.EventMatchView;
import com.hjo2oa.msg.event.subscription.domain.EventSubscriptionRepository;
import com.hjo2oa.msg.event.subscription.domain.NotificationCategory;
import com.hjo2oa.msg.event.subscription.domain.NotificationPriority;
import com.hjo2oa.msg.event.subscription.domain.SubscriptionPreference;
import com.hjo2oa.msg.event.subscription.domain.SubscriptionPreferenceView;
import com.hjo2oa.msg.event.subscription.domain.SubscriptionRule;
import com.hjo2oa.msg.event.subscription.domain.SubscriptionRuleView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class EventSubscriptionApplicationService {

    private static final Comparator<SubscriptionRule> RULE_ORDER = Comparator
            .comparing(SubscriptionRule::eventTypePattern)
            .thenComparing(SubscriptionRule::ruleCode);

    private final EventSubscriptionRepository repository;
    private final Clock clock;
    @Autowired
    public EventSubscriptionApplicationService(EventSubscriptionRepository repository) {
        this(repository, Clock.systemUTC());
    }
    public EventSubscriptionApplicationService(EventSubscriptionRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public SubscriptionRuleView createRule(EventSubscriptionCommands.SaveRuleCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        repository.findRuleByCode(command.ruleCode(), command.tenantId()).ifPresent(existing -> {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Subscription rule already exists");
        });
        SubscriptionRule rule = SubscriptionRule.create(
                UUID.randomUUID(),
                command.ruleCode(),
                command.eventTypePattern(),
                command.notificationCategory(),
                command.targetResolverType(),
                command.targetResolverConfig(),
                command.templateCode(),
                command.conditionExpr(),
                command.priorityMapping(),
                command.defaultPriority(),
                command.enabled(),
                command.tenantId(),
                now()
        );
        return repository.saveRule(rule).toView();
    }

    public SubscriptionRuleView updateRule(UUID ruleId, EventSubscriptionCommands.UpdateRuleCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        SubscriptionRule rule = loadRequiredRule(ruleId);
        return repository.saveRule(rule.update(
                command.eventTypePattern(),
                command.notificationCategory(),
                command.targetResolverType(),
                command.targetResolverConfig(),
                command.templateCode(),
                command.conditionExpr(),
                command.priorityMapping(),
                command.defaultPriority(),
                command.enabled(),
                now()
        )).toView();
    }

    public SubscriptionRuleView toggleRule(UUID ruleId, boolean enabled) {
        return repository.saveRule(loadRequiredRule(ruleId).toggle(enabled, now())).toView();
    }

    public void deleteRule(UUID ruleId) {
        loadRequiredRule(ruleId);
        repository.deleteRule(ruleId);
    }

    public SubscriptionRuleView getRule(UUID ruleId) {
        return loadRequiredRule(ruleId).toView();
    }

    public List<SubscriptionRuleView> listRules(UUID tenantId) {
        return repository.findRules(tenantId).stream()
                .sorted(RULE_ORDER)
                .map(SubscriptionRule::toView)
                .toList();
    }

    public SubscriptionPreferenceView savePreference(EventSubscriptionCommands.SavePreferenceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        SubscriptionPreference preference = repository
                .findPreference(command.personId(), command.category(), command.tenantId())
                .map(existing -> existing.update(
                        command.allowedChannels(),
                        command.quietWindow(),
                        command.digestMode(),
                        command.escalationOptIn(),
                        command.muteNonWorkingHours(),
                        command.enabled(),
                        now()
                ))
                .orElseGet(() -> SubscriptionPreference.create(
                        UUID.randomUUID(),
                        command.personId(),
                        command.category(),
                        command.allowedChannels(),
                        command.quietWindow(),
                        command.digestMode(),
                        command.escalationOptIn(),
                        command.muteNonWorkingHours(),
                        command.enabled(),
                        command.tenantId(),
                        now()
                ));
        return repository.savePreference(preference).toView();
    }

    public List<SubscriptionPreferenceView> listPreferences(UUID personId, UUID tenantId) {
        return repository.findPreferences(personId, tenantId).stream()
                .sorted(Comparator.comparing(preference -> preference.category().name()))
                .map(SubscriptionPreference::toView)
                .toList();
    }

    public List<EventMatchView> matchEvent(EventSubscriptionCommands.EventMatchQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return repository.findRules(query.tenantId()).stream()
                .filter(SubscriptionRule::enabled)
                .filter(rule -> rule.matchesEventType(query.eventType()))
                .sorted(RULE_ORDER)
                .map(rule -> toMatchView(rule, query))
                .toList();
    }

    public List<EventMatchView> matchEvent(
            String eventType,
            UUID tenantId,
            UUID recipientPersonId,
            NotificationPriority eventPriority,
            LocalTime occurredLocalTime
    ) {
        return matchEvent(new EventSubscriptionCommands.EventMatchQuery(
                null,
                eventType,
                tenantId,
                recipientPersonId,
                eventPriority,
                occurredLocalTime
        ));
    }

    private EventMatchView toMatchView(
            SubscriptionRule rule,
            EventSubscriptionCommands.EventMatchQuery query
    ) {
        SubscriptionPreference preference = query.recipientPersonId() == null
                ? null
                : repository.findPreference(
                        query.recipientPersonId(),
                        rule.notificationCategory(),
                        rule.tenantId()
                ).orElse(null);
        boolean preferenceEnabled = preference == null || preference.enabled();
        List<ChannelType> channels = preference == null ? List.of(ChannelType.INBOX) : preference.allowedChannels();
        DigestMode digestMode = preference == null ? DigestMode.IMMEDIATE : preference.digestMode();
        boolean quietNow = preference != null
                && preference.quietWindow() != null
                && preference.quietWindow().contains(resolveOccurredLocalTime(query));
        boolean escalationAllowed = preference == null || preference.escalationOptIn();
        return new EventMatchView(
                rule.id(),
                rule.ruleCode(),
                query.eventType(),
                rule.notificationCategory(),
                rule.targetResolverType(),
                rule.targetResolverConfig(),
                rule.templateCode(),
                query.eventPriority() == null ? rule.defaultPriority() : query.eventPriority(),
                quietNow || !preferenceEnabled,
                preferenceEnabled ? digestMode : DigestMode.DISABLED,
                preferenceEnabled ? channels : List.of(),
                escalationAllowed
        );
    }

    private LocalTime resolveOccurredLocalTime(EventSubscriptionCommands.EventMatchQuery query) {
        return query.occurredLocalTime() == null ? LocalTime.now(clock) : query.occurredLocalTime();
    }

    private SubscriptionRule loadRequiredRule(UUID ruleId) {
        return repository.findRuleById(ruleId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Subscription rule not found"
                ));
    }

    private Instant now() {
        return clock.instant();
    }
}
