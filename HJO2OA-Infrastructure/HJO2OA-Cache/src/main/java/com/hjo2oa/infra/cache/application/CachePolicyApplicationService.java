package com.hjo2oa.infra.cache.application;

import com.hjo2oa.infra.cache.domain.CacheBackendType;
import com.hjo2oa.infra.cache.domain.CacheInvalidatedEvent;
import com.hjo2oa.infra.cache.domain.CacheInvalidationRecord;
import com.hjo2oa.infra.cache.domain.CacheInvalidationView;
import com.hjo2oa.infra.cache.domain.CachePolicy;
import com.hjo2oa.infra.cache.domain.CachePolicyRepository;
import com.hjo2oa.infra.cache.domain.CachePolicyView;
import com.hjo2oa.infra.cache.domain.CacheRuntimeKeyView;
import com.hjo2oa.infra.cache.domain.CacheRuntimeMetricsView;
import com.hjo2oa.infra.cache.domain.EvictionPolicy;
import com.hjo2oa.infra.cache.domain.InvalidationMode;
import com.hjo2oa.infra.cache.domain.InvalidationReasonType;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CachePolicyApplicationService {

    private static final String EVENT_INVALIDATE_ALL_KEY = "*";

    private final CachePolicyRepository cachePolicyRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final CacheRuntimeService cacheRuntimeService;
    private final Clock clock;

    @Autowired
    public CachePolicyApplicationService(
            CachePolicyRepository cachePolicyRepository,
            DomainEventPublisher domainEventPublisher,
            CacheRuntimeService cacheRuntimeService
    ) {
        this(cachePolicyRepository, domainEventPublisher, cacheRuntimeService, Clock.systemUTC());
    }

    public CachePolicyApplicationService(
            CachePolicyRepository cachePolicyRepository,
            DomainEventPublisher domainEventPublisher
    ) {
        this(cachePolicyRepository, domainEventPublisher, new NoopCacheRuntimeService(), Clock.systemUTC());
    }

    public CachePolicyApplicationService(
            CachePolicyRepository cachePolicyRepository,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this(cachePolicyRepository, domainEventPublisher, new NoopCacheRuntimeService(), clock);
    }

    public CachePolicyApplicationService(
            CachePolicyRepository cachePolicyRepository,
            DomainEventPublisher domainEventPublisher,
            CacheRuntimeService cacheRuntimeService,
            Clock clock
    ) {
        this.cachePolicyRepository = Objects.requireNonNull(
                cachePolicyRepository,
                "cachePolicyRepository must not be null"
        );
        this.domainEventPublisher = Objects.requireNonNull(
                domainEventPublisher,
                "domainEventPublisher must not be null"
        );
        this.cacheRuntimeService = Objects.requireNonNull(cacheRuntimeService, "cacheRuntimeService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public CachePolicyView createPolicy(CachePolicyCommands.CreatePolicyCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return createPolicy(
                command.namespace(),
                command.backendType(),
                command.ttlSeconds(),
                command.maxCapacity(),
                command.evictionPolicy(),
                command.invalidationMode()
        );
    }

    public CachePolicyView createPolicy(
            String namespace,
            CacheBackendType backendType,
            int ttlSeconds,
            Integer maxCapacity,
            EvictionPolicy evictionPolicy,
            InvalidationMode invalidationMode
    ) {
        String normalizedNamespace = normalizeText(namespace, "namespace");
        ensureNamespaceUnique(normalizedNamespace);

        CachePolicy cachePolicy = CachePolicy.create(
                normalizedNamespace,
                backendType,
                ttlSeconds,
                maxCapacity,
                evictionPolicy,
                invalidationMode,
                now()
        );
        cachePolicyRepository.save(cachePolicy);
        return cachePolicy.toView();
    }

    public CachePolicyView updatePolicy(CachePolicyCommands.UpdatePolicyCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return updatePolicy(
                command.policyId(),
                command.ttlSeconds(),
                command.maxCapacity(),
                command.evictionPolicy()
        );
    }

    public CachePolicyView updatePolicy(
            UUID policyId,
            Integer ttlSeconds,
            Integer maxCapacity,
            EvictionPolicy evictionPolicy
    ) {
        CachePolicy currentPolicy = loadPolicy(policyId);
        ensurePolicyActive(currentPolicy);

        int resolvedTtlSeconds = ttlSeconds != null ? ttlSeconds : currentPolicy.ttlSeconds();
        Integer resolvedMaxCapacity = maxCapacity != null ? maxCapacity : currentPolicy.maxCapacity();
        EvictionPolicy resolvedEvictionPolicy = evictionPolicy != null ? evictionPolicy : currentPolicy.evictionPolicy();
        if (currentPolicy.ttlSeconds() == resolvedTtlSeconds
                && Objects.equals(currentPolicy.maxCapacity(), resolvedMaxCapacity)
                && currentPolicy.evictionPolicy() == resolvedEvictionPolicy) {
            return currentPolicy.toView();
        }

        CachePolicy updatedPolicy = currentPolicy.update(
                resolvedTtlSeconds,
                resolvedMaxCapacity,
                resolvedEvictionPolicy,
                now()
        );
        cachePolicyRepository.save(updatedPolicy);
        cacheRuntimeService.evictNamespace(updatedPolicy.namespace());
        return updatedPolicy.toView();
    }

    public CachePolicyView deactivatePolicy(UUID policyId) {
        CachePolicy currentPolicy = loadPolicy(policyId);
        if (!currentPolicy.active()) {
            return currentPolicy.toView();
        }

        CachePolicy deactivatedPolicy = currentPolicy.deactivate(now());
        cachePolicyRepository.save(deactivatedPolicy);
        cacheRuntimeService.evictNamespace(deactivatedPolicy.namespace());
        return deactivatedPolicy.toView();
    }

    public CacheInvalidationView invalidateKey(CachePolicyCommands.InvalidateKeyCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return invalidateKey(command.namespace(), command.key(), command.reasonType(), command.reasonRef());
    }

    public CacheInvalidationView invalidateKey(
            String namespace,
            String key,
            InvalidationReasonType reasonType,
            String reasonRef
    ) {
        CachePolicy cachePolicy = loadPolicyByNamespace(namespace);
        ensurePolicyActive(cachePolicy);
        ensureInvalidationAllowed(cachePolicy, reasonType);
        return recordInvalidation(cachePolicy, key, reasonType, reasonRef, true);
    }

    public CacheInvalidationView invalidateByEvent(String namespace, String eventRef) {
        CachePolicy cachePolicy = loadPolicyByNamespace(namespace);
        ensurePolicyActive(cachePolicy);
        if (cachePolicy.invalidationMode() != InvalidationMode.EVENT_DRIVEN) {
            throw new BizException(
                    CacheErrorDescriptors.INVALIDATION_MODE_MISMATCH,
                    "Cache policy is not event driven"
            );
        }
        return recordInvalidation(
                cachePolicy,
                EVENT_INVALIDATE_ALL_KEY,
                InvalidationReasonType.EVENT,
                normalizeText(eventRef, "eventRef"),
                true
        );
    }

    public CacheInvalidationView clearNamespace(String namespace, String reasonRef) {
        CachePolicy cachePolicy = loadPolicyByNamespace(namespace);
        ensurePolicyActive(cachePolicy);
        return recordInvalidation(
                cachePolicy,
                EVENT_INVALIDATE_ALL_KEY,
                InvalidationReasonType.MANUAL,
                reasonRef,
                true
        );
    }

    public CacheInvalidationView refreshPolicy(UUID policyId, String reasonRef) {
        CachePolicy cachePolicy = loadPolicy(policyId);
        ensurePolicyActive(cachePolicy);
        return recordInvalidation(
                cachePolicy,
                EVENT_INVALIDATE_ALL_KEY,
                InvalidationReasonType.MANUAL,
                reasonRef,
                true
        );
    }

    public Optional<CachePolicyView> queryByNamespace(String namespace) {
        return cachePolicyRepository.findByNamespace(normalizeText(namespace, "namespace"))
                .map(CachePolicy::toView);
    }

    public List<CachePolicyView> list() {
        return cachePolicyRepository.findAll().stream()
                .map(CachePolicy::toView)
                .sorted(Comparator.comparing(CachePolicyView::namespace).thenComparing(CachePolicyView::id))
                .toList();
    }

    public List<CacheRuntimeKeyView> queryKeys(String namespace, UUID tenantId, String keyword) {
        return cacheRuntimeService.findKeys(namespace, tenantId, keyword);
    }

    public List<CacheRuntimeMetricsView> metrics() {
        return cacheRuntimeService.metrics();
    }

    public CacheRuntimeMetricsView metrics(String namespace) {
        return cacheRuntimeService.metrics(namespace);
    }

    public List<CacheInvalidationView> listInvalidations(String namespace, int limit) {
        UUID policyId = namespace == null || namespace.isBlank()
                ? null
                : loadPolicyByNamespace(namespace).id();
        return cachePolicyRepository.findInvalidationRecords(policyId, Math.min(Math.max(limit, 1), 200)).stream()
                .map(record -> {
                    CachePolicy policy = cachePolicyRepository.findById(record.cachePolicyId()).orElse(null);
                    return record.toView(policy == null ? "unknown" : policy.namespace());
                })
                .toList();
    }

    private CacheInvalidationView recordInvalidation(
            CachePolicy cachePolicy,
            String key,
            InvalidationReasonType reasonType,
            String reasonRef,
            boolean evictRuntime
    ) {
        Instant occurredAt = now();
        String normalizedKey = normalizeText(key, "key");
        CacheInvalidationRecord invalidationRecord = CacheInvalidationRecord.create(
                cachePolicy.id(),
                normalizedKey,
                Objects.requireNonNull(reasonType, "reasonType must not be null"),
                reasonRef,
                occurredAt
        );
        CacheInvalidationRecord savedRecord = cachePolicyRepository.saveInvalidationRecord(invalidationRecord);
        CacheInvalidationView invalidationView = savedRecord.toView(cachePolicy.namespace());
        if (evictRuntime) {
            evictRuntime(cachePolicy.namespace(), normalizedKey);
        }
        domainEventPublisher.publish(CacheInvalidatedEvent.of(
                invalidationView.namespace(),
                invalidationView.invalidateKey(),
                invalidationView.reasonType(),
                invalidationView.invalidatedAt()
        ));
        return invalidationView;
    }

    private void evictRuntime(String namespace, String key) {
        if (EVENT_INVALIDATE_ALL_KEY.equals(key)) {
            cacheRuntimeService.evictNamespace(namespace);
            return;
        }
        cacheRuntimeService.evictKey(namespace, null, key);
    }

    private CachePolicy loadPolicy(UUID policyId) {
        Objects.requireNonNull(policyId, "policyId must not be null");
        return cachePolicyRepository.findById(policyId)
                .orElseThrow(() -> new BizException(
                        CacheErrorDescriptors.POLICY_NOT_FOUND,
                        "Cache policy not found"
                ));
    }

    private CachePolicy loadPolicyByNamespace(String namespace) {
        return cachePolicyRepository.findByNamespace(normalizeText(namespace, "namespace"))
                .orElseThrow(() -> new BizException(
                        CacheErrorDescriptors.POLICY_NOT_FOUND,
                        "Cache policy not found"
                ));
    }

    private void ensureNamespaceUnique(String namespace) {
        if (cachePolicyRepository.findByNamespace(namespace).isPresent()) {
            throw new BizException(
                    CacheErrorDescriptors.NAMESPACE_CONFLICT,
                    "Cache namespace already exists"
            );
        }
    }

    private void ensurePolicyActive(CachePolicy cachePolicy) {
        if (!cachePolicy.active()) {
            throw new BizException(CacheErrorDescriptors.POLICY_INACTIVE, "Cache policy is inactive");
        }
    }

    private void ensureInvalidationAllowed(CachePolicy cachePolicy, InvalidationReasonType reasonType) {
        Objects.requireNonNull(reasonType, "reasonType must not be null");
        switch (reasonType) {
            case MANUAL -> {
                return;
            }
            case EVENT, DEPENDENCY -> {
                if (cachePolicy.invalidationMode() != InvalidationMode.EVENT_DRIVEN) {
                    throw new BizException(
                            CacheErrorDescriptors.INVALIDATION_MODE_MISMATCH,
                            "Cache policy is not event driven"
                    );
                }
            }
            case TTL -> {
                if (cachePolicy.invalidationMode() != InvalidationMode.TIME_BASED) {
                    throw new BizException(
                            CacheErrorDescriptors.INVALIDATION_MODE_MISMATCH,
                            "Cache policy is not time based"
                    );
                }
            }
            default -> throw new IllegalArgumentException("Unsupported invalidation reason type: " + reasonType);
        }
    }

    private Instant now() {
        return clock.instant();
    }

    private String normalizeText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
