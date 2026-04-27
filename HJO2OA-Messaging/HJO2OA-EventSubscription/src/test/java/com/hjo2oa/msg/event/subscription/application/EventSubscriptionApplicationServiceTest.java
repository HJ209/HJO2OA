package com.hjo2oa.msg.event.subscription.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.msg.event.subscription.domain.ChannelType;
import com.hjo2oa.msg.event.subscription.domain.DigestMode;
import com.hjo2oa.msg.event.subscription.domain.EventMatchView;
import com.hjo2oa.msg.event.subscription.domain.NotificationCategory;
import com.hjo2oa.msg.event.subscription.domain.NotificationPriority;
import com.hjo2oa.msg.event.subscription.domain.QuietWindow;
import com.hjo2oa.msg.event.subscription.domain.TargetResolverType;
import com.hjo2oa.msg.event.subscription.infrastructure.InMemoryEventSubscriptionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventSubscriptionApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T06:00:00Z");
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PERSON_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void shouldMatchWildcardRuleAndApplyQuietPreference() {
        EventSubscriptionApplicationService service = service();
        service.createRule(new EventSubscriptionCommands.SaveRuleCommand(
                "todo.created.default",
                "todo.*",
                NotificationCategory.TODO_CREATED,
                TargetResolverType.PAYLOAD_ASSIGNMENT,
                "{\"personField\":\"recipientPersonId\"}",
                "todo-created",
                null,
                null,
                NotificationPriority.NORMAL,
                true,
                TENANT_ID
        ));
        service.savePreference(new EventSubscriptionCommands.SavePreferenceCommand(
                PERSON_ID,
                NotificationCategory.TODO_CREATED,
                List.of(ChannelType.INBOX, ChannelType.PUSH),
                new QuietWindow(LocalTime.of(22, 0), LocalTime.of(8, 0)),
                DigestMode.PERIODIC_DIGEST,
                false,
                true,
                true,
                TENANT_ID
        ));

        List<EventMatchView> matches = service.matchEvent(
                "todo.item.created",
                TENANT_ID,
                PERSON_ID,
                null,
                LocalTime.of(23, 0)
        );

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).ruleCode()).isEqualTo("todo.created.default");
        assertThat(matches.get(0).quietNow()).isTrue();
        assertThat(matches.get(0).digestMode()).isEqualTo(DigestMode.PERIODIC_DIGEST);
        assertThat(matches.get(0).allowedChannels()).containsExactly(ChannelType.INBOX, ChannelType.PUSH);
        assertThat(matches.get(0).escalationAllowed()).isFalse();
    }

    @Test
    void shouldUpdatePreferencePerPersonCategory() {
        EventSubscriptionApplicationService service = service();

        service.savePreference(new EventSubscriptionCommands.SavePreferenceCommand(
                PERSON_ID,
                NotificationCategory.TODO_OVERDUE,
                List.of(ChannelType.INBOX),
                null,
                DigestMode.IMMEDIATE,
                true,
                false,
                true,
                TENANT_ID
        ));
        service.savePreference(new EventSubscriptionCommands.SavePreferenceCommand(
                PERSON_ID,
                NotificationCategory.TODO_OVERDUE,
                List.of(ChannelType.EMAIL),
                null,
                DigestMode.DISABLED,
                false,
                true,
                false,
                TENANT_ID
        ));

        assertThat(service.listPreferences(PERSON_ID, TENANT_ID)).hasSize(1);
        assertThat(service.listPreferences(PERSON_ID, TENANT_ID).get(0).allowedChannels())
                .containsExactly(ChannelType.EMAIL);
        assertThat(service.listPreferences(PERSON_ID, TENANT_ID).get(0).enabled()).isFalse();
    }

    private EventSubscriptionApplicationService service() {
        return new EventSubscriptionApplicationService(
                new InMemoryEventSubscriptionRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
