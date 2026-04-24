package com.hjo2oa.data.service.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record DataServiceDefinition(
        UUID serviceId,
        UUID tenantId,
        String code,
        String name,
        ServiceType serviceType,
        SourceMode sourceMode,
        PermissionMode permissionMode,
        PermissionBoundary permissionBoundary,
        CachePolicy cachePolicy,
        Status status,
        String sourceRef,
        String connectorId,
        String description,
        int statusSequence,
        Instant activatedAt,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt,
        List<ServiceParameterDefinition> parameters,
        List<ServiceFieldMapping> fieldMappings
) {

    public DataServiceDefinition {
        Objects.requireNonNull(serviceId, "serviceId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        code = requireText(code, "code");
        name = requireText(name, "name");
        Objects.requireNonNull(serviceType, "serviceType must not be null");
        Objects.requireNonNull(sourceMode, "sourceMode must not be null");
        Objects.requireNonNull(permissionMode, "permissionMode must not be null");
        permissionBoundary = permissionBoundary == null ? PermissionBoundary.none() : permissionBoundary;
        cachePolicy = cachePolicy == null ? CachePolicy.disabled() : cachePolicy;
        Objects.requireNonNull(status, "status must not be null");
        sourceRef = requireText(sourceRef, "sourceRef");
        connectorId = normalizeNullableText(connectorId);
        description = normalizeNullableText(description);
        if (statusSequence < 0) {
            throw new IllegalArgumentException("statusSequence must not be negative");
        }
        createdBy = requireText(createdBy, "createdBy");
        updatedBy = requireText(updatedBy, "updatedBy");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        parameters = immutableParameters(serviceId, parameters);
        fieldMappings = immutableFieldMappings(serviceId, fieldMappings);
        permissionBoundary.validate(permissionMode);
        cachePolicy.validate(serviceType);
        validateSourceBinding(sourceMode, connectorId);
        validateStatus(status, activatedAt);
        validateParameters(parameters);
        validateFieldMappings(fieldMappings);
    }

    public static DataServiceDefinition create(
            UUID serviceId,
            UUID tenantId,
            String code,
            String name,
            ServiceType serviceType,
            SourceMode sourceMode,
            PermissionMode permissionMode,
            PermissionBoundary permissionBoundary,
            CachePolicy cachePolicy,
            String sourceRef,
            String connectorId,
            String description,
            String operatorId,
            Instant now,
            List<ServiceParameterDefinition> parameters,
            List<ServiceFieldMapping> fieldMappings
    ) {
        return new DataServiceDefinition(
                serviceId,
                tenantId,
                code,
                name,
                serviceType,
                sourceMode,
                permissionMode,
                permissionBoundary,
                cachePolicy,
                Status.DRAFT,
                sourceRef,
                connectorId,
                description,
                0,
                null,
                operatorId,
                operatorId,
                now,
                now,
                parameters,
                fieldMappings
        );
    }

    public DataServiceDefinition update(
            String code,
            String name,
            ServiceType serviceType,
            SourceMode sourceMode,
            PermissionMode permissionMode,
            PermissionBoundary permissionBoundary,
            CachePolicy cachePolicy,
            String sourceRef,
            String connectorId,
            String description,
            String operatorId,
            Instant now,
            List<ServiceParameterDefinition> parameters,
            List<ServiceFieldMapping> fieldMappings
    ) {
        return new DataServiceDefinition(
                serviceId,
                tenantId,
                code,
                name,
                serviceType,
                sourceMode,
                permissionMode,
                permissionBoundary,
                cachePolicy,
                status,
                sourceRef,
                connectorId,
                description,
                statusSequence,
                activatedAt,
                createdBy,
                operatorId,
                createdAt,
                now,
                parameters,
                fieldMappings
        );
    }

    public DataServiceDefinition activate(String operatorId, Instant now) {
        if (status == Status.ACTIVE) {
            return this;
        }
        return new DataServiceDefinition(
                serviceId,
                tenantId,
                code,
                name,
                serviceType,
                sourceMode,
                permissionMode,
                permissionBoundary,
                cachePolicy,
                Status.ACTIVE,
                sourceRef,
                connectorId,
                description,
                statusSequence + 1,
                now,
                createdBy,
                operatorId,
                createdAt,
                now,
                parameters,
                fieldMappings
        );
    }

    public DataServiceDefinition disable(String operatorId, Instant now) {
        if (status == Status.DISABLED) {
            return this;
        }
        return new DataServiceDefinition(
                serviceId,
                tenantId,
                code,
                name,
                serviceType,
                sourceMode,
                permissionMode,
                permissionBoundary,
                cachePolicy,
                Status.DISABLED,
                sourceRef,
                connectorId,
                description,
                statusSequence + 1,
                activatedAt,
                createdBy,
                operatorId,
                createdAt,
                now,
                parameters,
                fieldMappings
        );
    }

    public DataServiceDefinition upsertParameter(ServiceParameterDefinition parameter, String operatorId, Instant now) {
        Objects.requireNonNull(parameter, "parameter must not be null");
        List<ServiceParameterDefinition> updatedParameters = new ArrayList<>(parameters);
        updatedParameters.removeIf(existing -> sameParamCode(existing.paramCode(), parameter.paramCode()));
        updatedParameters.add(parameter);
        return new DataServiceDefinition(
                serviceId,
                tenantId,
                code,
                name,
                serviceType,
                sourceMode,
                permissionMode,
                permissionBoundary,
                cachePolicy,
                status,
                sourceRef,
                connectorId,
                description,
                statusSequence,
                activatedAt,
                createdBy,
                operatorId,
                createdAt,
                now,
                updatedParameters,
                fieldMappings
        );
    }

    public DataServiceDefinition upsertFieldMapping(ServiceFieldMapping fieldMapping, String operatorId, Instant now) {
        Objects.requireNonNull(fieldMapping, "fieldMapping must not be null");
        List<ServiceFieldMapping> updatedMappings = new ArrayList<>(fieldMappings);
        updatedMappings.removeIf(existing -> existing.mappingId().equals(fieldMapping.mappingId())
                || sameFieldPair(existing, fieldMapping));
        updatedMappings.add(fieldMapping);
        return new DataServiceDefinition(
                serviceId,
                tenantId,
                code,
                name,
                serviceType,
                sourceMode,
                permissionMode,
                permissionBoundary,
                cachePolicy,
                status,
                sourceRef,
                connectorId,
                description,
                statusSequence,
                activatedAt,
                createdBy,
                operatorId,
                createdAt,
                now,
                parameters,
                updatedMappings
        );
    }

    public boolean active() {
        return status == Status.ACTIVE;
    }

    public boolean reusableByOpenApi() {
        return active();
    }

    public boolean isReusableByOpenApi() {
        return reusableByOpenApi();
    }

    public boolean reusableByReport() {
        return active() && (serviceType == ServiceType.QUERY || serviceType == ServiceType.EXPORT);
    }

    public boolean isReusableByReport() {
        return reusableByReport();
    }

    public DataServiceViews.SummaryView toSummaryView(
            int openApiReferenceCount,
            int reportReferenceCount,
            int syncReferenceCount
    ) {
        return new DataServiceViews.SummaryView(
                serviceId,
                tenantId,
                code,
                name,
                serviceType,
                sourceMode,
                permissionMode,
                status,
                cachePolicy.enabled(),
                activatedAt,
                openApiReferenceCount,
                reportReferenceCount,
                syncReferenceCount,
                reusableByOpenApi(),
                reusableByReport(),
                createdAt,
                updatedAt
        );
    }

    public DataServiceViews.DetailView toDetailView() {
        return new DataServiceViews.DetailView(
                serviceId,
                tenantId,
                code,
                name,
                serviceType,
                sourceMode,
                permissionMode,
                permissionBoundary,
                cachePolicy,
                status,
                sourceRef,
                connectorId,
                description,
                statusSequence,
                activatedAt,
                createdBy,
                updatedBy,
                createdAt,
                updatedAt,
                reusableByOpenApi(),
                reusableByReport(),
                parameters.stream()
                        .sorted((left, right) -> Integer.compare(left.sortOrder(), right.sortOrder()))
                        .map(ServiceParameterDefinition::toView)
                        .toList(),
                fieldMappings.stream()
                        .sorted((left, right) -> Integer.compare(left.sortOrder(), right.sortOrder()))
                        .map(ServiceFieldMapping::toView)
                        .toList()
        );
    }

    public DataServiceViews.ReusableView toReusableView() {
        return new DataServiceViews.ReusableView(
                serviceId,
                tenantId,
                code,
                name,
                serviceType,
                sourceMode,
                permissionMode,
                cachePolicy,
                activatedAt,
                reusableByOpenApi(),
                reusableByReport()
        );
    }

    public enum ServiceType {
        QUERY,
        COMMAND,
        EXPORT,
        CALLBACK
    }

    public enum SourceMode {
        INTERNAL_QUERY,
        CONNECTOR,
        MIXED
    }

    public enum PermissionMode {
        PUBLIC_INTERNAL,
        APP_SCOPED,
        SUBJECT_SCOPED
    }

    public enum Status {
        DRAFT,
        ACTIVE,
        DEPRECATED,
        DISABLED
    }

    public enum CacheScope {
        TENANT,
        APP,
        SUBJECT,
        GLOBAL
    }

    public record PermissionBoundary(
            List<String> allowedAppCodes,
            List<String> allowedSubjectIds,
            List<String> requiredRoles
    ) {

        public PermissionBoundary {
            allowedAppCodes = immutableNormalizedList(allowedAppCodes);
            allowedSubjectIds = immutableNormalizedList(allowedSubjectIds);
            requiredRoles = immutableUpperRoleList(requiredRoles);
        }

        public static PermissionBoundary none() {
            return new PermissionBoundary(List.of(), List.of(), List.of());
        }

        public void validate(PermissionMode permissionMode) {
            Objects.requireNonNull(permissionMode, "permissionMode must not be null");
            if (permissionMode == PermissionMode.PUBLIC_INTERNAL) {
                if (!allowedAppCodes.isEmpty() || !allowedSubjectIds.isEmpty()) {
                    throw new IllegalArgumentException(
                            "PUBLIC_INTERNAL service must not declare app or subject restrictions"
                    );
                }
            }
            if (permissionMode == PermissionMode.APP_SCOPED && allowedAppCodes.isEmpty()) {
                throw new IllegalArgumentException("APP_SCOPED service must declare allowedAppCodes");
            }
            if (permissionMode == PermissionMode.SUBJECT_SCOPED && allowedSubjectIds.isEmpty()) {
                throw new IllegalArgumentException("SUBJECT_SCOPED service must declare allowedSubjectIds");
            }
        }
    }

    public record CachePolicy(
            boolean enabled,
            Long ttlSeconds,
            String cacheKeyTemplate,
            CacheScope scope,
            boolean cacheNullValue,
            List<String> invalidationEvents
    ) {

        public CachePolicy {
            cacheKeyTemplate = normalizeNullableText(cacheKeyTemplate);
            scope = scope == null ? CacheScope.TENANT : scope;
            invalidationEvents = immutableNormalizedList(invalidationEvents);
            if (!enabled) {
                ttlSeconds = null;
                cacheKeyTemplate = null;
            } else {
                if (ttlSeconds == null || ttlSeconds < 1) {
                    throw new IllegalArgumentException("ttlSeconds must be greater than 0 when cache is enabled");
                }
                if (cacheKeyTemplate == null) {
                    throw new IllegalArgumentException("cacheKeyTemplate is required when cache is enabled");
                }
            }
        }

        public static CachePolicy disabled() {
            return new CachePolicy(false, null, null, CacheScope.TENANT, false, List.of());
        }

        public void validate(ServiceType serviceType) {
            Objects.requireNonNull(serviceType, "serviceType must not be null");
            if (enabled && serviceType != ServiceType.QUERY && serviceType != ServiceType.EXPORT) {
                throw new IllegalArgumentException("Only QUERY or EXPORT service can enable cache");
            }
        }

        public String resolveCacheKey(
                UUID tenantId,
                String serviceCode,
                String appCode,
                String subjectId,
                Map<String, Object> parameters
        ) {
            if (!enabled) {
                return null;
            }
            String resolved = cacheKeyTemplate;
            resolved = replaceToken(resolved, "{tenantId}", tenantId == null ? null : tenantId.toString());
            resolved = replaceToken(resolved, "{serviceCode}", serviceCode);
            resolved = replaceToken(resolved, "{appCode}", appCode);
            resolved = replaceToken(resolved, "{subjectId}", subjectId);
            if (parameters != null && !parameters.isEmpty()) {
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    resolved = replaceToken(
                            resolved,
                            "{param." + entry.getKey() + "}",
                            entry.getValue() == null ? null : String.valueOf(entry.getValue())
                    );
                }
            }
            return resolved;
        }

        private static String replaceToken(String source, String token, String replacement) {
            String resolvedReplacement = replacement == null ? "NA" : replacement;
            return source.replace(token, resolvedReplacement);
        }
    }

    private static List<ServiceParameterDefinition> immutableParameters(
            UUID serviceId,
            List<ServiceParameterDefinition> values
    ) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<ServiceParameterDefinition> copied = new ArrayList<>(values.size());
        for (ServiceParameterDefinition value : values) {
            if (value == null) {
                continue;
            }
            if (!serviceId.equals(value.serviceId())) {
                throw new IllegalArgumentException("parameter serviceId must match aggregate serviceId");
            }
            copied.add(value);
        }
        return List.copyOf(copied);
    }

    private static List<ServiceFieldMapping> immutableFieldMappings(
            UUID serviceId,
            List<ServiceFieldMapping> values
    ) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<ServiceFieldMapping> copied = new ArrayList<>(values.size());
        for (ServiceFieldMapping value : values) {
            if (value == null) {
                continue;
            }
            if (!serviceId.equals(value.serviceId())) {
                throw new IllegalArgumentException("fieldMapping serviceId must match aggregate serviceId");
            }
            copied.add(value);
        }
        return List.copyOf(copied);
    }

    private static void validateStatus(Status status, Instant activatedAt) {
        if (status == Status.ACTIVE && activatedAt == null) {
            throw new IllegalArgumentException("activatedAt is required when service status is ACTIVE");
        }
    }

    private static void validateSourceBinding(SourceMode sourceMode, String connectorId) {
        if (sourceMode == SourceMode.INTERNAL_QUERY && connectorId != null) {
            throw new IllegalArgumentException("INTERNAL_QUERY service must not bind connectorId");
        }
        if ((sourceMode == SourceMode.CONNECTOR || sourceMode == SourceMode.MIXED) && connectorId == null) {
            throw new IllegalArgumentException(sourceMode + " service must bind connectorId");
        }
    }

    private static void validateParameters(List<ServiceParameterDefinition> values) {
        Set<String> codes = new LinkedHashSet<>();
        int pageableCount = 0;
        for (ServiceParameterDefinition value : values) {
            String codeKey = value.paramCode().toUpperCase(Locale.ROOT);
            if (!codes.add(codeKey)) {
                throw new IllegalArgumentException("duplicate parameter code: " + value.paramCode());
            }
            if (value.enabled() && value.pageable()) {
                pageableCount++;
            }
        }
        if (pageableCount > 1) {
            throw new IllegalArgumentException("only one enabled PAGEABLE parameter is allowed");
        }
    }

    private static void validateFieldMappings(List<ServiceFieldMapping> values) {
        Set<String> targets = new LinkedHashSet<>();
        Set<String> pairs = new LinkedHashSet<>();
        for (ServiceFieldMapping value : values) {
            String targetKey = value.targetField().toUpperCase(Locale.ROOT);
            if (!targets.add(targetKey)) {
                throw new IllegalArgumentException("duplicate target field: " + value.targetField());
            }
            String pairKey = pairKey(value.sourceField(), value.targetField());
            if (!pairs.add(pairKey)) {
                throw new IllegalArgumentException(
                        "duplicate field mapping pair: " + value.sourceField() + " -> " + value.targetField()
                );
            }
        }
    }

    private static boolean sameParamCode(String left, String right) {
        return pairKey(left, "").equals(pairKey(right, ""));
    }

    private static boolean sameFieldPair(ServiceFieldMapping left, ServiceFieldMapping right) {
        return pairKey(left.sourceField(), left.targetField()).equals(pairKey(right.sourceField(), right.targetField()));
    }

    private static String pairKey(String left, String right) {
        return (left == null ? "" : left.trim().toUpperCase(Locale.ROOT))
                + "->"
                + (right == null ? "" : right.trim().toUpperCase(Locale.ROOT));
    }

    private static List<String> immutableNormalizedList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String candidate = normalizeNullableText(value);
            if (candidate != null) {
                normalized.add(candidate);
            }
        }
        return List.copyOf(normalized);
    }

    private static List<String> immutableUpperRoleList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String candidate = normalizeNullableText(value);
            if (candidate != null) {
                normalized.add(candidate.toUpperCase(Locale.ROOT));
            }
        }
        return List.copyOf(normalized);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
