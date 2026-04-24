package com.hjo2oa.infra.errorcode.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.infra.errorcode.application.ErrorCodeDefinitionApplicationService;
import com.hjo2oa.infra.errorcode.domain.ErrorSeverity;
import com.hjo2oa.infra.errorcode.infrastructure.InMemoryErrorCodeDefinitionRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ErrorCodeDefinitionControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T04:00:00Z");

    @Test
    void shouldDefineQueryAndMutateUsingSharedWebContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        String response = mockMvc.perform(post("/api/v1/infra/error-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-error-create-1")
                        .content("""
                                {
                                  "code":"INFRA_4001",
                                  "moduleCode":"infra",
                                  "category":"event-bus",
                                  "severity":"ERROR",
                                  "httpStatus":400,
                                  "messageKey":"infra.error.invalid",
                                  "retryable":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.code").value("INFRA_4001"))
                .andExpect(jsonPath("$.data.moduleCode").value("infra"))
                .andExpect(jsonPath("$.meta.requestId").value("req-error-create-1"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String codeId = response.replaceAll("(?s).*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/v1/infra/error-codes/" + codeId + "/severity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "severity":"FATAL"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.severity").value("FATAL"));

        mockMvc.perform(put("/api/v1/infra/error-codes/" + codeId + "/http-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "httpStatus":503
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.httpStatus").value(503));

        mockMvc.perform(get("/api/v1/infra/error-codes/code/INFRA_4001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageKey").value("infra.error.invalid"));

        mockMvc.perform(get("/api/v1/infra/error-codes/module/infra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].code").value("INFRA_4001"));

        mockMvc.perform(put("/api/v1/infra/error-codes/" + codeId + "/deprecate")
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-error-deprecate-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deprecated").value(true))
                .andExpect(jsonPath("$.meta.requestId").value("req-error-deprecate-1"));
    }

    @Test
    void shouldRejectDuplicateCodeAsConflict() throws Exception {
        ErrorCodeDefinitionApplicationService applicationService = applicationService();
        applicationService.defineCode(
                "INFRA_4090",
                "infra",
                ErrorSeverity.ERROR,
                409,
                "infra.error.conflict",
                null,
                false
        );
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(post("/api/v1/infra/error-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"INFRA_4090",
                                  "moduleCode":"infra",
                                  "severity":"ERROR",
                                  "httpStatus":409,
                                  "messageKey":"infra.error.conflict"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    private MockMvc buildMockMvc() {
        return buildMockMvc(applicationService());
    }

    private MockMvc buildMockMvc(ErrorCodeDefinitionApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(new ErrorCodeDefinitionController(
                        applicationService,
                        new ErrorCodeDefinitionDtoMapper(),
                        responseMetaFactory
                ))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private ErrorCodeDefinitionApplicationService applicationService() {
        return new ErrorCodeDefinitionApplicationService(
                new InMemoryErrorCodeDefinitionRepository(),
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
