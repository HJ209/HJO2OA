package com.hjo2oa.infra.config.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.infra.config.application.ConfigEntryApplicationService;
import com.hjo2oa.infra.config.domain.ConfigType;
import com.hjo2oa.infra.config.domain.OverrideScopeType;
import com.hjo2oa.infra.config.infrastructure.InMemoryConfigEntryRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ConfigEntryControllerTest {

    @Test
    void shouldCreateConfigEntryUsingSharedWebContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/infra/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-config-create-1")
                        .content("""
                                {
                                  "configKey":"portal.home.layout",
                                  "name":"Portal Home Layout",
                                  "configType":"JSON",
                                  "defaultValue":"{\\"mode\\":\\"compact\\"}",
                                  "mutableAtRuntime":true,
                                  "tenantAware":true,
                                  "validationRule":"{\\"required\\":true}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.configKey").value("portal.home.layout"))
                .andExpect(jsonPath("$.data.tenantAware").value(true))
                .andExpect(jsonPath("$.meta.requestId").value("req-config-create-1"));
    }

    @Test
    void shouldAddAndRemoveOverrideAndResolveFinalValue() throws Exception {
        ConfigEntryApplicationService applicationService = applicationService();
        UUID entryId = applicationService.createEntry(
                "todo.page.size",
                "Todo Page Size",
                ConfigType.NUMBER,
                "20",
                true,
                true,
                "{\"min\":1,\"max\":100}"
        ).id();
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID overrideId = applicationService.addOverride(entryId, OverrideScopeType.TENANT, tenantId, "30")
                .overrides()
                .get(0)
                .id();
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(post("/api/v1/infra/configs/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "key":"todo.page.size",
                                  "tenantId":"11111111-1111-1111-1111-111111111111"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resolvedValue").value("30"))
                .andExpect(jsonPath("$.data.sourceType").value("OVERRIDE"));

        mockMvc.perform(delete("/api/v1/infra/configs/{entryId}/overrides/{overrideId}", entryId, overrideId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overrides.length()").value(0));
    }

    @Test
    void shouldAddFeatureRuleDisableAndListConfigs() throws Exception {
        ConfigEntryApplicationService applicationService = applicationService();
        UUID entryId = applicationService.createEntry(
                "portal.beta.enabled",
                "Portal Beta Enabled",
                ConfigType.FEATURE_FLAG,
                "false",
                true,
                true,
                null
        ).id();
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(post("/api/v1/infra/configs/{entryId}/feature-rules", entryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleType":"GLOBAL",
                                  "ruleValue":"true",
                                  "sortOrder":0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featureRules[0].ruleType").value("GLOBAL"));

        mockMvc.perform(put("/api/v1/infra/configs/{entryId}/disable", entryId)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-config-disable-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"))
                .andExpect(jsonPath("$.meta.requestId").value("req-config-disable-1"));

        mockMvc.perform(get("/api/v1/infra/configs").param("configType", "FEATURE_FLAG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].configKey").value("portal.beta.enabled"));
    }

    private MockMvc buildMockMvc() {
        return buildMockMvc(applicationService());
    }

    private MockMvc buildMockMvc(ConfigEntryApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        ConfigEntryDtoMapper dtoMapper = new ConfigEntryDtoMapper();
        return MockMvcBuilders.standaloneSetup(new ConfigEntryController(
                        applicationService,
                        dtoMapper,
                        responseMetaFactory
                ))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private ConfigEntryApplicationService applicationService() {
        return new ConfigEntryApplicationService(
                new InMemoryConfigEntryRepository(),
                event -> {
                },
                new ObjectMapper().findAndRegisterModules()
        );
    }
}
