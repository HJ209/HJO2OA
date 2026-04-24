package com.hjo2oa.infra.data.i18n.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.infra.data.i18n.application.TranslationEntryApplicationService;
import com.hjo2oa.infra.data.i18n.infrastructure.InMemoryTranslationEntryRepository;
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

class TranslationEntryControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T06:30:00Z");

    @Test
    void shouldUseSharedWebContractForTranslationCrudAndResolve() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        String response = mockMvc.perform(post("/api/v1/infra/translations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-translation-create-1")
                        .content("""
                                {
                                  "entityType":"article",
                                  "entityId":"A-100",
                                  "fieldName":"title",
                                  "locale":"zh-CN",
                                  "value":"中文标题",
                                  "tenantId":"11111111-1111-1111-1111-111111111111"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.translationStatus").value("TRANSLATED"))
                .andExpect(jsonPath("$.meta.requestId").value("req-translation-create-1"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String entryId = response.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/v1/infra/translations/" + entryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "value":"中文标题-控制器更新"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.translatedValue").value("中文标题-控制器更新"));

        mockMvc.perform(put("/api/v1/infra/translations/" + entryId + "/review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.translationStatus").value("REVIEWED"));

        mockMvc.perform(post("/api/v1/infra/translations/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entityType":"article",
                                  "entityId":"A-100",
                                  "fieldName":"title",
                                  "locale":"en-US",
                                  "tenantId":"11111111-1111-1111-1111-111111111111",
                                  "fallbackLocale":"zh-CN",
                                  "originalValue":"Article Title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resolveSource").value("FALLBACK"))
                .andExpect(jsonPath("$.data.resolvedLocale").value("zh-CN"))
                .andExpect(jsonPath("$.data.resolvedValue").value("中文标题-控制器更新"));

        mockMvc.perform(get("/api/v1/infra/translations/entity/article/A-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].fieldName").value("title"));
    }

    @Test
    void shouldBatchSaveTranslations() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/infra/translations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entries":[
                                    {
                                      "entityType":"article",
                                      "entityId":"A-200",
                                      "fieldName":"title",
                                      "locale":"zh-CN",
                                      "value":"标题一",
                                      "tenantId":"11111111-1111-1111-1111-111111111111"
                                    },
                                    {
                                      "entityType":"article",
                                      "entityId":"A-200",
                                      "fieldName":"summary",
                                      "locale":"zh-CN",
                                      "value":"摘要一",
                                      "tenantId":"11111111-1111-1111-1111-111111111111"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].translationStatus").value("TRANSLATED"));
    }

    @Test
    void shouldRejectDuplicateTranslationCreationAsConflict() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        String createBody = """
                {
                  "entityType":"article",
                  "entityId":"A-300",
                  "fieldName":"title",
                  "locale":"zh-CN",
                  "value":"中文标题",
                  "tenantId":"11111111-1111-1111-1111-111111111111"
                }
                """;

        mockMvc.perform(post("/api/v1/infra/translations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/infra/translations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    private MockMvc buildMockMvc() {
        TranslationEntryApplicationService applicationService = new TranslationEntryApplicationService(
                new InMemoryTranslationEntryRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(new TranslationEntryController(
                        applicationService,
                        new TranslationEntryDtoMapper(),
                        responseMetaFactory
                ))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }
}
