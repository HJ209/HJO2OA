package com.hjo2oa.msg.event.subscription.infrastructure;

import com.hjo2oa.msg.event.subscription.domain.SubscriptionExecutionLog;
import com.hjo2oa.msg.event.subscription.domain.SubscriptionExecutionLogRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySubscriptionExecutionLogRepository implements SubscriptionExecutionLogRepository {

    private final Map<String, SubscriptionExecutionLog> logs = new ConcurrentHashMap<>();

    @Override
    public SubscriptionExecutionLog saveIfAbsent(SubscriptionExecutionLog log) {
        return logs.computeIfAbsent(key(log.eventId(), log.ruleCode(), log.recipientId()), ignored -> log);
    }

    @Override
    public Optional<SubscriptionExecutionLog> findByEventRuleRecipient(UUID eventId, String ruleCode, String recipientId) {
        return Optional.ofNullable(logs.get(key(eventId, ruleCode, recipientId)));
    }

    @Override
    public List<SubscriptionExecutionLog> findByEventId(UUID eventId) {
        return logs.values().stream()
                .filter(log -> log.eventId().equals(eventId))
                .toList();
    }

    private String key(UUID eventId, String ruleCode, String recipientId) {
        return eventId + "\u0000" + ruleCode + "\u0000" + recipientId;
    }
}
