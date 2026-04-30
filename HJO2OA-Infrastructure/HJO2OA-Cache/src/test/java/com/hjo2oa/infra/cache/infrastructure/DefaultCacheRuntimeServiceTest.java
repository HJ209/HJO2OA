package com.hjo2oa.infra.cache.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.infra.cache.domain.CacheBackendType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DefaultCacheRuntimeServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T06:00:00Z");
    private static final UUID TENANT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void shouldRecordCacheHitAndKeepLoadedValueUntilInvalidated() {
        DefaultCacheRuntimeService cacheRuntimeService = cacheRuntimeService();
        AtomicInteger loads = new AtomicInteger();

        String first = cacheRuntimeService.getOrLoad(
                "infra.dictionary.runtime",
                TENANT_A,
                "priority:items:enabled=true:lang=en-US",
                Duration.ofMinutes(5),
                String.class,
                () -> "value-" + loads.incrementAndGet()
        );
        String second = cacheRuntimeService.getOrLoad(
                "infra.dictionary.runtime",
                TENANT_A,
                "priority:items:enabled=true:lang=en-US",
                Duration.ofMinutes(5),
                String.class,
                () -> "value-" + loads.incrementAndGet()
        );

        assertThat(first).isEqualTo("value-1");
        assertThat(second).isEqualTo("value-1");
        assertThat(loads).hasValue(1);
        assertThat(cacheRuntimeService.metrics("infra.dictionary.runtime").localHitCount()).isEqualTo(1);
        assertThat(cacheRuntimeService.metrics("infra.dictionary.runtime").missCount()).isEqualTo(1);
        assertThat(cacheRuntimeService.metrics("infra.dictionary.runtime").putCount()).isEqualTo(1);
        assertThat(cacheRuntimeService.findKeys("infra.dictionary.runtime", TENANT_A, "priority"))
                .singleElement()
                .satisfies(key -> {
                    assertThat(key.backendType()).isEqualTo(CacheBackendType.MEMORY);
                    assertThat(key.tenantId()).isEqualTo(TENANT_A.toString());
                });

        assertThat(cacheRuntimeService.evictByPrefix("infra.dictionary.runtime", TENANT_A, "priority:"))
                .isEqualTo(1);
        assertThat(cacheRuntimeService.get(
                "infra.dictionary.runtime",
                TENANT_A,
                "priority:items:enabled=true:lang=en-US",
                String.class
        )).isEmpty();
        assertThat(cacheRuntimeService.metrics("infra.dictionary.runtime").invalidationCount()).isEqualTo(1);
    }

    @Test
    void shouldKeepTenantScopedKeysIsolated() {
        DefaultCacheRuntimeService cacheRuntimeService = cacheRuntimeService();
        cacheRuntimeService.put(
                "infra.dictionary.runtime",
                TENANT_A,
                "priority:items:enabled=true:lang=en-US",
                "tenant-a",
                Duration.ofMinutes(5)
        );
        cacheRuntimeService.put(
                "infra.dictionary.runtime",
                TENANT_B,
                "priority:items:enabled=true:lang=en-US",
                "tenant-b",
                Duration.ofMinutes(5)
        );

        assertThat(cacheRuntimeService.get(
                "infra.dictionary.runtime",
                TENANT_A,
                "priority:items:enabled=true:lang=en-US",
                String.class
        )).contains("tenant-a");
        assertThat(cacheRuntimeService.get(
                "infra.dictionary.runtime",
                TENANT_B,
                "priority:items:enabled=true:lang=en-US",
                String.class
        )).contains("tenant-b");
        assertThat(cacheRuntimeService.findKeys("infra.dictionary.runtime", TENANT_A, null))
                .singleElement()
                .satisfies(key -> assertThat(key.tenantId()).isEqualTo(TENANT_A.toString()));

        assertThat(cacheRuntimeService.evictByPrefix("infra.dictionary.runtime", TENANT_A, "priority:"))
                .isEqualTo(1);

        assertThat(cacheRuntimeService.get(
                "infra.dictionary.runtime",
                TENANT_A,
                "priority:items:enabled=true:lang=en-US",
                String.class
        )).isEmpty();
        assertThat(cacheRuntimeService.get(
                "infra.dictionary.runtime",
                TENANT_B,
                "priority:items:enabled=true:lang=en-US",
                String.class
        )).contains("tenant-b");
    }

    @Test
    void shouldClearWholeNamespaceAcrossTenants() {
        DefaultCacheRuntimeService cacheRuntimeService = cacheRuntimeService();
        cacheRuntimeService.put("infra.dictionary.runtime", TENANT_A, "priority:a", "a", Duration.ofMinutes(5));
        cacheRuntimeService.put("infra.dictionary.runtime", TENANT_B, "priority:b", "b", Duration.ofMinutes(5));
        cacheRuntimeService.put("infra.config.runtime", TENANT_A, "config:a", "a", Duration.ofMinutes(5));

        assertThat(cacheRuntimeService.evictNamespace("infra.dictionary.runtime")).isEqualTo(2);

        assertThat(cacheRuntimeService.findKeys("infra.dictionary.runtime", null, null)).isEmpty();
        assertThat(cacheRuntimeService.findKeys("infra.config.runtime", null, null)).hasSize(1);
    }

    private DefaultCacheRuntimeService cacheRuntimeService() {
        return new DefaultCacheRuntimeService(
                Optional.empty(),
                false,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
