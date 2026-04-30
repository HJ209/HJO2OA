package com.hjo2oa.org.role.resource.auth.domain;

import java.util.Optional;

public interface PermissionSnapshotCache extends PermissionCacheInvalidator {

    Optional<PermissionSnapshot> get(PermissionCacheKey key);

    PermissionSnapshot put(PermissionCacheKey key, PermissionSnapshot snapshot);

    long version(PermissionCacheKey key);
}
