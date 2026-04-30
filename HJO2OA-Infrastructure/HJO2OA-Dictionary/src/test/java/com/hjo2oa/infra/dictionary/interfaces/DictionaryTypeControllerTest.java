package com.hjo2oa.infra.dictionary.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.infra.cache.infrastructure.DefaultCacheRuntimeService;
import com.hjo2oa.infra.dictionary.application.DictionaryCacheService;
import com.hjo2oa.infra.dictionary.application.DictionaryRuntimeService;
import com.hjo2oa.infra.dictionary.application.DictionaryTypeCommands;
import com.hjo2oa.infra.dictionary.application.DictionaryTypeApplicationService;
import com.hjo2oa.infra.dictionary.domain.DictionaryTypeView;
import com.hjo2oa.infra.dictionary.application.SystemEnumDictionaryService;
import com.hjo2oa.infra.dictionary.infrastructure.InMemoryDictionaryTypeRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DictionaryTypeControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T06:30:00Z");

    @Test
    void shouldCreateDictionaryTypeUsingSharedWebContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/infra/dictionaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-dictionary-create-1")
                        .content("""
                                {
                                  "code":"leave-type",
                                  "name":"Leave Type",
                                  "category":"workflow",
                                  "hierarchical":false,
                                  "cacheable":true,
                                  "tenantId":"11111111-1111-1111-1111-111111111111"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.code").value("leave-type"))
                .andExpect(jsonPath("$.meta.requestId").value("req-dictionary-create-1"));
    }

    @Test
    void shouldAddUpdateAndRemoveDictionaryItem() throws Exception {
        DictionaryTypeApplicationService applicationService = applicationService();
        UUID typeId = applicationService.createType(
                "priority",
                "Priority",
                "task",
                false,
                true,
                TENANT_ID
        ).id();
        MockMvc mockMvc = buildMockMvc(applicationService);

        String addResponse = mockMvc.perform(post("/api/v1/infra/dictionaries/{typeId}/items", typeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemCode":"P1",
                                  "displayName":"High",
                                  "sortOrder":10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].itemCode").value("P1"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String itemId = addResponse.replaceAll("(?s).*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/v1/infra/dictionaries/{typeId}/items/{itemId}", typeId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName":"Critical",
                                  "sortOrder":20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].displayName").value("Critical"))
                .andExpect(jsonPath("$.data.items[0].sortOrder").value(20));

        mockMvc.perform(delete("/api/v1/infra/dictionaries/{typeId}/items/{itemId}", typeId, itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void shouldQueryByCodeAndListDictionaries() throws Exception {
        DictionaryTypeApplicationService applicationService = applicationService();
        applicationService.createType("country", "Country", "common", false, true, null);
        applicationService.createType("priority", "Priority", "task", false, true, TENANT_ID);
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/infra/dictionaries/code/country"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("country"));

        mockMvc.perform(get("/api/v1/infra/dictionaries")
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].code").value("priority"));
    }

    @Test
    void shouldDisableAndEnableDictionaryType() throws Exception {
        DictionaryTypeApplicationService applicationService = applicationService();
        UUID typeId = applicationService.createType(
                "notice-level",
                "Notice Level",
                "portal",
                false,
                false,
                TENANT_ID
        ).id();
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(put("/api/v1/infra/dictionaries/{typeId}/disable", typeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(put("/api/v1/infra/dictionaries/{typeId}/enable", typeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void shouldReturnConflictForDuplicateDictionaryCode() throws Exception {
        DictionaryTypeApplicationService applicationService = applicationService();
        applicationService.createType("leave-type", "Leave Type", "workflow", false, true, TENANT_ID);
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(post("/api/v1/infra/dictionaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"leave-type",
                                  "name":"Leave Type Copy",
                                  "category":"workflow",
                                  "hierarchical":false,
                                  "cacheable":true,
                                  "tenantId":"11111111-1111-1111-1111-111111111111"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void shouldExposeRuntimeItemsTreeBatchAndRefreshApis() throws Exception {
        DefaultCacheRuntimeService cacheRuntimeService = new DefaultCacheRuntimeService(
                Optional.empty(),
                false,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        DictionaryTypeApplicationService applicationService = new DictionaryTypeApplicationService(
                new InMemoryDictionaryTypeRepository(),
                event -> {
                },
                new DictionaryCacheService(cacheRuntimeService),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        UUID typeId = applicationService.createType(
                "priority",
                "Priority",
                "task",
                true,
                true,
                TENANT_ID
        ).id();
        DictionaryTypeView withParent = applicationService.addItem(
                typeId,
                TENANT_ID,
                new DictionaryTypeCommands.AddItemCommand(
                        "P1",
                        "High",
                        null,
                        10,
                        true,
                        "HIGH",
                        "{\"color\":\"red\"}"
                )
        );
        UUID parentItemId = withParent.items().get(0).id();
        applicationService.addItem(
                typeId,
                TENANT_ID,
                new DictionaryTypeCommands.AddItemCommand(
                        "P1-1",
                        "Critical",
                        parentItemId,
                        20,
                        false,
                        "CRITICAL",
                        null
                )
        );
        MockMvc mockMvc = buildRuntimeMockMvc(applicationService, cacheRuntimeService);

        mockMvc.perform(get("/api/v1/infra/dictionaries/priority/items")
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .param("language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("P1"))
                .andExpect(jsonPath("$.data[0].value").value("HIGH"))
                .andExpect(jsonPath("$.data[0].defaultItem").value(true));

        mockMvc.perform(get("/api/v1/infra/dictionaries/priority/tree")
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .param("enabledOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].children[0].code").value("P1-1"));

        mockMvc.perform(post("/api/v1/infra/dictionaries/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .content("""
                                {
                                  "codes":["priority"],
                                  "enabledOnly":true,
                                  "tree":false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.priority.items.length()").value(2));

        mockMvc.perform(post("/api/v1/infra/dictionaries/priority/cache/refresh")
                        .header("X-Tenant-Id", TENANT_ID.toString())
                        .param("tree", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("priority"))
                .andExpect(jsonPath("$.data.items[0].children[0].code").value("P1-1"));
    }

    private MockMvc buildMockMvc() {
        return buildMockMvc(applicationService());
    }

    private MockMvc buildMockMvc(DictionaryTypeApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(new DictionaryTypeController(
                        applicationService,
                        new SystemEnumDictionaryService(applicationService),
                        new DictionaryTypeDtoMapper(),
                        responseMetaFactory
                ))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private MockMvc buildRuntimeMockMvc(
            DictionaryTypeApplicationService applicationService,
            DefaultCacheRuntimeService cacheRuntimeService
    ) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(new DictionaryTypeController(
                        applicationService,
                        new SystemEnumDictionaryService(applicationService),
                        new DictionaryRuntimeService(applicationService, cacheRuntimeService, 300),
                        new DictionaryTypeDtoMapper(),
                        responseMetaFactory
                ))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private DictionaryTypeApplicationService applicationService() {
        return new DictionaryTypeApplicationService(
                new InMemoryDictionaryTypeRepository(),
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
