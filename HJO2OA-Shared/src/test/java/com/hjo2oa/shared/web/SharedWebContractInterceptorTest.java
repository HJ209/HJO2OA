package com.hjo2oa.shared.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class SharedWebContractInterceptorTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        SharedWebContractInterceptor interceptor = new SharedWebContractInterceptor(
                new SharedRequestContextFactory(),
                new IdempotencyRegistry()
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController(responseMetaFactory))
                .addInterceptors(interceptor)
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void shouldParseContractHeadersIntoResponseMeta() throws Exception {
        mockMvc.perform(get("/api/test/shared/context")
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-ctx-1")
                        .header(SharedRequestContextFactory.TENANT_ID_HEADER, "11111111-1111-1111-1111-111111111111")
                        .header(SharedRequestContextFactory.ACCEPT_LANGUAGE_HEADER, "en-US,en;q=0.9")
                        .header(SharedRequestContextFactory.TIMEZONE_HEADER, "Asia/Shanghai")
                        .header(SharedRequestContextFactory.IDEMPOTENCY_KEY_HEADER, "idem-read-1"))
                .andExpect(status().isOk())
                .andExpect(header().string(ResponseMetaFactory.REQUEST_ID_HEADER, "req-ctx-1"))
                .andExpect(jsonPath("$.meta.requestId").value("req-ctx-1"))
                .andExpect(jsonPath("$.meta.tenantId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.meta.language").value("en-US"))
                .andExpect(jsonPath("$.meta.timezone").value("Asia/Shanghai"))
                .andExpect(jsonPath("$.meta.idempotencyKey").value("idem-read-1"));
    }

    @Test
    void shouldRejectInvalidTimezoneHeader() throws Exception {
        mockMvc.perform(get("/api/test/shared/context")
                        .header(SharedRequestContextFactory.TIMEZONE_HEADER, "Mars/Base"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.meta.requestId").exists());
    }

    @Test
    void shouldRejectInvalidTenantHeader() throws Exception {
        mockMvc.perform(get("/api/test/shared/context")
                        .header(SharedRequestContextFactory.TENANT_ID_HEADER, "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.meta.requestId").exists());
    }

    @Test
    void shouldRejectIdempotencyKeyReuseForDifferentMutation() throws Exception {
        mockMvc.perform(post("/api/test/shared/mutations/a")
                        .header(SharedRequestContextFactory.TENANT_ID_HEADER, "11111111-1111-1111-1111-111111111111")
                        .header(SharedRequestContextFactory.IDEMPOTENCY_KEY_HEADER, "idem-1"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/test/shared/mutations/b")
                        .header(SharedRequestContextFactory.TENANT_ID_HEADER, "11111111-1111-1111-1111-111111111111")
                        .header(SharedRequestContextFactory.IDEMPOTENCY_KEY_HEADER, "idem-1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    }

    @RestController
    @UseSharedWebContract
    @RequestMapping("/api/test/shared")
    static class TestController {

        private final ResponseMetaFactory responseMetaFactory;

        TestController(ResponseMetaFactory responseMetaFactory) {
            this.responseMetaFactory = responseMetaFactory;
        }

        @GetMapping("/context")
        ApiResponse<String> context(HttpServletRequest request) {
            return ApiResponse.success("ok", responseMetaFactory.create(request));
        }

        @PostMapping("/mutations/a")
        ApiResponse<String> mutateA(HttpServletRequest request) {
            return ApiResponse.success("a", responseMetaFactory.create(request));
        }

        @PostMapping("/mutations/b")
        ApiResponse<String> mutateB(HttpServletRequest request) {
            return ApiResponse.success("b", responseMetaFactory.create(request));
        }
    }
}
