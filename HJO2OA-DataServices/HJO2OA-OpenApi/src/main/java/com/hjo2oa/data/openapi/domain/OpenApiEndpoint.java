package com.hjo2oa.data.openapi.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record OpenApiEndpoint(
        String apiId,
        String tenantId,
        String code,
        String name,
        String dataServiceId,
        String dataServiceCode,
        String dataServiceName,
        String path,
        OpenApiHttpMethod httpMethod,
        String version,
        OpenApiAuthType authType,
        String compatibilityNotes,
        OpenApiStatus status,
        Instant publishedAt,
        Instant deprecatedAt,
        Instant sunsetAt,
        Instant createdAt,
        Instant updatedAt,
        List<ApiCredentialGrant> credentialGrants,
        List<ApiRateLimitPolicy> rateLimitPolicies
) {

    public OpenApiEndpoint {
        apiId = requireText(apiId, "apiId");
        tenantId = requireText(tenantId, "tenantId");
        code = requireText(code, "code");
        name = requireText(name, "name");
        dataServiceId = requireText(dataServiceId, "dataServiceId");
        dataServiceCode = requireText(dataServiceCode, "dataServiceCode");
        dataServiceName = requireText(dataServiceName, "dataServiceName");
        path = requirePath(path);
        Objects.requireNonNull(httpMethod, "httpMethod must not be null");
        version = requireText(version, "version");
        Objects.requireNonNull(authType, "authType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        credentialGrants = credentialGrants == null ? List.of() : List.copyOf(credentialGrants);
        rateLimitPolicies = rateLimitPolicies == null ? List.of() : List.copyOf(rateLimitPolicies);
    }

    public static OpenApiEndpoint create(
            String tenantId,
            String code,
            String name,
            String dataServiceId,
            String dataServiceCode,
            String dataServiceName,
            String path,
            OpenApiHttpMethod httpMethod,
            String version,
            OpenApiAuthType authType,
            String compatibilityNotes,
            Instant now
    ) {
        return new OpenApiEndpoint(
                UUID.randomUUID().toString(),
                tenantId,
                code,
                name,
                dataServiceId,
                dataServiceCode,
                dataServiceName,
                path,
                httpMethod,
                version,
                authType,
                compatibilityNotes,
                OpenApiStatus.DRAFT,
                null,
                null,
                null,
                now,
                now,
                List.of(),
                List.of()
        );
    }

    public OpenApiEndpoint updateDraft(
            String name,
            String dataServiceId,
            String dataServiceCode,
            String dataServiceName,
            String path,
            OpenApiHttpMethod httpMethod,
            OpenApiAuthType authType,
            String compatibilityNotes,
            Instant now
    ) {
        if (status != OpenApiStatus.DRAFT) {
            boolean sameDefinition = this.name.equals(name)
                    && this.dataServiceId.equals(dataServiceId)
                    && this.dataServiceCode.equals(dataServiceCode)
                    && this.dataServiceName.equals(dataServiceName)
                    && this.path.equals(path)
                    && this.httpMethod == httpMethod
                    && this.authType == authType
                    && Objects.equals(this.compatibilityNotes, compatibilityNotes);
            if (!sameDefinition) {
                throw new IllegalStateException("Published or offlined API version cannot be overwritten");
            }
            return this;
        }
        return new OpenApiEndpoint(
                apiId,
                tenantId,
                code,
                name,
                dataServiceId,
                dataServiceCode,
                dataServiceName,
                path,
                httpMethod,
                version,
                authType,
                compatibilityNotes,
                status,
                publishedAt,
                deprecatedAt,
                sunsetAt,
                createdAt,
                now,
                credentialGrants,
                rateLimitPolicies
        );
    }

    public OpenApiEndpoint publish(Instant now) {
        if (status == OpenApiStatus.ACTIVE) {
            return this;
        }
        return new OpenApiEndpoint(
                apiId,
                tenantId,
                code,
                name,
                dataServiceId,
                dataServiceCode,
                dataServiceName,
                path,
                httpMethod,
                version,
                authType,
                compatibilityNotes,
                OpenApiStatus.ACTIVE,
                publishedAt == null ? now : publishedAt,
                null,
                null,
                createdAt,
                now,
                credentialGrants,
                rateLimitPolicies
        );
    }

    public OpenApiEndpoint deprecate(Instant now, Instant sunsetAt) {
        if (status == OpenApiStatus.DEPRECATED && Objects.equals(this.sunsetAt, sunsetAt)) {
            return this;
        }
        return new OpenApiEndpoint(
                apiId,
                tenantId,
                code,
                name,
                dataServiceId,
                dataServiceCode,
                dataServiceName,
                path,
                httpMethod,
                version,
                authType,
                compatibilityNotes,
                OpenApiStatus.DEPRECATED,
                publishedAt,
                now,
                sunsetAt,
                createdAt,
                now,
                credentialGrants,
                rateLimitPolicies
        );
    }

    public OpenApiEndpoint offline(Instant now) {
        if (status == OpenApiStatus.OFFLINE) {
            return this;
        }
        return new OpenApiEndpoint(
                apiId,
                tenantId,
                code,
                name,
                dataServiceId,
                dataServiceCode,
                dataServiceName,
                path,
                httpMethod,
                version,
                authType,
                compatibilityNotes,
                OpenApiStatus.OFFLINE,
                publishedAt,
                deprecatedAt,
                sunsetAt,
                createdAt,
                now,
                credentialGrants,
                rateLimitPolicies
        );
    }

    public OpenApiEndpoint withCredential(ApiCredentialGrant credentialGrant) {
        List<ApiCredentialGrant> nextCredentials = credentialGrants.stream()
                .filter(existing -> !existing.clientCode().equals(credentialGrant.clientCode()))
                .toList();
        nextCredentials = new java.util.ArrayList<>(nextCredentials);
        nextCredentials.add(credentialGrant);
        nextCredentials.sort(Comparator.comparing(ApiCredentialGrant::clientCode));
        return new OpenApiEndpoint(
                apiId,
                tenantId,
                code,
                name,
                dataServiceId,
                dataServiceCode,
                dataServiceName,
                path,
                httpMethod,
                version,
                authType,
                compatibilityNotes,
                status,
                publishedAt,
                deprecatedAt,
                sunsetAt,
                createdAt,
                credentialGrant.updatedAt(),
                List.copyOf(nextCredentials),
                rateLimitPolicies
        );
    }

    public OpenApiEndpoint revokeCredential(String clientCode, Instant now) {
        List<ApiCredentialGrant> nextCredentials = credentialGrants.stream()
                .map(existing -> existing.clientCode().equals(clientCode) ? existing.revoke(now) : existing)
                .toList();
        return new OpenApiEndpoint(
                apiId,
                tenantId,
                code,
                name,
                dataServiceId,
                dataServiceCode,
                dataServiceName,
                path,
                httpMethod,
                version,
                authType,
                compatibilityNotes,
                status,
                publishedAt,
                deprecatedAt,
                sunsetAt,
                createdAt,
                now,
                nextCredentials,
                rateLimitPolicies
        );
    }

    public OpenApiEndpoint withPolicy(ApiRateLimitPolicy policy) {
        List<ApiRateLimitPolicy> nextPolicies = rateLimitPolicies.stream()
                .filter(existing -> !(existing.policyCode().equals(policy.policyCode())
                        && Objects.equals(existing.clientCode(), policy.clientCode())))
                .toList();
        nextPolicies = new java.util.ArrayList<>(nextPolicies);
        nextPolicies.add(policy);
        nextPolicies.sort(Comparator.comparing(ApiRateLimitPolicy::policyCode)
                .thenComparing(policyItem -> policyItem.clientCode() == null ? "" : policyItem.clientCode()));
        return new OpenApiEndpoint(
                apiId,
                tenantId,
                code,
                name,
                dataServiceId,
                dataServiceCode,
                dataServiceName,
                path,
                httpMethod,
                version,
                authType,
                compatibilityNotes,
                status,
                publishedAt,
                deprecatedAt,
                sunsetAt,
                createdAt,
                policy.updatedAt(),
                credentialGrants,
                List.copyOf(nextPolicies)
        );
    }

    public OpenApiEndpoint disablePolicy(String policyCode, String clientCode, Instant now) {
        List<ApiRateLimitPolicy> nextPolicies = rateLimitPolicies.stream()
                .map(existing -> existing.policyCode().equals(policyCode)
                        && Objects.equals(existing.clientCode(), normalizeNullable(clientCode))
                        ? existing.disable(now)
                        : existing)
                .toList();
        return new OpenApiEndpoint(
                apiId,
                tenantId,
                code,
                name,
                dataServiceId,
                dataServiceCode,
                dataServiceName,
                path,
                httpMethod,
                version,
                authType,
                compatibilityNotes,
                status,
                publishedAt,
                deprecatedAt,
                sunsetAt,
                createdAt,
                now,
                credentialGrants,
                nextPolicies
        );
    }

    public Optional<ApiCredentialGrant> credentialFor(String clientCode) {
        return credentialGrants.stream()
                .filter(credentialGrant -> credentialGrant.clientCode().equals(clientCode))
                .findFirst();
    }

    public List<ApiRateLimitPolicy> activePoliciesFor(String clientCode) {
        return rateLimitPolicies.stream()
                .filter(ApiRateLimitPolicy::isEnabled)
                .filter(policy -> policy.appliesToClient(clientCode))
                .toList();
    }

    public boolean canDelete() {
        return status == OpenApiStatus.DRAFT || status == OpenApiStatus.OFFLINE;
    }

    public boolean isCallableAt(Instant now) {
        if (status == OpenApiStatus.ACTIVE) {
            return true;
        }
        if (status == OpenApiStatus.DEPRECATED) {
            return sunsetAt == null || now.isBefore(sunsetAt);
        }
        return false;
    }

    public OpenApiEndpointListItemView toListItemView(
            OpenApiInvocationSummary invocationSummary,
            String recentAlertSummary
    ) {
        return new OpenApiEndpointListItemView(
                apiId,
                code,
                name,
                path,
                httpMethod,
                version,
                authType,
                status,
                dataServiceCode,
                dataServiceName,
                invocationSummary,
                recentAlertSummary,
                publishedAt,
                deprecatedAt,
                createdAt,
                updatedAt
        );
    }

    public OpenApiEndpointView toView(
            List<OpenApiVersionRelationView> versions,
            OpenApiInvocationSummary invocationSummary,
            String recentAlertSummary
    ) {
        return new OpenApiEndpointView(
                apiId,
                code,
                name,
                path,
                httpMethod,
                version,
                authType,
                status,
                dataServiceId,
                dataServiceCode,
                dataServiceName,
                compatibilityNotes,
                publishedAt,
                deprecatedAt,
                sunsetAt,
                createdAt,
                updatedAt,
                credentialGrants,
                rateLimitPolicies,
                versions,
                invocationSummary,
                recentAlertSummary
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String requirePath(String value) {
        String normalized = requireText(value, "path");
        if (!normalized.startsWith("/api/open/")) {
            throw new IllegalArgumentException("path must start with /api/open/");
        }
        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
