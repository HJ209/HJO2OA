package com.hjo2oa.org.role.resource.auth.infrastructure;

import com.hjo2oa.org.role.resource.auth.domain.PermissionCacheKey;
import com.hjo2oa.org.role.resource.auth.domain.PermissionSnapshot;
import com.hjo2oa.org.role.resource.auth.domain.PermissionSnapshotCache;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class InMemoryPermissionSnapshotCache implements PermissionSnapshotCache {

    private final Map<PermissionCacheKey, PermissionSnapshot> snapshots = new LinkedHashMap<>();
    private final AtomicLong version = new AtomicLong(1L);

    @Override
    public synchronized Optional<PermissionSnapshot> get(PermissionCacheKey key) {
        return Optional.ofNullable(snapshots.get(key));
    }

    @Override
    public synchronized PermissionSnapshot put(PermissionCacheKey key, PermissionSnapshot snapshot) {
        snapshots.put(key, snapshot);
        return snapshot;
    }

    @Override
    public synchronized long version(PermissionCacheKey key) {
        return version.get();
    }

    @Override
    public synchronized void invalidateAll() {
        snapshots.clear();
        version.incrementAndGet();
    }
}
