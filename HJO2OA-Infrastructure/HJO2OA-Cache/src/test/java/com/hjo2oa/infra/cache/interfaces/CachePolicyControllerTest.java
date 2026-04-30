package com.hjo2oa.infra.cache.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.infra.cache.application.CachePolicyApplicationService;
import com.hjo2oa.infra.cache.domain.CacheBackendType;
import com.hjo2oa.infra.cache.domain.CachePolicyView;
import com.hjo2oa.infra.cache.domain.EvictionPolicy;
import com.hjo2oa.infra.cache.domain.InvalidationMode;
import com.hjo2oa.infra.cache.infrastructure.DefaultCacheRuntimeService;
import com.hjo2oa.infra.cache.infrastructure.InMemoryCachePolicyRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CachePolicyControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T06:00:00Z");
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void shouldCreatePolicyUsingSharedWebContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/infra/cache/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-cache-create-1")
                        .content("""
                                {
                                  "namespace":"portal.home",
                                  "backendType":"REDIS",
                                  "ttlSeconds":300,
                                  "maxCapacity":1000,
                                  "evictionPolicy":"LRU",
                                  "invalidationMode":"MANUAL"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.namespace").value("portal.home"))
                .andExpect(jsonPath("$.data.backendType").value("REDIS"))
                .andExpect(jsonPath("$.meta.requestId").value("req-cache-create-1"));
    }

    @Test
    void shouldUpdatePolicyUsingSharedWebContract() throws Exception {
        CachePolicyApplicationService applicationService = applicationService();
        CachePolicyView created = applicationService.createPolicy(
                "portal.home",
                CacheBackendType.REDIS,
                300,
                1000,
                EvictionPolicy.LRU,
                InvalidationMode.MANUAL
        );
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(put("/api/v1/infra/cache/policies/{policyId}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ttlSeconds":600,
                                  "evictionPolicy":"LFU"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.ttlSeconds").value(600))
                .andExpect(jsonPath("$.data.evictionPolicy").value("LFU"));
    }

    @Test
    void shouldDeactivatePolicyUsingSharedWebContract() throws Exception {
        CachePolicyApplicationService applicationService = applicationService();
        CachePolicyView created = applicationService.createPolicy(
                "portal.home",
                CacheBackendType.REDIS,
                300,
                1000,
                EvictionPolicy.LRU,
                InvalidationMode.MANUAL
        );
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(put("/api/v1/infra/cache/policies/{policyId}/deactivate", created.id())
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-cache-deactivate-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.meta.requestId").value("req-cache-deactivate-1"));
    }

    @Test
    void shouldInvalidateKeyUsingSharedWebContract() throws Exception {
        CachePolicyApplicationService applicationService = applicationService();
        applicationService.createPolicy(
                "portal.home",
                CacheBackendType.REDIS,
                300,
                1000,
                EvictionPolicy.LRU,
                InvalidationMode.MANUAL
        );
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(post("/api/v1/infra/cache/policies/invalidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "namespace":"portal.home",
                                  "key":"home:widget:1",
                                  "reasonType":"MANUAL",
                                  "reasonRef":"ticket-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.namespace").value("portal.home"))
                .andExpect(jsonPath("$.data.invalidateKey").value("home:widget:1"))
                .andExpect(jsonPath("$.data.reasonType").value("MANUAL"));
    }

    @Test
    void shouldQueryByNamespace() throws Exception {
        CachePolicyApplicationService applicationService = applicationService();
        applicationService.createPolicy(
                "portal.home",
                CacheBackendType.REDIS,
                300,
                1000,
                EvictionPolicy.LRU,
                InvalidationMode.MANUAL
        );
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/infra/cache/policies/namespace/{namespace}", "portal.home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.namespace").value("portal.home"));
    }

    @Test
    void shouldListPolicies() throws Exception {
        CachePolicyApplicationService applicationService = applicationService();
        applicationService.createPolicy(
                "portal.home",
                CacheBackendType.REDIS,
                300,
                1000,
                EvictionPolicy.LRU,
                InvalidationMode.MANUAL
        );
        applicationService.createPolicy(
                "portal.office",
                CacheBackendType.MEMORY,
                60,
                null,
                EvictionPolicy.NONE,
                InvalidationMode.MANUAL
        );
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/infra/cache/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].namespace").value("portal.home"));
    }

    @Test
    void shouldRejectDuplicateNamespaceAsConflict() throws Exception {
        CachePolicyApplicationService applicationService = applicationService();
        applicationService.createPolicy(
                "portal.home",
                CacheBackendType.REDIS,
                300,
                1000,
                EvictionPolicy.LRU,
                InvalidationMode.MANUAL
        );
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(post("/api/v1/infra/cache/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "namespace":"portal.home",
                                  "backendType":"MEMORY",
                                  "ttlSeconds":60,
                                  "evictionPolicy":"NONE",
                                  "invalidationMode":"MANUAL"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INFRA_CACHE_NAMESPACE_CONFLICT"));
    }

    @Test
    void shouldExposeRuntimeManagementApi() throws Exception {
        DefaultCacheRuntimeService cacheRuntimeService = new DefaultCacheRuntimeService(
                Optional.empty(),
                false,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        CachePolicyApplicationService applicationService = new CachePolicyApplicationService(
                new InMemoryCachePolicyRepository(),
                event -> {
                },
                cacheRuntimeService,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        applicationService.createPolicy(
                "infra.dictionary.runtime",
                CacheBackendType.MEMORY,
                300,
                1000,
                EvictionPolicy.LRU,
                InvalidationMode.MANUAL
        );
        cacheRuntimeService.put(
                "infra.dictionary.runtime",
                TENANT_ID,
                "priority:items:enabled=true:lang=en-US",
                "cached",
                Duration.ofMinutes(5)
        );
        MockMvc mockMvc = buildRuntimeMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/infra/cache/keys")
                        .param("namespace", "infra.dictionary.runtime")
                        .param("tenantId", TENANT_ID.toString())
                        .param("keyword", "priority"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].namespace").value("infra.dictionary.runtime"))
                .andExpect(jsonPath("$.data[0].key").value("priority:items:enabled=true:lang=en-US"))
                .andExpect(jsonPath("$.data[0].backendType").value("MEMORY"));

        mockMvc.perform(get("/api/v1/infra/cache/metrics/infra.dictionary.runtime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.namespace").value("infra.dictionary.runtime"))
                .andExpect(jsonPath("$.data.keyCount").value(1));

        mockMvc.perform(post("/api/v1/infra/cache/namespaces/infra.dictionary.runtime/clear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reasonRef":"ops"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.namespace").value("infra.dictionary.runtime"))
                .andExpect(jsonPath("$.data.invalidateKey").value("*"))
                .andExpect(jsonPath("$.data.reasonRef").value("ops"));

        mockMvc.perform(get("/api/v1/infra/cache/invalidations")
                        .param("namespace", "infra.dictionary.runtime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].invalidateKey").value("*"));
    }

    private MockMvc buildMockMvc() {
        return buildMockMvc(applicationService());
    }

    private MockMvc buildMockMvc(CachePolicyApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(
                        new CachePolicyController(applicationService, new CachePolicyDtoMapper(), responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private MockMvc buildRuntimeMockMvc(CachePolicyApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(
                        new CacheRuntimeController(applicationService, new CachePolicyDtoMapper(), responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private CachePolicyApplicationService applicationService() {
        return new CachePolicyApplicationService(
                new InMemoryCachePolicyRepository(),
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
