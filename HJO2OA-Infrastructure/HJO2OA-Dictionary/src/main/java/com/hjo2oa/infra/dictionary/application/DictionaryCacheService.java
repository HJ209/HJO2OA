package com.hjo2oa.infra.dictionary.application;

import com.hjo2oa.infra.cache.application.CacheRuntimeService;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DictionaryCacheService {

    public static final String NAMESPACE = "infra.dictionary.runtime";

    private final CacheRuntimeService cacheRuntimeService;

    public DictionaryCacheService(CacheRuntimeService cacheRuntimeService) {
        this.cacheRuntimeService = Objects.requireNonNull(cacheRuntimeService, "cacheRuntimeService must not be null");
    }

    public int invalidate(UUID tenantId, String dictionaryCode) {
        return cacheRuntimeService.evictByPrefix(NAMESPACE, tenantId, dictionaryCode + ":");
    }
}
