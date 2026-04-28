package com.hjo2oa.data.openapi.application;

import com.hjo2oa.data.openapi.domain.ApiCredentialGrant;
import com.hjo2oa.data.openapi.domain.ApiCredentialStatus;
import com.hjo2oa.data.openapi.domain.ApiInvocationAuditLog;
import com.hjo2oa.data.openapi.domain.ApiInvocationAuditLogRepository;
import com.hjo2oa.data.openapi.domain.ApiInvocationOutcome;
import com.hjo2oa.data.openapi.domain.ApiPolicyType;
import com.hjo2oa.data.openapi.domain.ApiQuotaUsageCounter;
import com.hjo2oa.data.openapi.domain.ApiQuotaUsageCounterRepository;
import com.hjo2oa.data.openapi.domain.ApiRateLimitPolicy;
import com.hjo2oa.data.openapi.domain.ApiRateLimitPolicyView;
import com.hjo2oa.data.openapi.domain.ApiWindowUnit;
import com.hjo2oa.data.openapi.domain.DataApiDeprecatedEvent;
import com.hjo2oa.data.openapi.domain.DataApiPublishedEvent;
import com.hjo2oa.data.openapi.domain.OpenApiAuthType;
import com.hjo2oa.data.openapi.domain.OpenApiEndpoint;
import com.hjo2oa.data.openapi.domain.OpenApiEndpointListItemView;
import com.hjo2oa.data.openapi.domain.OpenApiEndpointRepository;
import com.hjo2oa.data.openapi.domain.OpenApiEndpointView;
import com.hjo2oa.data.openapi.domain.OpenApiErrorDescriptors;
import com.hjo2oa.data.openapi.domain.OpenApiHttpMethod;
import com.hjo2oa.data.openapi.domain.OpenApiInvocationSummary;
import com.hjo2oa.data.openapi.domain.OpenApiOperatorContext;
import com.hjo2oa.data.openapi.domain.OpenApiOperatorContextProvider;
import com.hjo2oa.data.openapi.domain.OpenApiOperatorPermission;
import com.hjo2oa.data.openapi.domain.OpenApiStatus;
import com.hjo2oa.data.openapi.domain.OpenApiVersionRelationView;
import com.hjo2oa.data.service.domain.DataServiceDefinition;
import com.hjo2oa.data.service.domain.DataServiceDefinitionRepository;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import com.hjo2oa.shared.web.PageData;
import com.hjo2oa.shared.web.Pagination;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenApiManagementApplicationService {

    private final OpenApiEndpointRepository endpointRepository;
    private final ApiInvocationAuditLogRepository auditLogRepository;
    private final ApiQuotaUsageCounterRepository quotaUsageCounterRepository;
    private final DataServiceDefinitionRepository dataServiceDefinitionRepository;
    private final OpenApiOperatorContextProvider operatorContextProvider;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;
    @Autowired
    public OpenApiManagementApplicationService(
            OpenApiEndpointRepository endpointRepository,
            ApiInvocationAuditLogRepository auditLogRepository,
            ApiQuotaUsageCounterRepository quotaUsageCounterRepository,
            DataServiceDefinitionRepository dataServiceDefinitionRepository,
            OpenApiOperatorContextProvider operatorContextProvider,
            DomainEventPublisher domainEventPublisher
    ) {
        this(
                endpointRepository,
                auditLogRepository,
                quotaUsageCounterRepository,
                dataServiceDefinitionRepository,
                operatorContextProvider,
                domainEventPublisher,
                Clock.systemUTC()
        );
    }
    public OpenApiManagementApplicationService(
            OpenApiEndpointRepository endpointRepository,
            ApiInvocationAuditLogRepository auditLogRepository,
            ApiQuotaUsageCounterRepository quotaUsageCounterRepository,
            DataServiceDefinitionRepository dataServiceDefinitionRepository,
            OpenApiOperatorContextProvider operatorContextProvider,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.endpointRepository = Objects.requireNonNull(endpointRepository, "endpointRepository must not be null");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository must not be null");
        this.quotaUsageCounterRepository = Objects.requireNonNull(
                quotaUsageCounterRepository,
                "quotaUsageCounterRepository must not be null"
        );
        this.dataServiceDefinitionRepository = Objects.requireNonNull(
                dataServiceDefinitionRepository,
                "dataServiceDefinitionRepository must not be null"
        );
        this.operatorContextProvider = Objects.requireNonNull(
                operatorContextProvider,
                "operatorContextProvider must not be null"
        );
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public PageData<OpenApiEndpointListItemView> pageEndpoints(
            String path,
            OpenApiHttpMethod httpMethod,
            String version,
            OpenApiStatus status,
            String dataServiceCode,
            int page,
            int size
    ) {
        OpenApiOperatorContext context = currentContext();
        requireAnyPermission(
                context,
                OpenApiOperatorPermission.OPEN_API_ADMIN,
                OpenApiOperatorPermission.SECURITY_ADMIN,
                OpenApiOperatorPermission.AUDITOR,
                OpenApiOperatorPermission.OPS_ADMIN
        );

        List<OpenApiEndpointListItemView> items = endpointRepository.findAllByTenant(context.tenantId()).stream()
                .filter(endpoint -> path == null || endpoint.path().contains(path))
                .filter(endpoint -> httpMethod == null || endpoint.httpMethod() == httpMethod)
                .filter(endpoint -> version == null || endpoint.version().equals(version))
                .filter(endpoint -> status == null || endpoint.status() == status)
                .filter(endpoint -> dataServiceCode == null || endpoint.dataServiceCode().equals(dataServiceCode))
                .sorted(Comparator.comparing(OpenApiEndpoint::code).thenComparing(OpenApiEndpoint::version))
                .map(endpoint -> endpoint.toListItemView(
                        summarizeInvocation(context.tenantId(), endpoint.apiId()),
                        recentAlertSummary(context.tenantId(), endpoint.apiId())
                ))
                .toList();
        return paginate(items, page, size);
    }

    public OpenApiEndpointView endpointDetail(String code, String version) {
        OpenApiOperatorContext context = currentContext();
        requireAnyPermission(
                context,
                OpenApiOperatorPermission.OPEN_API_ADMIN,
                OpenApiOperatorPermission.SECURITY_ADMIN,
                OpenApiOperatorPermission.AUDITOR,
                OpenApiOperatorPermission.OPS_ADMIN
        );
        OpenApiEndpoint endpoint = loadEndpoint(context.tenantId(), code, version);
        List<OpenApiVersionRelationView> versions = versionRelations(context.tenantId(), code);
        return endpoint.toView(
                versions,
                summarizeInvocation(context.tenantId(), endpoint.apiId()),
                recentAlertSummary(context.tenantId(), endpoint.apiId())
        );
    }

    @Transactional
    public OpenApiEndpointView upsertEndpoint(
            String code,
            String version,
            String name,
            String dataServiceCode,
            String path,
            OpenApiHttpMethod httpMethod,
            OpenApiAuthType authType,
            String compatibilityNotes
    ) {
        OpenApiOperatorContext context = currentContext();
        requireAnyPermission(context, OpenApiOperatorPermission.OPEN_API_ADMIN);
        Instant now = now();

        DataServiceDefinition dataServiceDefinition = resolveReusableDataService(context.tenantId(), dataServiceCode);
        ensurePathUniqueness(context.tenantId(), path, httpMethod, version, nullIfSameApi(context.tenantId(), code, version));

        OpenApiEndpoint existing = endpointRepository.findByCodeAndVersion(context.tenantId(), code, version).orElse(null);
        OpenApiEndpoint endpoint = existing == null
                ? OpenApiEndpoint.create(
                        context.tenantId(),
                        code,
                        name,
                        dataServiceDefinition.serviceId().toString(),
                        dataServiceDefinition.code(),
                        dataServiceDefinition.name(),
                        path,
                        httpMethod,
                        version,
                        authType,
                        compatibilityNotes,
                        now
                )
                : existing.updateDraft(
                        name,
                        dataServiceDefinition.serviceId().toString(),
                        dataServiceDefinition.code(),
                        dataServiceDefinition.name(),
                        path,
                        httpMethod,
                        authType,
                        compatibilityNotes,
                        now
                );
        endpointRepository.save(endpoint);
        return endpointDetail(code, version);
    }

    @Transactional
    public OpenApiEndpointView publishEndpoint(String code, String version) {
        OpenApiOperatorContext context = currentContext();
        requireAnyPermission(context, OpenApiOperatorPermission.OPEN_API_ADMIN);
        Instant now = now();
        OpenApiEndpoint endpoint = loadEndpoint(context.tenantId(), code, version);
        if (endpoint.credentialGrants().stream().noneMatch(grant -> grant.isActiveAt(now))) {
            throw new BizException(
                    OpenApiErrorDescriptors.API_STATUS_INVALID,
                    "Active credential is required before publishing endpoint"
            );
        }
        boolean emitEvent = endpoint.status() != OpenApiStatus.ACTIVE;
        OpenApiEndpoint published = endpoint.publish(now);
        endpointRepository.save(published);
        if (emitEvent) {
            domainEventPublisher.publish(DataApiPublishedEvent.from(published, now));
        }
        return endpointDetail(code, version);
    }

    @Transactional
    public OpenApiEndpointView deprecateEndpoint(String code, String version, Instant sunsetAt) {
        OpenApiOperatorContext context = currentContext();
        requireAnyPermission(context, OpenApiOperatorPermission.OPEN_API_ADMIN);
        Instant now = now();
        OpenApiEndpoint endpoint = loadEndpoint(context.tenantId(), code, version);
        boolean emitEvent = endpoint.status() != OpenApiStatus.DEPRECATED || !Objects.equals(endpoint.sunsetAt(), sunsetAt);
        OpenApiEndpoint deprecated = endpoint.deprecate(now, sunsetAt);
        endpointRepository.save(deprecated);
        if (emitEvent) {
            domainEventPublisher.publish(DataApiDeprecatedEvent.from(deprecated, now));
        }
        return endpointDetail(code, version);
    }

    @Transactional
    public OpenApiEndpointView offlineEndpoint(String code, String version) {
        OpenApiOperatorContext context = currentContext();
        requireAnyPermission(context, OpenApiOperatorPermission.OPEN_API_ADMIN);
        OpenApiEndpoint endpoint = loadEndpoint(context.tenantId(), code, version);
        endpointRepository.save(endpoint.offline(now()));
        return endpointDetail(code, version);
    }

    @Transactional
    public void deleteEndpoint(String code, String version) {
        OpenApiOperatorContext context = currentContext();
        requireAnyPermission(context, OpenApiOperatorPermission.OPEN_API_ADMIN);
        OpenApiEndpoint endpoint = loadEndpoint(context.tenantId(), code, version);
        if (!endpoint.canDelete()) {
            throw new BizException(OpenApiErrorDescriptors.API_STATUS_INVALID, "Only draft or offlined endpoints can be deleted");
        }
        endpointRepository.delete(endpoint.apiId());
    }

    public PageData<ApiCredentialGrant> pageCredentials(
            String clientCode,
            ApiCredentialStatus status,
            Instant expiresBefore,
            int page,
            int size
    ) {
        OpenApiOperatorContext context = currentContext();
        requireAnyPermission(
                context,
                OpenApiOperatorPermission.OPEN_API_ADMIN,
                OpenApiOperatorPermission.SECURITY_ADMIN,
                OpenApiOperatorPermission.AUDITOR
        );
        Instant now = now();
        List<ApiCredentialGrant> items = endpointRepository.findAllByTenant(context.tenantId()).stream()
                .flatMap(endpoint -> endpoint.credentialGrants().stream())
                .filter(grant -> clientCode == null || grant.clientCode().equals(clientCode))
                .filter(grant -> status == null || grant.effectiveStatus(now) == status)
                .filter(grant -> expiresBefore == null || (grant.expiresAt() != null && !grant.expiresAt().isAfter(expiresBefore)))
                .sorted(Comparator.comparing(ApiCredentialGrant::clientCode).thenComparing(ApiCredentialGrant::updatedAt).reversed())
                .toList();
        return paginate(items, page, size);
    }

    @Transactional
    public OpenApiEndpointView upsertCredential(
            String code,
            String version,
            String clientCode,
            String secretRef,
            List<String> scopes,
            Instant expiresAt
    ) {
        OpenApiOperatorContext context = currentContext();
        requireAnyPermission(
                context,
                OpenApiOperatorPermission.OPEN_API_ADMIN,
                OpenApiOperatorPermission.SECURITY_ADMIN
        );
        Instant now = now();
        OpenApiEndpoint endpoint = loadEndpoint(context.tenantId(), code, version);
        ApiCredentialGrant credentialGrant = endpoint.credentialFor(clientCode)
                .map(existing -> existing.update(secretRef, scopes, expiresAt, now))
                .orElse(ApiCredentialGrant.create(endpoint.apiId(), context.tenantId(), clientCode, secretRef, scopes, expiresAt, now));
        endpointRepository.save(endpoint.withCredential(credentialGrant));
        return endpointDetail(code, version);
    }

    @Transactional
    public OpenApiEndpointView revokeCredential(String code, String version, String clientCode) {
        OpenApiOperatorContext context = currentContext();
        requireAnyPermission(
                context,
                OpenApiOperatorPermission.OPEN_API_ADMIN,
                OpenApiOperatorPermission.SECURITY_ADMIN
        );
        OpenApiEndpoint endpoint = loadEndpoint(context.tenantId(), code, version);
        if (endpoint.credentialFor(clientCode).isEmpty()) {
            throw new BizException(OpenApiErrorDescriptors.CREDENTIAL_NOT_FOUND, "Credential grant not found");
        }
        endpointRepository.save(endpoint.revokeCredential(clientCode, now()));
        return endpointDetail(code, version);
    }

    public PageData<ApiRateLimitPolicyView> pagePolicies(
            String endpointCode,
            String clientCode,
            String policyCode,
            int page,
            int size
    ) {
        OpenApiOperatorContext context = currentContext();
        requireAnyPermission(
                context,
                OpenApiOperatorPermission.OPEN_API_ADMIN,
                OpenApiOperatorPermission.AUDITOR,
                OpenApiOperatorPermission.OPS_ADMIN
        );
        List<ApiRateLimitPolicyView> items = endpointRepository.findAllByTenant(context.tenantId()).stream()
                .filter(endpoint -> endpointCode == null || endpoint.code().equals(endpointCode))
                .flatMap(endpoint -> endpoint.rateLimitPolicies().stream().map(policy -> toPolicyView(context.tenantId(), endpoint, policy)))
                .filter(view -> clientCode == null || Objects.equals(view.clientCode(), clientCode))
                .filter(view -> policyCode == null || view.policyCode().equals(policyCode))
                .sorted(Comparator.comparing(ApiRateLimitPolicyView::policyCode))
                .toList();
        return paginate(items, page, size);
    }

    @Transactional
    public OpenApiEndpointView upsertPolicy(
            String code,
            String version,
            String policyCode,
            String clientCode,
            ApiPolicyType policyType,
            long windowValue,
            ApiWindowUnit windowUnit,
            long threshold,
            String description
    ) {
        OpenApiOperatorContext context = currentContext();
        requireAnyPermission(context, OpenApiOperatorPermission.OPEN_API_ADMIN);
        Instant now = now();
        OpenApiEndpoint endpoint = loadEndpoint(context.tenantId(), code, version);
        ApiRateLimitPolicy policy = endpoint.rateLimitPolicies().stream()
                .filter(existing -> existing.policyCode().equals(policyCode))
                .filter(existing -> Objects.equals(existing.clientCode(), normalizeNullable(clientCode)))
                .findFirst()
                .map(existing -> existing.update(clientCode, policyType, windowValue, windowUnit, threshold, description, now))
                .orElse(ApiRateLimitPolicy.create(
                        endpoint.apiId(),
                        context.tenantId(),
                        policyCode,
                        clientCode,
                        policyType,
                        windowValue,
                        windowUnit,
                        threshold,
                        description,
                        now
                ));
        endpointRepository.save(endpoint.withPolicy(policy));
        return endpointDetail(code, version);
    }

    @Transactional
    public OpenApiEndpointView disablePolicy(String code, String version, String policyCode, String clientCode) {
        OpenApiOperatorContext context = currentContext();
        requireAnyPermission(context, OpenApiOperatorPermission.OPEN_API_ADMIN);
        OpenApiEndpoint endpoint = loadEndpoint(context.tenantId(), code, version);
        endpointRepository.save(endpoint.disablePolicy(policyCode, clientCode, now()));
        return endpointDetail(code, version);
    }

    public PageData<ApiInvocationAuditLog> pageAuditLogs(
            String endpointCode,
            String clientCode,
            Integer responseStatus,
            Instant occurredFrom,
            Instant occurredTo,
            int page,
            int size
    ) {
        OpenApiOperatorContext context = currentContext();
        requireAnyPermission(context, OpenApiOperatorPermission.AUDITOR, OpenApiOperatorPermission.OPS_ADMIN);
        List<ApiInvocationAuditLog> items = auditLogRepository.findAllByTenant(context.tenantId()).stream()
                .filter(log -> endpointCode == null || endpointCode.equals(log.endpointCode()))
                .filter(log -> clientCode == null || clientCode.equals(log.clientCode()))
                .filter(log -> responseStatus == null || responseStatus == log.responseStatus())
                .filter(log -> occurredFrom == null || !log.occurredAt().isBefore(occurredFrom))
                .filter(log -> occurredTo == null || !log.occurredAt().isAfter(occurredTo))
                .sorted(Comparator.comparing(ApiInvocationAuditLog::occurredAt).reversed())
                .toList();
        return paginate(items, page, size);
    }

    public ApiInvocationAuditLog auditLogDetail(String logId) {
        OpenApiOperatorContext context = currentContext();
        requireAnyPermission(context, OpenApiOperatorPermission.AUDITOR, OpenApiOperatorPermission.OPS_ADMIN);
        return auditLogRepository.findById(logId)
                .filter(log -> log.tenantId().equals(context.tenantId()))
                .orElseThrow(() -> new BizException(OpenApiErrorDescriptors.AUDIT_LOG_NOT_FOUND, "Audit log not found"));
    }

    @Transactional
    public ApiInvocationAuditLog reviewAuditLog(
            String logId,
            boolean abnormalFlag,
            String reviewConclusion,
            String note
    ) {
        OpenApiOperatorContext context = currentContext();
        requireAnyPermission(context, OpenApiOperatorPermission.AUDITOR, OpenApiOperatorPermission.OPS_ADMIN);
        ApiInvocationAuditLog auditLog = auditLogDetail(logId);
        ApiInvocationAuditLog reviewed = auditLog.review(
                abnormalFlag,
                reviewConclusion,
                note,
                context.operatorId(),
                now()
        );
        return auditLogRepository.save(reviewed);
    }

    private DataServiceDefinition resolveReusableDataService(String tenantId, String code) {
        return dataServiceDefinitionRepository.findByCode(UUID.fromString(tenantId), code)
                .filter(DataServiceDefinition::reusableByOpenApi)
                .orElseThrow(() -> new BizException(
                        OpenApiErrorDescriptors.DATA_SERVICE_NOT_FOUND,
                        "Active DataServiceDefinition not found: " + code
                ));
    }

    private void ensurePathUniqueness(
            String tenantId,
            String path,
            OpenApiHttpMethod httpMethod,
            String version,
            String currentApiId
    ) {
        endpointRepository.findByPathMethodAndVersion(tenantId, path, httpMethod, version)
                .filter(endpoint -> !endpoint.apiId().equals(currentApiId))
                .ifPresent(endpoint -> {
                    throw new BizException(OpenApiErrorDescriptors.API_PATH_CONFLICT, "API path/method/version conflict");
                });
    }

    private String nullIfSameApi(String tenantId, String code, String version) {
        return endpointRepository.findByCodeAndVersion(tenantId, code, version)
                .map(OpenApiEndpoint::apiId)
                .orElse(null);
    }

    private OpenApiEndpoint loadEndpoint(String tenantId, String code, String version) {
        return endpointRepository.findByCodeAndVersion(tenantId, code, version)
                .orElseThrow(() -> new BizException(OpenApiErrorDescriptors.API_VERSION_NOT_FOUND, "API version not found"));
    }

    private List<OpenApiVersionRelationView> versionRelations(String tenantId, String code) {
        return endpointRepository.findAllByTenant(tenantId).stream()
                .filter(endpoint -> endpoint.code().equals(code))
                .sorted(Comparator.comparing(OpenApiEndpoint::version))
                .map(endpoint -> new OpenApiVersionRelationView(
                        endpoint.version(),
                        endpoint.status(),
                        endpoint.publishedAt(),
                        endpoint.deprecatedAt(),
                        endpoint.sunsetAt()
                ))
                .toList();
    }

    private OpenApiInvocationSummary summarizeInvocation(String tenantId, String apiId) {
        List<ApiInvocationAuditLog> logs = auditLogRepository.findAllByTenant(tenantId).stream()
                .filter(log -> apiId.equals(log.apiId()))
                .toList();
        long totalCalls = logs.size();
        long successCalls = logs.stream().filter(log -> log.outcome() == ApiInvocationOutcome.SUCCESS).count();
        Instant lastOccurredAt = logs.stream()
                .map(ApiInvocationAuditLog::occurredAt)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new OpenApiInvocationSummary(totalCalls, successCalls, lastOccurredAt);
    }

    private String recentAlertSummary(String tenantId, String apiId) {
        return auditLogRepository.findAllByTenant(tenantId).stream()
                .filter(log -> apiId.equals(log.apiId()))
                .filter(log -> log.outcome() != ApiInvocationOutcome.SUCCESS || log.abnormalFlag())
                .max(Comparator.comparing(ApiInvocationAuditLog::occurredAt))
                .map(log -> log.outcome().name() + "@" + log.occurredAt())
                .orElse(null);
    }

    private ApiRateLimitPolicyView toPolicyView(String tenantId, OpenApiEndpoint endpoint, ApiRateLimitPolicy policy) {
        Instant windowStartedAt = policy.windowStartedAt(now());
        long usedCount = quotaUsageCounterRepository.findByWindow(
                        tenantId,
                        policy.policyId(),
                        policy.clientCode() == null ? "*" : policy.clientCode(),
                        windowStartedAt
                )
                .map(ApiQuotaUsageCounter::usedCount)
                .orElse(0L);
        return new ApiRateLimitPolicyView(
                policy.policyId(),
                policy.policyCode(),
                policy.clientCode(),
                policy.policyType(),
                policy.windowValue(),
                policy.windowUnit(),
                policy.threshold(),
                policy.status(),
                policy.description(),
                usedCount,
                windowStartedAt,
                policy.createdAt(),
                policy.updatedAt()
        );
    }

    private OpenApiOperatorContext currentContext() {
        return operatorContextProvider.currentContext();
    }

    private Instant now() {
        return clock.instant();
    }

    private void requireAnyPermission(
            OpenApiOperatorContext context,
            OpenApiOperatorPermission firstPermission,
            OpenApiOperatorPermission... remainingPermissions
    ) {
        if (context.has(firstPermission)) {
            return;
        }
        for (OpenApiOperatorPermission permission : remainingPermissions) {
            if (context.has(permission)) {
                return;
            }
        }
        throw new BizException(SharedErrorDescriptors.FORBIDDEN, "Operator permission is not sufficient");
    }

    private <T> PageData<T> paginate(List<T> items, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min((safePage - 1) * safeSize, items.size());
        int toIndex = Math.min(fromIndex + safeSize, items.size());
        Pagination pagination = Pagination.of(safePage, safeSize, items.size());
        return new PageData<>(items.subList(fromIndex, toIndex), pagination);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
