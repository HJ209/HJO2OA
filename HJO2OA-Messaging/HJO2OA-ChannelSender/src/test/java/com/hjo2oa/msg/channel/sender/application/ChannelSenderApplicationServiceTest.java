package com.hjo2oa.msg.channel.sender.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.msg.channel.sender.domain.ChannelEndpoint;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import com.hjo2oa.msg.channel.sender.domain.DeliveryTaskStatus;
import com.hjo2oa.msg.channel.sender.domain.MessageCategory;
import com.hjo2oa.msg.channel.sender.domain.MessagePriority;
import com.hjo2oa.msg.channel.sender.domain.ProviderType;
import com.hjo2oa.msg.channel.sender.domain.QuietWindowBehavior;
import com.hjo2oa.msg.channel.sender.infrastructure.InMemoryChannelSenderRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChannelSenderApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-29T01:00:00Z");
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void shouldRenderPublishedTemplateVariablesByLocale() {
        ChannelSenderApplicationService service = service(List.of());
        var draft = service.createTemplate(new ChannelSenderCommands.CreateTemplateCommand(
                "todo-created",
                ChannelType.INBOX,
                "zh-CN",
                null,
                MessageCategory.TODO_CREATED,
                "待办 {{title}}",
                "请处理 {{title}}，截止 {{dueAt}}",
                null,
                false,
                TENANT_ID
        ));
        service.publishTemplate(draft.id());

        RenderedMessageView rendered = service.renderTemplate(new ChannelSenderCommands.RenderTemplateCommand(
                TENANT_ID,
                "todo-created",
                ChannelType.INBOX,
                "zh-CN",
                Map.of("title", "费用审批", "dueAt", "18:00")
        ));

        assertThat(rendered.title()).isEqualTo("待办 费用审批");
        assertThat(rendered.body()).isEqualTo("请处理 费用审批，截止 18:00");
    }

    @Test
    void shouldRecordFailureAndRetryDeliveryTask() {
        FailingOnceAdapter adapter = new FailingOnceAdapter();
        ChannelSenderApplicationService service = service(List.of(adapter));
        ChannelEndpoint endpoint = ChannelEndpoint.create(
                UUID.randomUUID(),
                "webhook-main",
                ChannelType.WEBHOOK,
                ProviderType.WEBHOOK,
                "Webhook",
                "https://example.invalid/hook",
                null,
                null,
                TENANT_ID,
                NOW
        );
        ((InMemoryChannelSenderRepository) repository(service)).saveEndpoint(endpoint);
        service.createRoutingPolicy(new ChannelSenderCommands.CreateRoutingPolicyCommand(
                "critical-webhook",
                MessageCategory.TODO_OVERDUE,
                MessagePriority.NORMAL,
                List.of(ChannelType.WEBHOOK),
                List.of(ChannelType.INBOX),
                QuietWindowBehavior.BYPASS,
                60,
                null,
                TENANT_ID
        ));

        var dispatched = service.dispatchNotification(new ChannelSenderCommands.DispatchNotificationCommand(
                UUID.randomUUID(),
                TENANT_ID,
                "user@example.com",
                "逾期提醒",
                "待办已逾期",
                "/todo/1",
                MessageCategory.TODO_OVERDUE,
                MessagePriority.CRITICAL,
                List.of(ChannelType.WEBHOOK)
        ));

        assertThat(dispatched).singleElement().satisfies(task -> {
            assertThat(task.status()).isEqualTo(DeliveryTaskStatus.FAILED);
            assertThat(task.retryCount()).isEqualTo(1);
            assertThat(task.lastErrorCode()).isEqualTo("TEMPORARY");
        });

        var retried = service.retryDeliveryTask(dispatched.get(0).id());

        assertThat(retried.status()).isEqualTo(DeliveryTaskStatus.DELIVERED);
        assertThat(retried.retryCount()).isEqualTo(1);
        assertThat(retried.attempts()).hasSize(2);
    }

    private ChannelSenderApplicationService service(List<ChannelDeliveryAdapter> adapters) {
        return new ChannelSenderApplicationService(
                new InMemoryChannelSenderRepository(),
                adapters,
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private Object repository(ChannelSenderApplicationService service) {
        try {
            java.lang.reflect.Field field = ChannelSenderApplicationService.class.getDeclaredField("repository");
            field.setAccessible(true);
            return field.get(service);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static final class FailingOnceAdapter implements ChannelDeliveryAdapter {

        private int attempts;

        @Override
        public ChannelType channelType() {
            return ChannelType.WEBHOOK;
        }

        @Override
        public ChannelDeliveryResult send(ChannelDeliveryRequest request) {
            attempts++;
            if (attempts == 1) {
                return ChannelDeliveryResult.failure("TEMPORARY", "Temporary provider failure", "{}");
            }
            return ChannelDeliveryResult.success("provider-2", "{}");
        }
    }
}
