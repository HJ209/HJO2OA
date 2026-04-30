package com.hjo2oa.msg.channel.sender.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.msg.channel.sender.domain.ChannelEndpoint;
import com.hjo2oa.msg.channel.sender.domain.ChannelEndpointStatus;
import com.hjo2oa.msg.channel.sender.domain.ChannelSenderRepository;
import com.hjo2oa.msg.channel.sender.domain.ChannelType;
import com.hjo2oa.msg.channel.sender.domain.DeliveryAttempt;
import com.hjo2oa.msg.channel.sender.domain.DeliveryAttemptResultStatus;
import com.hjo2oa.msg.channel.sender.domain.DeliveryTask;
import com.hjo2oa.msg.channel.sender.domain.DeliveryTaskStatus;
import com.hjo2oa.msg.channel.sender.domain.DeliveryTaskView;
import com.hjo2oa.msg.channel.sender.domain.MessageTemplate;
import com.hjo2oa.msg.channel.sender.domain.MessageTemplateStatus;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

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
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ChannelSenderRepository repository;
    private final List<ChannelDeliveryAdapter> deliveryAdapters;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ChannelSenderApplicationService(
            ChannelSenderRepository repository,
            List<ChannelDeliveryAdapter> deliveryAdapters,
            ObjectMapper objectMapper
    ) {
        this(repository, deliveryAdapters, objectMapper, Clock.systemUTC());
    }

    public ChannelSenderApplicationService(ChannelSenderRepository repository) {
        this(repository, List.of(), new ObjectMapper(), Clock.systemUTC());
    }

    public ChannelSenderApplicationService(ChannelSenderRepository repository, Clock clock) {
        this(repository, List.of(), new ObjectMapper(), clock);
    }

    public ChannelSenderApplicationService(
            ChannelSenderRepository repository,
            List<ChannelDeliveryAdapter> deliveryAdapters,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.deliveryAdapters = List.copyOf(deliveryAdapters == null ? List.of() : deliveryAdapters);
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
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

    public RenderedMessageView renderTemplate(ChannelSenderCommands.RenderTemplateCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        MessageTemplate template = resolvePublishedTemplate(
                command.tenantId(),
                command.templateCode(),
                command.channelType(),
                command.locale()
        );
        return new RenderedMessageView(
                template.id(),
                template.code(),
                template.channelType(),
                template.locale(),
                template.version(),
                renderText(template.titleTemplate(), command.variables()),
                renderText(template.bodyTemplate(), command.variables())
        );
    }

    public List<DeliveryTaskView> dispatchNotification(
            ChannelSenderCommands.DispatchNotificationCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        List<ChannelType> channels = resolveChannels(command);
        java.util.ArrayList<DeliveryTaskView> sentTasks = new java.util.ArrayList<>(channels.size());
        int order = 0;
        for (ChannelType channel : channels) {
            if (channel == ChannelType.INBOX) {
                order++;
                continue;
            }
            UUID endpointId = selectEndpoint(command.tenantId(), channel);
            DeliveryTaskView task = createDeliveryTask(new ChannelSenderCommands.CreateDeliveryTaskCommand(
                    command.notificationId(),
                    channel,
                    endpointId,
                    order++,
                    command.tenantId()
            ));
            sentTasks.add(sendDeliveryTask(
                    task.id(),
                    command.recipientId(),
                    command.title(),
                    command.body(),
                    command.deepLink(),
                    Map.of("source", "message-center")
            ));
        }
        return sentTasks;
    }

    public DeliveryTaskView sendTest(ChannelSenderCommands.SendTestCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        DeliveryTaskView task = createDeliveryTask(new ChannelSenderCommands.CreateDeliveryTaskCommand(
                UUID.randomUUID(),
                command.channelType(),
                command.endpointId(),
                0,
                command.tenantId()
        ));
        return sendDeliveryTask(
                task.id(),
                command.target(),
                command.title(),
                command.body(),
                command.deepLink(),
                Map.of("source", "send-test")
        );
    }

    public DeliveryTaskView retryDeliveryTask(UUID deliveryTaskId) {
        DeliveryTask task = loadTask(deliveryTaskId);
        if (task.status() != DeliveryTaskStatus.FAILED && task.status() != DeliveryTaskStatus.GAVE_UP) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Delivery task is not retryable");
        }
        Map<String, Object> snapshot = lastRequestSnapshot(task);
        return sendDeliveryTask(
                deliveryTaskId,
                stringValue(snapshot.get("recipientId")),
                stringValue(snapshot.get("title")),
                stringValue(snapshot.get("body")),
                stringValue(snapshot.get("deepLink")),
                snapshot
        );
    }

    private DeliveryTaskView sendDeliveryTask(
            UUID deliveryTaskId,
            String recipientId,
            String title,
            String body,
            String deepLink,
            Map<String, Object> attributes
    ) {
        DeliveryTask task = loadTask(deliveryTaskId);
        ChannelEndpoint endpoint = task.endpointId() == null ? null : loadEndpoint(task.endpointId());
        ChannelDeliveryRequest request = new ChannelDeliveryRequest(
                task.id(),
                task.notificationId(),
                task.tenantId(),
                task.channelType(),
                endpoint,
                recipientId,
                title,
                body,
                deepLink,
                attributes
        );
        ChannelDeliveryResult result = adapterFor(task.channelType()).send(request);
        return recordDeliveryResult(new ChannelSenderCommands.RecordDeliveryResultCommand(
                deliveryTaskId,
                result.success()
                        ? DeliveryAttemptResultStatus.SUCCESS
                        : DeliveryAttemptResultStatus.FAILED,
                requestSnapshot(request),
                result.providerResponse(),
                result.providerMessageId(),
                result.errorCode(),
                result.errorMessage()
        ));
    }

    private List<ChannelType> resolveChannels(ChannelSenderCommands.DispatchNotificationCommand command) {
        List<RoutingPolicy> policies = repository.findEnabledRoutingPolicies(
                command.tenantId(),
                command.category(),
                command.priority()
        );
        List<ChannelType> channels = policies.isEmpty()
                ? command.allowedChannels()
                : policies.get(0).targetChannelOrder();
        if (command.allowedChannels().isEmpty()) {
            return channels;
        }
        return channels.stream()
                .filter(channel -> command.allowedChannels().contains(channel))
                .toList();
    }

    private ChannelDeliveryAdapter adapterFor(ChannelType channelType) {
        return deliveryAdapters.stream()
                .filter(adapter -> adapter.channelType() == channelType)
                .findFirst()
                .orElse(new MissingChannelDeliveryAdapter(channelType));
    }

    private MessageTemplate resolvePublishedTemplate(
            UUID tenantId,
            String templateCode,
            ChannelType channelType,
            String locale
    ) {
        String normalizedLocale = normalizeLocale(locale == null ? "zh-CN" : locale);
        return candidateTemplates(tenantId).stream()
                .filter(template -> template.status() == MessageTemplateStatus.PUBLISHED)
                .filter(template -> template.code().equals(templateCode))
                .filter(template -> template.channelType() == channelType)
                .sorted(Comparator
                        .comparingInt((MessageTemplate template) -> localeScore(template.locale(), normalizedLocale))
                        .thenComparing(Comparator.comparingInt(MessageTemplate::version).reversed()))
                .findFirst()
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Published message template not found"
                ));
    }

    private List<MessageTemplate> candidateTemplates(UUID tenantId) {
        java.util.ArrayList<MessageTemplate> templates =
                new java.util.ArrayList<>(repository.findTemplates(tenantId, null));
        if (tenantId != null) {
            templates.addAll(repository.findTemplates(null, null));
        }
        return templates;
    }

    private int localeScore(String candidate, String requested) {
        if (candidate.equals(requested)) {
            return 0;
        }
        if ("zh-cn".equals(candidate)) {
            return 1;
        }
        if ("en-us".equals(candidate)) {
            return 2;
        }
        return 3;
    }

    private String renderText(String template, Map<String, Object> variables) {
        String rendered = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            rendered = rendered.replace("{{" + entry.getKey() + "}}", value)
                    .replace("${" + entry.getKey() + "}", value);
        }
        if (rendered.contains("{{") || rendered.contains("${")) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "Template variables are incomplete"
            );
        }
        return rendered;
    }

    private String requestSnapshot(ChannelDeliveryRequest request) {
        Map<String, Object> snapshot = new LinkedHashMap<>(request.attributes());
        snapshot.put("notificationId", request.notificationId());
        snapshot.put("tenantId", request.tenantId());
        snapshot.put("channelType", request.channelType());
        snapshot.put("recipientId", request.recipientId());
        snapshot.put("title", request.title());
        snapshot.put("body", request.body());
        snapshot.put("deepLink", request.deepLink());
        snapshot.put("endpointId", request.endpoint() == null ? null : request.endpoint().id());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new BizException(SharedErrorDescriptors.INTERNAL_ERROR, "Unable to serialize delivery request");
        }
    }

    private Map<String, Object> lastRequestSnapshot(DeliveryTask task) {
        return task.attempts().stream()
                .reduce((first, second) -> second)
                .map(DeliveryAttempt::requestPayloadSnapshot)
                .filter(snapshot -> snapshot != null && !snapshot.isBlank())
                .map(this::readSnapshot)
                .orElse(Map.of());
    }

    private Map<String, Object> readSnapshot(String snapshot) {
        try {
            return objectMapper.readValue(snapshot, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalizeLocale(String locale) {
        return locale.trim().replace('_', '-').toLowerCase(Locale.ROOT);
    }

    private int nextTemplateVersion(ChannelSenderCommands.CreateTemplateCommand command) {
        return repository.findTemplates(command.tenantId(), command.category()).stream()
                .filter(template -> template.code().equals(command.code()))
                .filter(template -> template.channelType() == command.channelType())
                .filter(template -> template.locale().equals(normalizeLocale(command.locale())))
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
