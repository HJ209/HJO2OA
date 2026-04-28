package com.hjo2oa.data.connector.application;

import com.hjo2oa.data.connector.infrastructure.ConnectorDriverRegistry;
import com.hjo2oa.data.common.application.audit.DataAuditLog;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import com.hjo2oa.shared.web.Pagination;
import com.hjo2oa.data.connector.domain.ConnectorAuthMode;
import com.hjo2oa.data.connector.domain.ConnectorCheckType;
import com.hjo2oa.data.connector.domain.ConnectorContext;
import com.hjo2oa.data.connector.domain.ConnectorContextProvider;
import com.hjo2oa.data.connector.domain.ConnectorDefinition;
import com.hjo2oa.data.connector.domain.ConnectorDefinitionRepository;
import com.hjo2oa.data.connector.domain.ConnectorDefinitionView;
import com.hjo2oa.data.connector.domain.ConnectorDriver;
import com.hjo2oa.data.connector.domain.ConnectorHealthOverviewView;
import com.hjo2oa.data.connector.domain.ConnectorHealthSnapshot;
import com.hjo2oa.data.connector.domain.ConnectorHealthStatus;
import com.hjo2oa.data.connector.domain.ConnectorHealthSnapshotView;
import com.hjo2oa.data.connector.domain.ConnectorListFilterView;
import com.hjo2oa.data.connector.domain.ConnectorListView;
import com.hjo2oa.data.connector.domain.ConnectorPageResult;
import com.hjo2oa.data.connector.domain.ConnectorParameter;
import com.hjo2oa.data.connector.domain.ConnectorParameterTemplate;
import com.hjo2oa.data.connector.domain.ConnectorParameterTemplateCategory;
import com.hjo2oa.data.connector.domain.ConnectorStatus;
import com.hjo2oa.data.connector.domain.ConnectorSummaryView;
import com.hjo2oa.data.connector.domain.ConnectorTestResult;
import com.hjo2oa.data.connector.domain.ConnectorType;
import com.hjo2oa.data.connector.domain.DataConnectorUpdatedEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConnectorDefinitionApplicationService {

    private static final Duration CONNECTIVITY_CHECK_DEDUP_WINDOW = Duration.ofSeconds(30);

    private final ConnectorDefinitionRepository repository;
    private final ConnectorContextProvider contextProvider;
    private final ConnectorDriverRegistry connectorDriverRegistry;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;
    @Autowired
    public ConnectorDefinitionApplicationService(
            ConnectorDefinitionRepository repository,
            ConnectorContextProvider contextProvider,
            ConnectorDriverRegistry connectorDriverRegistry,
            DomainEventPublisher domainEventPublisher
    ) {
        this(repository, contextProvider, connectorDriverRegistry, domainEventPublisher, Clock.systemUTC());
    }
    public ConnectorDefinitionApplicationService(
            ConnectorDefinitionRepository repository,
            ConnectorContextProvider contextProvider,
            ConnectorDriverRegistry connectorDriverRegistry,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.contextProvider = Objects.requireNonNull(contextProvider, "contextProvider must not be null");
        this.connectorDriverRegistry = Objects.requireNonNull(
                connectorDriverRegistry,
                "connectorDriverRegistry must not be null"
        );
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ConnectorListView list(
            ConnectorType connectorType,
            ConnectorStatus status,
            String code,
            String keyword,
            Integer page,
            Integer size
    ) {
        ConnectorContext context = contextProvider.currentContext();
        int normalizedPage = page == null || page < 1 ? 1 : page;
        int normalizedSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        ConnectorPageResult result = repository.findPage(
                context.tenantId(),
                connectorType,
                status,
                code,
                keyword,
                normalizedPage,
                normalizedSize
        );
        List<ConnectorSummaryView> items = result.items().stream()
                .map(connector -> connector.toSummaryView(
                        latestSnapshot(connector.connectorId(), ConnectorCheckType.MANUAL_TEST).orElse(null),
                        latestSnapshot(connector.connectorId(), ConnectorCheckType.HEALTH_CHECK).orElse(null)
                ))
                .sorted(Comparator.comparing(ConnectorSummaryView::code))
                .toList();
        return new ConnectorListView(
                items,
                Pagination.of(normalizedPage, normalizedSize, result.total()),
                new ConnectorListFilterView(connectorType, status, normalizeNullable(code), normalizeNullable(keyword),
                        normalizedPage, normalizedSize)
        );
    }

    public Optional<ConnectorDefinitionView> current(String connectorId) {
        Objects.requireNonNull(connectorId, "connectorId must not be null");
        return repository.findById(connectorId)
                .map(connector -> connector.toView(
                        latestSnapshot(connector.connectorId(), ConnectorCheckType.MANUAL_TEST).orElse(null),
                        latestSnapshot(connector.connectorId(), ConnectorCheckType.HEALTH_CHECK).orElse(null)
                ));
    }

    public List<ConnectorParameterTemplate> parameterTemplates(
            String connectorId,
            ConnectorParameterTemplateCategory category,
            Boolean sensitive
    ) {
        ConnectorDefinition connectorDefinition = loadRequiredConnector(connectorId);
        return driverFor(connectorDefinition).parameterTemplates(connectorDefinition).stream()
                .filter(template -> category == null || template.category() == category)
                .filter(template -> sensitive == null || template.sensitive() == sensitive)
                .toList();
    }

    public List<ConnectorHealthSnapshotView> recentTestSnapshots(
            String connectorId,
            ConnectorHealthStatus healthStatus,
            Instant checkedFrom,
            Instant checkedTo,
            int limit
    ) {
        loadRequiredConnector(connectorId);
        return repository.findSnapshots(
                        connectorId,
                        ConnectorCheckType.MANUAL_TEST,
                        healthStatus,
                        checkedFrom,
                        checkedTo,
                        normalizeLimit(limit)
                ).stream()
                .map(ConnectorHealthSnapshot::toView)
                .toList();
    }

    public Optional<ConnectorHealthSnapshotView> latestTestSnapshot(String connectorId) {
        loadRequiredConnector(connectorId);
        return latestSnapshot(connectorId, ConnectorCheckType.MANUAL_TEST).map(ConnectorHealthSnapshot::toView);
    }

    public List<ConnectorHealthSnapshotView> recentHealthSnapshots(
            String connectorId,
            ConnectorHealthStatus healthStatus,
            Instant checkedFrom,
            Instant checkedTo,
            int limit
    ) {
        loadRequiredConnector(connectorId);
        return repository.findSnapshots(
                        connectorId,
                        ConnectorCheckType.HEALTH_CHECK,
                        healthStatus,
                        checkedFrom,
                        checkedTo,
                        normalizeLimit(limit)
                ).stream()
                .map(ConnectorHealthSnapshot::toView)
                .toList();
    }

    public Optional<ConnectorHealthOverviewView> latestHealthOverview(String connectorId) {
        loadRequiredConnector(connectorId);
        ConnectorCheckType snapshotType = ConnectorCheckType.HEALTH_CHECK;
        Optional<ConnectorHealthSnapshot> latestHealthSnapshot = latestSnapshot(connectorId, snapshotType);
        if (latestHealthSnapshot.isEmpty()) {
            snapshotType = ConnectorCheckType.MANUAL_TEST;
            latestHealthSnapshot = latestSnapshot(connectorId, snapshotType);
        }
        if (latestHealthSnapshot.isEmpty()) {
            return Optional.empty();
        }
        List<ConnectorHealthSnapshot> recentHealthSnapshots = repository.findSnapshots(
                connectorId,
                snapshotType,
                null,
                null,
                null,
                10
        );
        ConnectorHealthSnapshot lastFailureSnapshot = recentHealthSnapshots.stream()
                .filter(snapshot -> snapshot.healthStatus() != ConnectorHealthStatus.HEALTHY)
                .findFirst()
                .orElse(null);
        return Optional.of(new ConnectorHealthOverviewView(
                connectorId,
                latestHealthSnapshot.get().toView(),
                lastFailureSnapshot == null ? null : lastFailureSnapshot.toView(),
                recentHealthSnapshots.size(),
                recentHealthSnapshots.stream()
                        .filter(snapshot -> snapshot.healthStatus() == ConnectorHealthStatus.HEALTHY)
                        .count(),
                recentHealthSnapshots.stream()
                        .filter(snapshot -> snapshot.healthStatus() == ConnectorHealthStatus.DEGRADED)
                        .count(),
                recentHealthSnapshots.stream()
                        .filter(snapshot -> snapshot.healthStatus() == ConnectorHealthStatus.UNREACHABLE)
                        .count()
        ));
    }

    @DataAuditLog(module = "connector", action = "upsert-definition", targetType = "ConnectorDefinition", captureArguments = true)
    public ConnectorDefinitionView upsert(UpsertConnectorDefinitionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ConnectorContext context = contextProvider.currentContext();
        Instant now = now();
        ensureSupportedConnectorType(command.connectorType());
        ConnectorDefinition existing = repository.findById(command.connectorId()).orElse(null);
        ensureCodeUniqueness(context.tenantId(), command.code(), command.connectorId());

        ConnectorDefinition connectorDefinition = existing == null
                ? ConnectorDefinition.create(
                        command.connectorId(),
                        context.tenantId(),
                        command.code(),
                        command.name(),
                        command.connectorType(),
                        command.vendor(),
                        command.protocol(),
                        command.authMode(),
                        command.timeoutConfig(),
                        now
                )
                : existing.updateMetadata(
                        command.code(),
                        command.name(),
                        command.connectorType(),
                        command.vendor(),
                        command.protocol(),
                        command.authMode(),
                        command.timeoutConfig(),
                        now
                );
        validateAuthMode(connectorDefinition);

        List<String> changedFields = changedFields(existing, connectorDefinition);
        repository.save(connectorDefinition);
        publishIfChanged(connectorDefinition, changedFields, now);
        return connectorDefinition.toView(
                latestSnapshot(connectorDefinition.connectorId(), ConnectorCheckType.MANUAL_TEST).orElse(null),
                latestSnapshot(connectorDefinition.connectorId(), ConnectorCheckType.HEALTH_CHECK).orElse(null)
        );
    }

    @DataAuditLog(module = "connector", action = "configure-parameters", targetType = "ConnectorDefinition", captureArguments = true)
    public ConnectorDefinitionView configureParameters(ConfigureConnectorParametersCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ConnectorDefinition connectorDefinition = loadRequiredConnector(command.connectorId());
        Instant now = now();
        List<ConnectorParameter> parameters = toDomainParameters(connectorDefinition.connectorId(), command.parameters());
        validateParameters(connectorDefinition, parameters);
        ConnectorDefinition updated = connectorDefinition.configureParameters(parameters, now);
        repository.save(updated);
        publishIfChanged(updated, changedFields(connectorDefinition, updated), now);
        return updated.toView(
                latestSnapshot(updated.connectorId(), ConnectorCheckType.MANUAL_TEST).orElse(null),
                latestSnapshot(updated.connectorId(), ConnectorCheckType.HEALTH_CHECK).orElse(null)
        );
    }

    @DataAuditLog(module = "connector", action = "activate", targetType = "ConnectorDefinition", captureArguments = true)
    public ConnectorDefinitionView activate(String connectorId) {
        ConnectorDefinition connectorDefinition = loadRequiredConnector(connectorId);
        validateParameters(connectorDefinition, connectorDefinition.parameters());
        ConnectorHealthSnapshot latestTestSnapshot = latestSnapshot(connectorId, ConnectorCheckType.MANUAL_TEST)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                        "连接器启用前必须先完成成功的连接测试"
                ));
        if (!latestTestSnapshot.healthy() || latestTestSnapshot.changeSequence() != connectorDefinition.changeSequence()) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "连接器启用前必须基于最新配置完成成功的连接测试"
            );
        }
        ConnectorDefinition updated = connectorDefinition.activate(now());
        repository.save(updated);
        publishIfChanged(updated, changedFields(connectorDefinition, updated), now());
        return updated.toView(
                latestSnapshot(updated.connectorId(), ConnectorCheckType.MANUAL_TEST).orElse(null),
                latestSnapshot(updated.connectorId(), ConnectorCheckType.HEALTH_CHECK).orElse(null)
        );
    }

    @DataAuditLog(module = "connector", action = "disable", targetType = "ConnectorDefinition", captureArguments = true)
    public ConnectorDefinitionView disable(String connectorId) {
        ConnectorDefinition connectorDefinition = loadRequiredConnector(connectorId);
        ConnectorDefinition updated = connectorDefinition.disable(now());
        repository.save(updated);
        publishIfChanged(updated, changedFields(connectorDefinition, updated), now());
        return updated.toView(
                latestSnapshot(updated.connectorId(), ConnectorCheckType.MANUAL_TEST).orElse(null),
                latestSnapshot(updated.connectorId(), ConnectorCheckType.HEALTH_CHECK).orElse(null)
        );
    }

    @DataAuditLog(module = "connector", action = "manual-test", targetType = "ConnectorHealthSnapshot", captureArguments = true)
    public ConnectorHealthSnapshotView testConnection(String connectorId) {
        return runConnectivityCheck(connectorId, ConnectorCheckType.MANUAL_TEST);
    }

    @DataAuditLog(module = "connector", action = "refresh-health", targetType = "ConnectorHealthSnapshot", captureArguments = true)
    public ConnectorHealthSnapshotView refreshHealth(String connectorId) {
        return runConnectivityCheck(connectorId, ConnectorCheckType.HEALTH_CHECK);
    }

    @DataAuditLog(module = "connector", action = "confirm-health-abnormal", targetType = "ConnectorHealthSnapshot", captureArguments = true)
    public ConnectorHealthSnapshotView confirmHealthAbnormal(String connectorId, String snapshotId, String note) {
        loadRequiredConnector(connectorId);
        ConnectorHealthSnapshot healthSnapshot = repository.findHealthSnapshotById(snapshotId)
                .filter(snapshot -> snapshot.connectorId().equals(connectorId))
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "连接器健康快照不存在"
                ));
        if (healthSnapshot.healthy()) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "健康状态正常的快照无需人工确认异常"
            );
        }
        ConnectorContext context = contextProvider.currentContext();
        ConnectorHealthSnapshot confirmedSnapshot = healthSnapshot.confirmAbnormal(context.operatorId(), note, now());
        repository.saveHealthSnapshot(confirmedSnapshot);
        return confirmedSnapshot.toView();
    }

    private ConnectorHealthSnapshotView runConnectivityCheck(String connectorId, ConnectorCheckType checkType) {
        ConnectorDefinition connectorDefinition = loadRequiredConnector(connectorId);
        validateParameters(connectorDefinition, connectorDefinition.parameters());
        ConnectorContext context = contextProvider.currentContext();
        Instant checkedAt = now();
        Optional<ConnectorHealthSnapshot> latestSnapshot = latestSnapshot(connectorId, checkType);
        if (latestSnapshot.isPresent()
                && latestSnapshot.get().changeSequence() == connectorDefinition.changeSequence()
                && latestSnapshot.get().targetEnvironment().equals(context.environment())
                && !latestSnapshot.get().checkedAt().isBefore(checkedAt.minus(CONNECTIVITY_CHECK_DEDUP_WINDOW))) {
            return latestSnapshot.get().toView();
        }
        ConnectorDriver connectorDriver = driverFor(connectorDefinition);
        ConnectorTestResult testResult = connectorDriver.testConnection(connectorDefinition);
        ConnectorHealthSnapshot snapshot = ConnectorHealthSnapshot.from(
                UUID.randomUUID().toString(),
                connectorDefinition.connectorId(),
                checkType,
                context.operatorId(),
                context.environment(),
                connectorDefinition.changeSequence(),
                testResult,
                checkedAt
        );
        repository.saveHealthSnapshot(snapshot);
        return snapshot.toView();
    }

    private ConnectorDefinition loadRequiredConnector(String connectorId) {
        return repository.findById(connectorId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "连接器不存在"
                ));
    }

    private ConnectorDriver driverFor(ConnectorDefinition connectorDefinition) {
        return connectorDriverRegistry.driverFor(connectorDefinition.connectorType())
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                        "当前阶段仅支持 HTTP / DATABASE / MQ 三类基础连接器"
                ));
    }

    private void ensureSupportedConnectorType(ConnectorType connectorType) {
        if (!connectorDriverRegistry.supports(connectorType)) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "当前阶段仅支持 HTTP / DATABASE / MQ 三类基础连接器"
            );
        }
    }

    private void validateAuthMode(ConnectorDefinition connectorDefinition) {
        ConnectorDriver connectorDriver = driverFor(connectorDefinition);
        if (!connectorDriver.supportedAuthModes().contains(connectorDefinition.authMode())) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "连接器认证方式与当前连接器类型不兼容"
            );
        }
    }

    private void ensureCodeUniqueness(String tenantId, String code, String connectorId) {
        repository.findByCode(tenantId, code)
                .filter(existing -> !existing.connectorId().equals(connectorId))
                .ifPresent(existing -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "连接器编码已存在");
                });
    }

    private List<ConnectorParameter> toDomainParameters(
            String connectorId,
            List<ConnectorParameterValue> parameterValues
    ) {
        if (parameterValues == null) {
            return List.of();
        }
        return parameterValues.stream()
                .map(parameterValue -> ConnectorParameter.of(
                        UUID.randomUUID().toString(),
                        connectorId,
                        parameterValue.paramKey(),
                        parameterValue.paramValueRef(),
                        parameterValue.sensitive()
                ))
                .toList();
    }

    private void validateParameters(ConnectorDefinition connectorDefinition, List<ConnectorParameter> parameters) {
        ConnectorDriver connectorDriver = driverFor(connectorDefinition);
        validateAuthMode(connectorDefinition);
        List<ConnectorParameterTemplate> templates = connectorDriver.parameterTemplates(connectorDefinition);
        Map<String, ConnectorParameterTemplate> templateMap = new HashMap<>();
        for (ConnectorParameterTemplate template : templates) {
            templateMap.put(template.paramKey(), template);
        }
        Map<String, ConnectorParameter> parameterMap = new HashMap<>();
        for (ConnectorParameter parameter : parameters) {
            if (parameterMap.put(parameter.paramKey(), parameter) != null) {
                throw new BizException(SharedErrorDescriptors.CONFLICT, "连接参数键重复: " + parameter.paramKey());
            }
            ConnectorParameterTemplate template = templateMap.get(parameter.paramKey());
            if (template == null) {
                throw new BizException(
                        SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                        "存在未定义的连接参数: " + parameter.paramKey()
                );
            }
            if (template.sensitive() != parameter.sensitive()) {
                throw new BizException(
                        SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                        "连接参数敏感标记不符合模板要求: " + parameter.paramKey()
                );
            }
        }
        List<String> missingRequiredKeys = templates.stream()
                .filter(ConnectorParameterTemplate::required)
                .map(ConnectorParameterTemplate::paramKey)
                .filter(requiredKey -> !parameterMap.containsKey(requiredKey))
                .sorted()
                .toList();
        if (!missingRequiredKeys.isEmpty()) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "连接参数缺失: " + String.join(", ", missingRequiredKeys)
            );
        }
        if (connectorDefinition.status() == ConnectorStatus.ACTIVE && parameters.isEmpty()) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "启用中的连接器不能清空参数");
        }
    }

    private void publishIfChanged(ConnectorDefinition connectorDefinition, List<String> changedFields, Instant occurredAt) {
        if (changedFields.isEmpty()) {
            return;
        }
        domainEventPublisher.publish(DataConnectorUpdatedEvent.from(connectorDefinition, changedFields, occurredAt));
    }

    private List<String> changedFields(ConnectorDefinition before, ConnectorDefinition after) {
        if (before == null) {
            return List.of(
                    "code",
                    "name",
                    "connectorType",
                    "vendor",
                    "protocol",
                    "authMode",
                    "timeoutConfig",
                    "status"
            );
        }
        List<String> changedFields = new ArrayList<>();
        if (!before.code().equals(after.code())) {
            changedFields.add("code");
        }
        if (!before.name().equals(after.name())) {
            changedFields.add("name");
        }
        if (before.connectorType() != after.connectorType()) {
            changedFields.add("connectorType");
        }
        if (!Objects.equals(before.vendor(), after.vendor())) {
            changedFields.add("vendor");
        }
        if (!Objects.equals(before.protocol(), after.protocol())) {
            changedFields.add("protocol");
        }
        if (before.authMode() != after.authMode()) {
            changedFields.add("authMode");
        }
        if (!Objects.equals(before.timeoutConfig(), after.timeoutConfig())) {
            changedFields.add("timeoutConfig");
        }
        if (!before.parameters().equals(after.parameters())) {
            changedFields.add("parameters");
        }
        if (before.status() != after.status()) {
            changedFields.add("status");
        }
        return List.copyOf(changedFields);
    }

    private Instant now() {
        return clock.instant();
    }

    private Optional<ConnectorHealthSnapshot> latestSnapshot(String connectorId, ConnectorCheckType checkType) {
        return repository.findLatestSnapshot(connectorId, checkType);
    }

    private int normalizeLimit(int limit) {
        return limit <= 0 ? 10 : Math.min(limit, 50);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
