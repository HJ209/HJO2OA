package com.hjo2oa.infra.cache.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hjo2oa.infra.cache.domain.CacheBackendType;
import com.hjo2oa.infra.cache.domain.CacheInvalidatedEvent;
import com.hjo2oa.infra.cache.domain.CacheInvalidationView;
import com.hjo2oa.infra.cache.domain.CachePolicyView;
import com.hjo2oa.infra.cache.domain.EvictionPolicy;
import com.hjo2oa.infra.cache.domain.InvalidationMode;
import com.hjo2oa.infra.cache.domain.InvalidationReasonType;
import com.hjo2oa.infra.cache.infrastructure.InMemoryCachePolicyRepository;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEvent;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CachePolicyApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T06:00:00Z");

    @Test
    void shouldCreatePolicyAndQueryByNamespace() {
        CachePolicyApplicationService applicationService = applicationService(
                new InMemoryCachePolicyRepository(),
                new RecordingDomainEventPublisher()
        );

        CachePolicyView created = applicationService.createPolicy(
                "portal.home",
                CacheBackendType.REDIS,
                300,
                1000,
                EvictionPolicy.LRU,
                InvalidationMode.MANUAL
        );

        assertNotNull(created.id());
        assertEquals("portal.home", created.namespace());
        assertEquals(CacheBackendType.REDIS, created.backendType());
        assertEquals(300, created.ttlSeconds());
        assertEquals(1000, created.maxCapacity());
        assertTrue(created.metricsEnabled());
        assertTrue(created.active());
        assertEquals(FIXED_TIME, created.createdAt());
        assertEquals(created, applicationService.queryByNamespace("portal.home").orElseThrow());
    }

    @Test
    void shouldRejectDuplicateNamespace() {
        CachePolicyApplicationService applicationService = applicationService(
                new InMemoryCachePolicyRepository(),
                new RecordingDomainEventPublisher()
        );
        applicationService.createPolicy(
                "portal.home",
                CacheBackendType.REDIS,
                300,
                1000,
                EvictionPolicy.LRU,
                InvalidationMode.MANUAL
        );

        BizException exception = assertThrows(BizException.class, () -> applicationService.createPolicy(
                "portal.home",
                CacheBackendType.MEMORY,
                60,
                null,
                EvictionPolicy.NONE,
                InvalidationMode.MANUAL
        ));

        assertEquals(CacheErrorDescriptors.NAMESPACE_CONFLICT.code(), exception.errorCode());
    }

    @Test
    void shouldUpdatePolicyUsingPartialValues() {
        CachePolicyApplicationService applicationService = applicationService(
                new InMemoryCachePolicyRepository(),
                new RecordingDomainEventPublisher()
        );
        CachePolicyView created = applicationService.createPolicy(
                "portal.home",
                CacheBackendType.REDIS,
                300,
                1000,
                EvictionPolicy.LRU,
                InvalidationMode.MANUAL
        );

        CachePolicyView updated = applicationService.updatePolicy(created.id(), 600, null, EvictionPolicy.LFU);

        assertEquals(created.id(), updated.id());
        assertEquals(600, updated.ttlSeconds());
        assertEquals(1000, updated.maxCapacity());
        assertEquals(EvictionPolicy.LFU, updated.evictionPolicy());
        assertEquals(created.createdAt(), updated.createdAt());
        assertEquals(FIXED_TIME, updated.updatedAt());
    }

    @Test
    void shouldInvalidateKeyAndPublishEvent() {
        InMemoryCachePolicyRepository repository = new InMemoryCachePolicyRepository();
        RecordingDomainEventPublisher publisher = new RecordingDomainEventPublisher();
        CachePolicyApplicationService applicationService = applicationService(repository, publisher);
        applicationService.createPolicy(
                "portal.home",
                CacheBackendType.REDIS,
                300,
                1000,
                EvictionPolicy.LRU,
                InvalidationMode.MANUAL
        );

        CacheInvalidationView invalidationView = applicationService.invalidateKey(
                "portal.home",
                "home:widget:1",
                InvalidationReasonType.MANUAL,
                "ticket-1"
        );

        assertEquals("portal.home", invalidationView.namespace());
        assertEquals("home:widget:1", invalidationView.invalidateKey());
        assertEquals(InvalidationReasonType.MANUAL, invalidationView.reasonType());
        assertEquals("ticket-1", invalidationView.reasonRef());
        assertEquals(1, repository.invalidationRecords().size());
        assertEquals(1, publisher.publishedEvents.size());
        assertTrue(publisher.publishedEvents.get(0) instanceof CacheInvalidatedEvent);
        CacheInvalidatedEvent event = (CacheInvalidatedEvent) publisher.publishedEvents.get(0);
        assertEquals(CacheInvalidatedEvent.EVENT_TYPE, event.eventType());
        assertEquals("portal.home", event.namespace());
        assertEquals("home:widget:1", event.invalidateKey());
        assertEquals(InvalidationReasonType.MANUAL, event.reasonType());
    }

    @Test
    void shouldInvalidateByEventWhenPolicyUsesEventDrivenMode() {
        InMemoryCachePolicyRepository repository = new InMemoryCachePolicyRepository();
        RecordingDomainEventPublisher publisher = new RecordingDomainEventPublisher();
        CachePolicyApplicationService applicationService = applicationService(repository, publisher);
        applicationService.createPolicy(
                "portal.aggregate",
                CacheBackendType.HYBRID,
                120,
                500,
                EvictionPolicy.FIFO,
                InvalidationMode.EVENT_DRIVEN
        );

        CacheInvalidationView invalidationView = applicationService.invalidateByEvent(
                "portal.aggregate",
                "portal.template.published#v1"
        );

        assertEquals("portal.aggregate", invalidationView.namespace());
        assertEquals("*", invalidationView.invalidateKey());
        assertEquals(InvalidationReasonType.EVENT, invalidationView.reasonType());
        assertEquals("portal.template.published#v1", invalidationView.reasonRef());
        assertEquals(1, repository.invalidationRecords().size());
        assertEquals(1, publisher.publishedEvents.size());
    }

    @Test
    void shouldRejectInvalidationForInactivePolicy() {
        CachePolicyApplicationService applicationService = applicationService(
                new InMemoryCachePolicyRepository(),
                new RecordingDomainEventPublisher()
        );
        CachePolicyView created = applicationService.createPolicy(
                "portal.home",
                CacheBackendType.REDIS,
                300,
                1000,
                EvictionPolicy.LRU,
                InvalidationMode.MANUAL
        );
        applicationService.deactivatePolicy(created.id());

        BizException exception = assertThrows(BizException.class, () -> applicationService.invalidateKey(
                "portal.home",
                "home:widget:1",
                InvalidationReasonType.MANUAL,
                null
        ));

        assertEquals(CacheErrorDescriptors.POLICY_INACTIVE.code(), exception.errorCode());
    }

    private CachePolicyApplicationService applicationService(
            InMemoryCachePolicyRepository repository,
            DomainEventPublisher domainEventPublisher
    ) {
        return new CachePolicyApplicationService(
                repository,
                domainEventPublisher,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    private static final class RecordingDomainEventPublisher implements DomainEventPublisher {

        private final List<DomainEvent> publishedEvents = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            publishedEvents.add(event);
        }
    }
}
