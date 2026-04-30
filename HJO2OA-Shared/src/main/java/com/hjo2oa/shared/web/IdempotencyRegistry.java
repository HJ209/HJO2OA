package com.hjo2oa.shared.web;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyRegistry {

    private static final Duration ENTRY_TTL = Duration.ofHours(24);
    private static final int CLEANUP_INTERVAL = 256;

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private int cleanupCounter;

    public void register(String tenantId, String key, String fingerprint, Instant now) {
        if (key == null || key.isBlank()) {
            return;
        }
        cleanup(now);
        String scopedKey = scopedKey(tenantId, key);
        Entry current = entries.putIfAbsent(scopedKey, new Entry(fingerprint, now.plus(ENTRY_TTL)));
        if (current != null && current.expiresAt().isAfter(now) && !current.fingerprint().equals(fingerprint)) {
            throw new IdempotencyConflictException("X-Idempotency-Key has already been used for another request");
        }
        if (current != null && !current.expiresAt().isAfter(now)) {
            entries.replace(scopedKey, current, new Entry(fingerprint, now.plus(ENTRY_TTL)));
        }
    }

    private void cleanup(Instant now) {
        cleanupCounter++;
        if (cleanupCounter % CLEANUP_INTERVAL != 0) {
            return;
        }
        entries.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private static String scopedKey(String tenantId, String key) {
        return (tenantId == null || tenantId.isBlank() ? "global" : tenantId) + ":" + key;
    }

    private record Entry(String fingerprint, Instant expiresAt) {
    }
}
