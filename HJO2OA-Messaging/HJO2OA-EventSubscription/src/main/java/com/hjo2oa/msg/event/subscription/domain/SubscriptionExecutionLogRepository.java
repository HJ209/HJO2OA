package com.hjo2oa.msg.event.subscription.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionExecutionLogRepository {

    SubscriptionExecutionLog saveIfAbsent(SubscriptionExecutionLog log);

    Optional<SubscriptionExecutionLog> findByEventRuleRecipient(UUID eventId, String ruleCode, String recipientId);

    List<SubscriptionExecutionLog> findByEventId(UUID eventId);
}
