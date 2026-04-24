package com.hjo2oa.infra.i18n.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.infra.i18n.application.LocaleBundleApplicationService;
import com.hjo2oa.infra.i18n.infrastructure.InMemoryLocaleBundleRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LocaleBundleControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T06:00:00Z");

    @Test
    void shouldUseSharedWebContractForBundleLifecycleAndResolve() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        String createResponse = mockMvc.perform(post("/api/v1/infra/i18n/bundles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-i18n-create-1")
                        .content("""
                                {
                                  "bundleCode":"portal.messages",
                                  "moduleCode":"portal",
                                  "locale":"en-US"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.bundleCode").value("portal.messages"))
                .andExpect(jsonPath("$.meta.requestId").value("req-i18n-create-1"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String bundleId = createResponse.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/v1/infra/i18n/bundles/" + bundleId + "/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceKey":"greeting",
                                  "resourceValue":"Hello"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entries[0].resourceKey").value("greeting"))
                .andExpect(jsonPath("$.data.entries[0].resourceValue").value("Hello"));

        mockMvc.perform(put("/api/v1/infra/i18n/bundles/" + bundleId + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        String zhCreateResponse = mockMvc.perform(post("/api/v1/infra/i18n/bundles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bundleCode":"portal.messages",
                                  "moduleCode":"portal",
                                  "locale":"zh-CN",
                                  "fallbackLocale":"en-US"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String zhBundleId = zhCreateResponse.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/v1/infra/i18n/bundles/" + zhBundleId + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/infra/i18n/bundles/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bundleCode":"portal.messages",
                                  "resourceKey":"greeting",
                                  "locale":"zh-CN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resourceValue").value("Hello"))
                .andExpect(jsonPath("$.data.resolvedLocale").value("en-us"))
                .andExpect(jsonPath("$.data.usedFallback").value(true));

        mockMvc.perform(get("/api/v1/infra/i18n/bundles/code/portal.messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/v1/infra/i18n/bundles/module/portal/locale/en-US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].bundleCode").value("portal.messages"));

        mockMvc.perform(put("/api/v1/infra/i18n/bundles/" + bundleId + "/entries/greeting")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceValue":"Hi"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entries[0].resourceValue").value("Hi"));

        mockMvc.perform(delete("/api/v1/infra/i18n/bundles/" + bundleId + "/entries/greeting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entries.length()").value(0));
    }

    @Test
    void shouldReturnNotFoundWhenBundleCodeDoesNotExist() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(get("/api/v1/infra/i18n/bundles/code/unknown.bundle"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private MockMvc buildMockMvc() {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        LocaleBundleApplicationService applicationService = new LocaleBundleApplicationService(
                new InMemoryLocaleBundleRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        LocaleBundleDtoMapper dtoMapper = new LocaleBundleDtoMapper();
        return MockMvcBuilders.standaloneSetup(
                        new LocaleBundleController(applicationService, dtoMapper, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }
}
