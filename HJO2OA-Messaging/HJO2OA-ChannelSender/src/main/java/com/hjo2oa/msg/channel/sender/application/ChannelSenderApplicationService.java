package com.hjo2oa.msg.channel.sender.application;

import com.hjo2oa.msg.channel.sender.domain.ChannelEndpoint;
import com.hjo2oa.msg.channel.sender.domain.ChannelEndpointStatus;
import com.hjo2oa.msg.channel.sender.domain.ChannelSenderRepository;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import com.hjo2oa.msg.channel.sender.domain.DeliveryAttempt;
import com.hjo2oa.msg.channel.sender.domain.DeliveryAttemptResultStatus;
import com.hjo2oa.msg.channel.sender.domain.DeliveryTask;
import com.hjo2oa.msg.channel.sender.domain.DeliveryTaskView;
import com.hjo2oa.msg.channel.sender.domain.MessageTemplate;
import com.hjo2oa.msg.channel.sender.domain.MessageTemplateView;
import com.hjo2oa.msg.channel.sender.domain.RoutingPolicy;
import com.hjo2oa.msg.channel.sender.domain.RoutingPolicyView;
import com.hjo2oa.msg.channel.sender.domain.ChannelEndpointView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ChannelSenderApplicationService {

    private static final int DEFAULT_DEDUP_WINDOW_SECONDS = 300;
    private static final List<Duration> DEFAULT_RETRY_DELAYS = List.of(
            Duration.ofSeconds(1),
            Duration.ofSeconds(5),
            Duration.ofSeconds(30),
            Duration.ofMinutes(5),
            Duration.ofMinutes(30)
    );

    private final ChannelSenderRepository repository;
    private final Clock clock;

    public ChannelSenderApplicationService(ChannelSenderRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public ChannelSenderApplicationService(ChannelSenderRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public MessageTemplateView createTemplate(ChannelSenderCommands.CreateTemplateCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        int version = command.version() == null ? nextTemplateVersion(command) : command.version();
        repository.findTemplate(
                        command.tenantId(),
                        command.code(),
                        command.channelType(),
                        command.locale(),
                        version
                )
                .ifPresent(existing -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Message template already exists");
                });
        MessageTemplate template = MessageTemplate.create(
                UUID.randomUUID(),
                command.code(),
                command.channelType(),
                command.locale(),
                version,
                command.category(),
                command.titleTemplate(),
                command.bodyTemplate(),
                command.variableSchema(),
                command.systemLocked(),
                command.tenantId(),
                now()
        );
        return repository.saveTemplate(template).toView();
    }

    public MessageTemplateView publishTemplate(UUID templateId) {
        MessageTemplate template = loadTemplate(templateId);
        return repository.saveTemplate(template.publish(now())).toView();
    }

    public MessageTemplateView disableTemplate(UUID templateId) {
        try {
            return repository.saveTemplate(loadTemplate(templateId).disable(now())).toView();
        } catch (IllegalStateException ex) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, ex.getMessage());
        }
    }

    public List<MessageTemplateView> listTemplates(UUID tenantId, String category) {
        com.hjo2oa.msg.channel.sender.domain.MessageCategory resolvedCategory = category == null
                ? null
                : com.hjo2oa.msg.channel.sender.domain.MessageCategory.valueOf(category);
        return repository.findTemplates(tenantId, resolvedCategory).stream()
                .sorted(Comparator.comparing(MessageTemplate::code).thenComparing(MessageTemplate::version))
                .map(MessageTemplate::toView)
                .toList();
    }

    public ChannelEndpointView createEndpoint(ChannelSenderCommands.CreateEndpointCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        repository.findEndpoint(command.tenantId(), command.endpointCode())
                .ifPresent(existing -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Channel endpoint already exists");
                });
        ChannelEndpoint endpoint = ChannelEndpoint.create(
                UUID.randomUUID(),
                command.endpointCode(),
                command.channelType(),
                command.providerType(),
                command.displayName(),
                command.configRef(),
                command.rateLimitPerMinute(),
                command.dailyQuota(),
                command.tenantId(),
                now()
        );
        return repository.saveEndpoint(endpoint).toView();
    }

    public ChannelEndpointView changeEndpointStatus(ChannelSenderCommands.ChangeEndpointStatusCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ChannelEndpoint endpoint = loadEndpoint(command.endpointId());
        ChannelEndpointStatus status = Objects.requireNonNull(command.status(), "status must not be null");
        return repository.saveEndpoint(endpoint.withStatus(status, now())).toView();
    }

    public List<ChannelEndpointView> listEndpoints(UUID tenantId, ChannelType channelType) {
        return repository.findEndpoints(tenantId, channelType).stream()
                .sorted(Comparator.comparing(ChannelEndpoint::endpointCode))
                .map(ChannelEndpoint::toView)
                .toList();
    }

    public RoutingPolicyView createRoutingPolicy(ChannelSenderCommands.CreateRoutingPolicyCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        repository.findRoutingPolicy(command.tenantId(), command.policyCode())
                .ifPresent(existing -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Routing policy already exists");
                });
        RoutingPolicy policy = RoutingPolicy.create(
                UUID.randomUUID(),
                command.policyCode(),
                command.category(),
                command.priorityThreshold(),
                command.targetChannelOrder(),
                command.fallbackChannelOrder(),
                command.quietWindowBehavior(),
                command.dedupWindowSeconds() == null ? DEFAULT_DEDUP_WINDOW_SECONDS : command.dedupWindowSeconds(),
                command.escalationPolicy(),
                command.tenantId(),
                now()
        );
        return repository.saveRoutingPolicy(policy).toView();
    }

    public RoutingPolicyView enableRoutingPolicy(UUID policyId) {
        return repository.saveRoutingPolicy(loadPolicy(policyId).enable(now())).toView();
    }

    public RoutingPolicyView disableRoutingPolicy(UUID policyId) {
        return repository.saveRoutingPolicy(loadPolicy(policyId).disable(now())).toView();
    }

    public List<RoutingPolicyView> listRoutingPolicies(
            UUID tenantId,
            com.hjo2oa.msg.channel.sender.domain.MessageCategory category,
            com.hjo2oa.msg.channel.sender.domain.MessagePriority priority
    ) {
        return repository.findEnabledRoutingPolicies(tenantId, category, priority).stream()
                .sorted(Comparator.comparing(RoutingPolicy::policyCode))
                .map(RoutingPolicy::toView)
                .toList();
    }

    public DeliveryTaskView createDeliveryTask(ChannelSenderCommands.CreateDeliveryTaskCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        DeliveryTask task = DeliveryTask.create(
                UUID.randomUUID(),
                command.notificationId(),
                command.channelType(),
                command.endpointId(),
                command.routeOrder(),
                command.tenantId(),
                now()
        );
        return repository.saveDeliveryTask(task).toView();
    }

    public List<DeliveryTaskView> routeDeliveryTasks(ChannelSenderCommands.RouteDeliveryTasksCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        List<RoutingPolicy> policies = repository.findEnabledRoutingPolicies(
                command.tenantId(),
                command.category(),
                command.priority()
        );
        List<ChannelType> channels = policies.isEmpty()
                ? List.of(ChannelType.INBOX)
                : policies.get(0).targetChannelOrder();
        java.util.ArrayList<DeliveryTaskView> created = new java.util.ArrayList<>(channels.size());
        int order = 0;
        for (ChannelType channel : channels) {
            UUID endpointId = selectEndpoint(command.tenantId(), channel);
            created.add(createDeliveryTask(new ChannelSenderCommands.CreateDeliveryTaskCommand(
                    command.notificationId(),
                    channel,
                    endpointId,
                    order++,
                    command.tenantId()
            )));
        }
        return created;
    }

    public DeliveryTaskView recordDeliveryResult(ChannelSenderCommands.RecordDeliveryResultCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        DeliveryTask task = loadTask(command.deliveryTaskId());
        Instant current = now();
        DeliveryAttempt attempt = DeliveryAttempt.create(
                UUID.randomUUID(),
                task.id(),
                task.attempts().size() + 1,
                command.requestPayloadSnapshot(),
                command.providerResponse(),
                command.resultStatus(),
                command.errorCode(),
                command.errorMessage(),
                current
        );
        if (command.resultStatus() == DeliveryAttemptResultStatus.SUCCESS) {
            return repository.saveDeliveryTask(task.markDelivered(
                    command.providerMessageId(),
                    attempt,
                    current
            )).toView();
        }
        Instant nextRetryAt = current.plus(retryDelay(task.retryCount()));
        DeliveryTask failed = task.markFailed(
                attempt,
                DEFAULT_RETRY_DELAYS.size(),
                nextRetryAt,
                command.errorCode(),
                command.errorMessage(),
                current
        );
        return repository.saveDeliveryTask(failed).toView();
    }

    public List<DeliveryTaskView> retryableTasks(UUID tenantId) {
        return repository.findRetryableTasks(tenantId).stream()
                .sorted(Comparator.comparing(DeliveryTask::nextRetryAt))
                .map(DeliveryTask::toView)
                .toList();
    }

    private int nextTemplateVersion(ChannelSenderCommands.CreateTemplateCommand command) {
        return repository.findTemplates(command.tenantId(), command.category()).stream()
                .filter(template -> template.code().equals(command.code()))
                .filter(template -> template.channelType() == command.channelType())
                .filter(template -> template.locale().equals(command.locale()
                        .replace('_', '-')
                        .toLowerCase(java.util.Locale.ROOT)))
                .mapToInt(MessageTemplate::version)
                .max()
                .orElse(0) + 1;
    }

    private UUID selectEndpoint(UUID tenantId, ChannelType channel) {
        if (channel == ChannelType.INBOX) {
            return null;
        }
        return repository.findEndpoints(tenantId, channel).stream()
                .filter(endpoint -> endpoint.status() == ChannelEndpointStatus.ENABLED)
                .map(ChannelEndpoint::id)
                .findFirst()
                .orElse(null);
    }

    private Duration retryDelay(int retryCount) {
        return DEFAULT_RETRY_DELAYS.get(Math.min(retryCount, DEFAULT_RETRY_DELAYS.size() - 1));
    }

    private MessageTemplate loadTemplate(UUID templateId) {
        return repository.findTemplateById(templateId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Message template not found"
                ));
    }

    private ChannelEndpoint loadEndpoint(UUID endpointId) {
        return repository.findEndpointById(endpointId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Channel endpoint not found"
                ));
    }

    private RoutingPolicy loadPolicy(UUID policyId) {
        return repository.findRoutingPolicyById(policyId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Routing policy not found"
                ));
    }

    private DeliveryTask loadTask(UUID taskId) {
        return repository.findDeliveryTaskById(taskId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Delivery task not found"
                ));
    }

    private Instant now() {
        return clock.instant();
    }
}
