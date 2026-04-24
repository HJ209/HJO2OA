package com.hjo2oa.infra.cache.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CachePolicyRepository {

    Optional<CachePolicy> findById(UUID policyId);

    Optional<CachePolicy> findByNamespace(String namespace);

    List<CachePolicy> findAll();

    CachePolicy save(CachePolicy cachePolicy);

    CacheInvalidationRecord saveInvalidationRecord(CacheInvalidationRecord invalidationRecord);
}
