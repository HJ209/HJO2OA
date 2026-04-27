package com.hjo2oa.wf.form.renderer.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.infra.data.i18n.application.TranslationEntryApplicationService;
import com.hjo2oa.infra.data.i18n.infrastructure.InMemoryTranslationEntryRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import com.hjo2oa.wf.form.renderer.application.FormRendererApplicationService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FormRendererControllerTest {

    @Test
    void shouldUseSharedWebContractForRenderAndValidate() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/wf/form-renderer/render")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-form-render-1")
                        .content(requestBody(true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.fields[0].fieldCode").value("amount"))
                .andExpect(jsonPath("$.data.fields[0].editable").value(false))
                .andExpect(jsonPath("$.data.validation.valid").value(true))
                .andExpect(jsonPath("$.meta.requestId").value("req-form-render-1"));

        mockMvc.perform(post("/api/v1/wf/form-renderer/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.errors[0].fieldCode").value("amount"));
    }

    private MockMvc buildMockMvc() {
        TranslationEntryApplicationService translationService = new TranslationEntryApplicationService(
                new InMemoryTranslationEntryRepository(),
                Clock.fixed(Instant.parse("2026-04-24T06:30:00Z"), ZoneOffset.UTC)
        );
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(new FormRendererController(
                        new FormRendererApplicationService(translationService),
                        new FormRendererDtoMapper(),
                        responseMetaFactory
                ))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    private String requestBody(boolean validValue) {
        String amount = validValue ? "300" : "";
        return """
                {
                  "metadataSnapshot": {
                    "metadataId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                    "code": "expense",
                    "name": "报销单",
                    "version": 1,
                    "tenantId": "11111111-1111-1111-1111-111111111111",
                    "layout": {"type": "vertical"},
                    "fields": [
                      {
                        "fieldCode": "amount",
                        "fieldName": "金额",
                        "fieldType": "NUMBER",
                        "required": true,
                        "visible": true,
                        "editable": true,
                        "min": 0,
                        "max": 1000
                      }
                    ],
                    "fieldPermissionMap": {
                      "approve": {
                        "amount": {
                          "editable": false,
                          "required": true
                        }
                      }
                    }
                  },
                  "processInstanceId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                  "formDataId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
                  "nodeId": "approve",
                  "locale": "zh-CN",
                  "formData": {
                    "amount": "%s"
                  }
                }
                """.formatted(amount);
    }
}
