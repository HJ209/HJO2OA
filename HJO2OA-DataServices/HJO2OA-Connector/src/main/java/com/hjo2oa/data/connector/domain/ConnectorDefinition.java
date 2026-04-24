package com.hjo2oa.data.connector.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ConnectorDefinition(
        String connectorId,
        String tenantId,
        String code,
        String name,
        ConnectorType connectorType,
        String vendor,
        String protocol,
        ConnectorAuthMode authMode,
        TimeoutRetryConfig timeoutConfig,
        ConnectorStatus status,
        long changeSequence,
        List<ConnectorParameter> parameters,
        Instant createdAt,
        Instant updatedAt
) {

    public ConnectorDefinition {
        connectorId = requireText(connectorId, "connectorId");
        tenantId = requireText(tenantId, "tenantId");
        code = requireText(code, "code");
        name = requireText(name, "name");
        Objects.requireNonNull(connectorType, "connectorType must not be null");
        vendor = normalizeNullable(vendor);
        protocol = normalizeNullable(protocol);
        Objects.requireNonNull(authMode, "authMode must not be null");
        timeoutConfig = timeoutConfig == null ? TimeoutRetryConfig.defaultConfig() : timeoutConfig;
        Objects.requireNonNull(status, "status must not be null");
        if (changeSequence < 0) {
            throw new IllegalArgumentException("changeSequence must not be negative");
        }
        parameters = normalizeParameters(connectorId, parameters);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ConnectorDefinition create(
            String connectorId,
            String tenantId,
            String code,
            String name,
            ConnectorType connectorType,
            String vendor,
            String protocol,
            ConnectorAuthMode authMode,
            TimeoutRetryConfig timeoutConfig,
            Instant now
    ) {
        return new ConnectorDefinition(
                connectorId,
                tenantId,
                code,
                name,
                connectorType,
                vendor,
                protocol,
                authMode,
                timeoutConfig,
                ConnectorStatus.DRAFT,
                0L,
                List.of(),
                now,
                now
        );
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    public ConnectorDefinition updateMetadata(
            String code,
            String name,
            ConnectorType connectorType,
            String vendor,
            String protocol,
            ConnectorAuthMode authMode,
            TimeoutRetryConfig timeoutConfig,
            Instant now
    ) {
        boolean changed = !this.code.equals(code)
                || !this.name.equals(name)
                || this.connectorType != connectorType
                || !Objects.equals(this.vendor, normalizeNullable(vendor))
                || !Objects.equals(this.protocol, normalizeNullable(protocol))
                || this.authMode != authMode
                || !Objects.equals(this.timeoutConfig, timeoutConfig == null
                ? TimeoutRetryConfig.defaultConfig()
                : timeoutConfig);
        if (!changed) {
            return this;
        }
        return new ConnectorDefinition(
                connectorId,
                tenantId,
                code,
                name,
                connectorType,
                vendor,
                protocol,
                authMode,
                timeoutConfig,
                downgradeStatusOnDefinitionChange(status),
                changeSequence + 1,
                parameters,
                createdAt,
                now
        );
    }

    public ConnectorDefinition configureParameters(List<ConnectorParameter> newParameters, Instant now) {
        List<ConnectorParameter> normalizedParameters = normalizeParameters(connectorId, newParameters);
        if (parameters.equals(normalizedParameters)) {
            return this;
        }
        return new ConnectorDefinition(
                connectorId,
                tenantId,
                code,
                name,
                connectorType,
                vendor,
                protocol,
                authMode,
                timeoutConfig,
                downgradeStatusOnDefinitionChange(status),
                changeSequence + 1,
                normalizedParameters,
                createdAt,
                now
        );
    }

    public ConnectorDefinition activate(Instant now) {
        if (status == ConnectorStatus.ACTIVE) {
            return this;
        }
        return new ConnectorDefinition(
                connectorId,
                tenantId,
                code,
                name,
                connectorType,
                vendor,
                protocol,
                authMode,
                timeoutConfig,
                ConnectorStatus.ACTIVE,
                changeSequence + 1,
                parameters,
                createdAt,
                now
        );
    }

    public ConnectorDefinition disable(Instant now) {
        if (status == ConnectorStatus.DISABLED) {
            return this;
        }
        return new ConnectorDefinition(
                connectorId,
                tenantId,
                code,
                name,
                connectorType,
                vendor,
                protocol,
                authMode,
                timeoutConfig,
                ConnectorStatus.DISABLED,
                changeSequence + 1,
                parameters,
                createdAt,
                now
        );
    }

    public ConnectorDefinitionView toView(
            ConnectorHealthSnapshot latestTestSnapshot,
            ConnectorHealthSnapshot latestHealthSnapshot
    ) {
        return new ConnectorDefinitionView(
                connectorId,
                tenantId,
                code,
                name,
                connectorType,
                vendor,
                protocol,
                authMode,
                timeoutConfig,
                status,
                changeSequence,
                parameters.stream().map(ConnectorParameter::toView).toList(),
                latestTestSnapshot == null ? null : latestTestSnapshot.toView(),
                latestHealthSnapshot == null ? null : latestHealthSnapshot.toView(),
                createdAt,
                updatedAt
        );
    }

    public ConnectorSummaryView toSummaryView(
            ConnectorHealthSnapshot latestTestSnapshot,
            ConnectorHealthSnapshot latestHealthSnapshot
    ) {
        return new ConnectorSummaryView(
                connectorId,
                tenantId,
                code,
                name,
                connectorType,
                vendor,
                protocol,
                authMode,
                timeoutConfig,
                status,
                changeSequence,
                latestTestSnapshot == null ? null : latestTestSnapshot.toView(),
                latestHealthSnapshot == null ? null : latestHealthSnapshot.toView(),
                createdAt,
                updatedAt
        );
    }

    private static ConnectorStatus downgradeStatusOnDefinitionChange(ConnectorStatus currentStatus) {
        return currentStatus == ConnectorStatus.ACTIVE ? ConnectorStatus.DRAFT : currentStatus;
    }

    private static List<ConnectorParameter> normalizeParameters(String connectorId, List<ConnectorParameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return List.of();
        }
        return parameters.stream()
                .map(parameter -> new ConnectorParameter(
                        parameter.parameterId(),
                        connectorId,
                        parameter.paramKey(),
                        parameter.paramValueRef(),
                        parameter.sensitive()
                ))
                .sorted(Comparator.comparing(ConnectorParameter::paramKey))
                .toList();
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
