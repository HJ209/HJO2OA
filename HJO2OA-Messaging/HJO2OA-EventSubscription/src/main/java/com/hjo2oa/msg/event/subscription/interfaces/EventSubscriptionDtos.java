package com.hjo2oa.msg.event.subscription.interfaces;

import com.hjo2oa.msg.event.subscription.application.EventSubscriptionCommands;
import com.hjo2oa.msg.event.subscription.domain.ChannelType;
import com.hjo2oa.msg.event.subscription.domain.DigestMode;
import com.hjo2oa.msg.event.subscription.domain.EventMatchView;
import com.hjo2oa.msg.event.subscription.domain.NotificationCategory;
import com.hjo2oa.msg.event.subscription.domain.NotificationPriority;
import com.hjo2oa.msg.event.subscription.domain.QuietWindow;
import com.hjo2oa.msg.event.subscription.domain.SubscriptionPreferenceView;
import com.hjo2oa.msg.event.subscription.domain.SubscriptionRuleView;
import com.hjo2oa.msg.event.subscription.domain.TargetResolverType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class EventSubscriptionDtos {

    private EventSubscriptionDtos() {
    }

    public record SaveRuleRequest(
            @NotBlank @Size(max = 64) String ruleCode,
            @NotBlank @Size(max = 128) String eventTypePattern,
            @NotNull NotificationCategory notificationCategory,
            @NotNull TargetResolverType targetResolverType,
            @Size(max = 4000) String targetResolverConfig,
            @NotBlank @Size(max = 64) String templateCode,
            @Size(max = 1024) String conditionExpr,
            @Size(max = 4000) String priorityMapping,
            NotificationPriority defaultPriority,
            boolean enabled,
            @NotNull UUID tenantId
    ) {

        public EventSubscriptionCommands.SaveRuleCommand toCommand() {
            return new EventSubscriptionCommands.SaveRuleCommand(
                    ruleCode,
                    eventTypePattern,
                    notificationCategory,
                    targetResolverType,
                    targetResolverConfig,
                    templateCode,
                    conditionExpr,
                    priorityMapping,
                    defaultPriority,
                    enabled,
                    tenantId
            );
        }
    }

    public record UpdateRuleRequest(
            @NotBlank @Size(max = 128) String eventTypePattern,
            @NotNull NotificationCategory notificationCategory,
            @NotNull TargetResolverType targetResolverType,
            @Size(max = 4000) String targetResolverConfig,
            @NotBlank @Size(max = 64) String templateCode,
            @Size(max = 1024) String conditionExpr,
            @Size(max = 4000) String priorityMapping,
            NotificationPriority defaultPriority,
            boolean enabled
    ) {

        public EventSubscriptionCommands.UpdateRuleCommand toCommand() {
            return new EventSubscriptionCommands.UpdateRuleCommand(
                    eventTypePattern,
                    notificationCategory,
                    targetResolverType,
                    targetResolverConfig,
                    templateCode,
                    conditionExpr,
                    priorityMapping,
                    defaultPriority,
                    enabled
            );
        }
    }

    public record ToggleRuleRequest(
            boolean enabled,
            @Size(max = 256) String reason
    ) {
    }

    public record QuietWindowRequest(
            @NotNull LocalTime startsAt,
            @NotNull LocalTime endsAt
    ) {

        public QuietWindow toDomain() {
            return new QuietWindow(startsAt, endsAt);
        }
    }

    public record SavePreferenceRequest(
            @NotNull UUID personId,
            @NotNull UUID tenantId,
            @NotNull List<ChannelType> allowedChannels,
            QuietWindowRequest quietWindow,
            DigestMode digestMode,
            boolean escalationOptIn,
            boolean muteNonWorkingHours,
            boolean enabled
    ) {

        public EventSubscriptionCommands.SavePreferenceCommand toCommand(NotificationCategory category) {
            return new EventSubscriptionCommands.SavePreferenceCommand(
                    personId,
                    category,
                    allowedChannels,
                    quietWindow == null ? null : quietWindow.toDomain(),
                    digestMode,
                    escalationOptIn,
                    muteNonWorkingHours,
                    enabled,
                    tenantId
            );
        }
    }

    public record MatchEventRequest(
            @Size(max = 128) String eventId,
            @NotBlank @Size(max = 128) String eventType,
            @NotNull UUID tenantId,
            UUID recipientPersonId,
            NotificationPriority eventPriority,
            LocalTime occurredLocalTime
    ) {

        public EventSubscriptionCommands.EventMatchQuery toQuery() {
            return new EventSubscriptionCommands.EventMatchQuery(
                    eventId,
                    eventType,
                    tenantId,
                    recipientPersonId,
                    eventPriority,
                    occurredLocalTime
            );
        }
    }

    public record RuleResponse(
            UUID id,
            String ruleCode,
            String eventTypePattern,
            NotificationCategory notificationCategory,
            TargetResolverType targetResolverType,
            String targetResolverConfig,
            String templateCode,
            String conditionExpr,
            String priorityMapping,
            NotificationPriority defaultPriority,
            boolean enabled,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt
    ) {

        static RuleResponse from(SubscriptionRuleView view) {
            return new RuleResponse(
                    view.id(),
                    view.ruleCode(),
                    view.eventTypePattern(),
                    view.notificationCategory(),
                    view.targetResolverType(),
                    view.targetResolverConfig(),
                    view.templateCode(),
                    view.conditionExpr(),
                    view.priorityMapping(),
                    view.defaultPriority(),
                    view.enabled(),
                    view.tenantId(),
                    view.createdAt(),
                    view.updatedAt()
            );
        }
    }

    public record PreferenceResponse(
            UUID id,
            UUID personId,
            NotificationCategory category,
            List<ChannelType> allowedChannels,
            QuietWindow quietWindow,
            DigestMode digestMode,
            boolean escalationOptIn,
            boolean muteNonWorkingHours,
            boolean enabled,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt
    ) {

        static PreferenceResponse from(SubscriptionPreferenceView view) {
            return new PreferenceResponse(
                    view.id(),
                    view.personId(),
                    view.category(),
                    view.allowedChannels(),
                    view.quietWindow(),
                    view.digestMode(),
                    view.escalationOptIn(),
                    view.muteNonWorkingHours(),
                    view.enabled(),
                    view.tenantId(),
                    view.createdAt(),
                    view.updatedAt()
            );
        }
    }

    public record EventMatchResponse(
            UUID ruleId,
            String ruleCode,
            String eventType,
            NotificationCategory notificationCategory,
            TargetResolverType targetResolverType,
            String templateCode,
            NotificationPriority priority,
            boolean quietNow,
            DigestMode digestMode,
            List<ChannelType> allowedChannels,
            boolean escalationAllowed
    ) {

        static EventMatchResponse from(EventMatchView view) {
            return new EventMatchResponse(
                    view.ruleId(),
                    view.ruleCode(),
                    view.eventType(),
                    view.notificationCategory(),
                    view.targetResolverType(),
                    view.templateCode(),
                    view.priority(),
                    view.quietNow(),
                    view.digestMode(),
                    view.allowedChannels(),
                    view.escalationAllowed()
            );
        }
    }
}
