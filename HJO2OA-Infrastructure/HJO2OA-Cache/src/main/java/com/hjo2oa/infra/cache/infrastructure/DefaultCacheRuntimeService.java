package com.hjo2oa.infra.cache.infrastructure;

import com.hjo2oa.infra.cache.application.CacheRuntimeService;
import com.hjo2oa.infra.cache.domain.CacheBackendType;
import com.hjo2oa.infra.cache.domain.CacheRuntimeKeyView;
import com.hjo2oa.infra.cache.domain.CacheRuntimeMetricsView;
import com.hjo2oa.shared.cache.RedisCacheOperations;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DefaultCacheRuntimeService implements CacheRuntimeService {

    private static final String REDIS_KEY_PREFIX = "hjo2oa:runtime-cache:";
    private static final String GLOBAL_TENANT = "GLOBAL";

    private final ConcurrentMap<RuntimeKey, LocalEntry> localEntries = new ConcurrentHashMap<>();
    private final Set<RuntimeKey> knownKeys = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String, StatsBucket> statsByNamespace = new ConcurrentHashMap<>();
    private final Optional<RedisCacheOperations> redisCacheOperations;
    private final boolean redisEnabled;
    private final Clock clock;

    @Autowired
    public DefaultCacheRuntimeService(
            ObjectProvider<RedisCacheOperations> redisCacheOperationsProvider,
            @Value("${hjo2oa.cache.runtime.redis-enabled:true}") boolean redisEnabled
    ) {
        this(Optional.ofNullable(redisCacheOperationsProvider.getIfAvailable()), redisEnabled, Clock.systemUTC());
    }

    public DefaultCacheRuntimeService(
            Optional<RedisCacheOperations> redisCacheOperations,
            boolean redisEnabled,
            Clock clock
    ) {
        this.redisCacheOperations = Objects.requireNonNull(
                redisCacheOperations,
                "redisCacheOperations must not be null"
        );
        this.redisEnabled = redisEnabled;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public <T> Optional<T> get(String namespace, UUID tenantId, String key, Class<T> type) {
        RuntimeKey runtimeKey = runtimeKey(namespace, tenantId, key);
        Objects.requireNonNull(type, "type must not be null");
        cleanupIfExpired(runtimeKey);
        LocalEntry localEntry = localEntries.get(runtimeKey);
        StatsBucket stats = stats(runtimeKey.namespace());
        if (localEntry != null) {
            if (!type.isInstance(localEntry.value())) {
                throw new IllegalStateException("Cached value type mismatch for key " + runtimeKey.key());
            }
            stats.localHits.increment();
            return Optional.of(type.cast(localEntry.value()));
        }
        Optional<T> redisValue = getFromRedis(runtimeKey, type);
        if (redisValue.isPresent()) {
            stats.redisHits.increment();
            putLocal(runtimeKey, redisValue.get(), Duration.ofMinutes(5));
            return redisValue;
        }
        stats.misses.increment();
        return Optional.empty();
    }

    @Override
    public <T> T getOrLoad(
            String namespace,
            UUID tenantId,
            String key,
            Duration ttl,
            Class<T> type,
            Supplier<T> loader
    ) {
        return get(namespace, tenantId, key, type).orElseGet(() -> {
            T loadedValue = Objects.requireNonNull(loader.get(), "loader returned null");
            put(namespace, tenantId, key, loadedValue, ttl);
            return loadedValue;
        });
    }

    @Override
    public void put(String namespace, UUID tenantId, String key, Object value, Duration ttl) {
        RuntimeKey runtimeKey = runtimeKey(namespace, tenantId, key);
        Objects.requireNonNull(value, "value must not be null");
        Duration resolvedTtl = resolveTtl(ttl);
        putLocal(runtimeKey, value, resolvedTtl);
        putRedis(runtimeKey, value, resolvedTtl);
        knownKeys.add(runtimeKey);
        stats(runtimeKey.namespace()).puts.increment();
    }

    @Override
    public boolean evictKey(String namespace, UUID tenantId, String key) {
        RuntimeKey runtimeKey = runtimeKey(namespace, tenantId, key);
        boolean removedLocal = localEntries.remove(runtimeKey) != null;
        boolean removedKnown = knownKeys.remove(runtimeKey);
        deleteRedis(runtimeKey);
        if (removedLocal || removedKnown) {
            stats(runtimeKey.namespace()).invalidations.increment();
            return true;
        }
        return false;
    }

    @Override
    public int evictByPrefix(String namespace, UUID tenantId, String keyPrefix) {
        String normalizedNamespace = requireText(namespace, "namespace");
        String normalizedPrefix = requireText(keyPrefix, "keyPrefix");
        String tenantKey = tenantKey(tenantId);
        List<RuntimeKey> keys = knownKeys.stream()
                .filter(key -> key.namespace().equals(normalizedNamespace))
                .filter(key -> tenantId == null || key.tenantKey().equals(tenantKey))
                .filter(key -> key.key().startsWith(normalizedPrefix))
                .toList();
        keys.forEach(key -> evictKey(key.namespace(), key.tenantUuid(), key.key()));
        return keys.size();
    }

    @Override
    public int evictNamespace(String namespace) {
        String normalizedNamespace = requireText(namespace, "namespace");
        List<RuntimeKey> keys = knownKeys.stream()
                .filter(key -> key.namespace().equals(normalizedNamespace))
                .toList();
        keys.forEach(key -> evictKey(key.namespace(), key.tenantUuid(), key.key()));
        return keys.size();
    }

    @Override
    public List<CacheRuntimeKeyView> findKeys(String namespace, UUID tenantId, String keyword) {
        String normalizedNamespace = normalizeNullableText(namespace);
        String normalizedKeyword = normalizeNullableText(keyword);
        String tenantKey = tenantId == null ? null : tenantKey(tenantId);
        knownKeys.forEach(this::cleanupIfExpired);
        return knownKeys.stream()
                .filter(key -> normalizedNamespace == null || key.namespace().equals(normalizedNamespace))
                .filter(key -> tenantKey == null || key.tenantKey().equals(tenantKey))
                .filter(key -> normalizedKeyword == null || key.key().contains(normalizedKeyword))
                .map(this::toView)
                .sorted(Comparator.comparing(CacheRuntimeKeyView::namespace)
                        .thenComparing(CacheRuntimeKeyView::tenantId)
                        .thenComparing(CacheRuntimeKeyView::key))
                .toList();
    }

    @Override
    public List<CacheRuntimeMetricsView> metrics() {
        return statsByNamespace.keySet().stream()
                .sorted()
                .map(this::metrics)
                .toList();
    }

    @Override
    public CacheRuntimeMetricsView metrics(String namespace) {
        String normalizedNamespace = requireText(namespace, "namespace");
        StatsBucket stats = stats(normalizedNamespace);
        int keyCount = (int) knownKeys.stream()
                .filter(key -> key.namespace().equals(normalizedNamespace))
                .count();
        return new CacheRuntimeMetricsView(
                normalizedNamespace,
                stats.localHits.sum(),
                stats.redisHits.sum(),
                stats.misses.sum(),
                stats.puts.sum(),
                stats.invalidations.sum(),
                keyCount
        );
    }

    private void putLocal(RuntimeKey runtimeKey, Object value, Duration ttl) {
        localEntries.put(runtimeKey, new LocalEntry(value, expiresAt(ttl)));
        knownKeys.add(runtimeKey);
    }

    private <T> Optional<T> getFromRedis(RuntimeKey runtimeKey, Class<T> type) {
        if (!redisEnabled || redisCacheOperations.isEmpty()) {
            return Optional.empty();
        }
        try {
            return redisCacheOperations.get().get(redisKey(runtimeKey), type);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private void putRedis(RuntimeKey runtimeKey, Object value, Duration ttl) {
        if (!redisEnabled || redisCacheOperations.isEmpty()) {
            return;
        }
        try {
            redisCacheOperations.get().set(redisKey(runtimeKey), value, ttl);
        } catch (RuntimeException ex) {
            // Local cache remains authoritative for this process when Redis is unavailable.
        }
    }

    private void deleteRedis(RuntimeKey runtimeKey) {
        if (!redisEnabled || redisCacheOperations.isEmpty()) {
            return;
        }
        try {
            redisCacheOperations.get().delete(redisKey(runtimeKey));
        } catch (RuntimeException ex) {
            // Best effort invalidation; local keys are already removed.
        }
    }

    private void cleanupIfExpired(RuntimeKey runtimeKey) {
        LocalEntry localEntry = localEntries.get(runtimeKey);
        if (localEntry == null || localEntry.expiresAt() == null || localEntry.expiresAt().isAfter(now())) {
            return;
        }
        localEntries.remove(runtimeKey);
        knownKeys.remove(runtimeKey);
    }

    private CacheRuntimeKeyView toView(RuntimeKey runtimeKey) {
        LocalEntry localEntry = localEntries.get(runtimeKey);
        CacheBackendType backendType = localEntry == null ? CacheBackendType.REDIS : CacheBackendType.MEMORY;
        return new CacheRuntimeKeyView(
                runtimeKey.namespace(),
                runtimeKey.tenantKey(),
                runtimeKey.key(),
                backendType,
                localEntry == null ? null : localEntry.expiresAt()
        );
    }

    private StatsBucket stats(String namespace) {
        return statsByNamespace.computeIfAbsent(namespace, ignored -> new StatsBucket());
    }

    private RuntimeKey runtimeKey(String namespace, UUID tenantId, String key) {
        return new RuntimeKey(
                requireText(namespace, "namespace"),
                tenantKey(tenantId),
                requireText(key, "key")
        );
    }

    private static String tenantKey(UUID tenantId) {
        return tenantId == null ? GLOBAL_TENANT : tenantId.toString();
    }

    private static String redisKey(RuntimeKey runtimeKey) {
        return REDIS_KEY_PREFIX
                + runtimeKey.namespace()
                + ":tenant:"
                + runtimeKey.tenantKey()
                + ":"
                + runtimeKey.key();
    }

    private Instant expiresAt(Duration ttl) {
        Duration resolvedTtl = resolveTtl(ttl);
        return resolvedTtl.isZero() || resolvedTtl.isNegative() ? null : now().plus(resolvedTtl);
    }

    private Instant now() {
        return clock.instant();
    }

    private static Duration resolveTtl(Duration ttl) {
        return ttl == null ? Duration.ofMinutes(5) : ttl;
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

    private record RuntimeKey(String namespace, String tenantKey, String key) {

        private UUID tenantUuid() {
            return GLOBAL_TENANT.equals(tenantKey) ? null : UUID.fromString(tenantKey);
        }
    }

    private record LocalEntry(Object value, Instant expiresAt) {
    }

    private static final class StatsBucket {

        private final LongAdder localHits = new LongAdder();
        private final LongAdder redisHits = new LongAdder();
        private final LongAdder misses = new LongAdder();
        private final LongAdder puts = new LongAdder();
        private final LongAdder invalidations = new LongAdder();
    }
}
