package com.hjo2oa.shared.web;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class SharedGlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController(responseMetaFactory))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void shouldReturnPagedSuccessResponse() throws Exception {
        mockMvc.perform(get("/test/shared/page")
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items[0]").value("A"))
                .andExpect(jsonPath("$.data.pagination.page").value(1))
                .andExpect(jsonPath("$.data.pagination.totalPages").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("req-123"))
                .andExpect(jsonPath("$.meta.timestamp").exists())
                .andExpect(jsonPath("$.meta.serverTimezone").value("UTC"));
    }

    @Test
    void shouldMapBusinessExceptionToUnifiedErrorResponse() throws Exception {
        mockMvc.perform(get("/test/shared/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.meta.requestId").exists());
    }

    @Test
    void shouldResolveErrorMessageThroughConfiguredResolver() throws Exception {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        MockMvc localizedMockMvc = MockMvcBuilders
                .standaloneSetup(new TestController(responseMetaFactory))
                .setControllerAdvice(new SharedGlobalExceptionHandler(
                        responseMetaFactory,
                        (descriptor, fallbackMessage, request) -> Optional.of("Localized conflict")
                ))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        localizedMockMvc.perform(get("/test/shared/business")
                        .header("Accept-Language", "en-US"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Localized conflict"));
    }

    @Test
    void shouldMapValidationExceptionToUnifiedErrorResponse() throws Exception {
        mockMvc.perform(post("/test/shared/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.meta.requestId").exists());
    }

    @Test
    void shouldMapMissingTenantContextToTenantRequiredError() throws Exception {
        mockMvc.perform(get("/test/shared/tenant-required"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TENANT_REQUIRED"))
                .andExpect(jsonPath("$.meta.requestId").exists());
    }

    @Test
    void shouldMapNestedMissingTenantContextToTenantRequiredError() throws Exception {
        mockMvc.perform(get("/test/shared/nested-tenant-required"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TENANT_REQUIRED"))
                .andExpect(jsonPath("$.meta.requestId").exists());
    }

    @RestController
    @Validated
    @UseSharedWebContract
    @RequestMapping("/test/shared")
    public static class TestController {

        private final ResponseMetaFactory responseMetaFactory;

        public TestController(ResponseMetaFactory responseMetaFactory) {
            this.responseMetaFactory = responseMetaFactory;
        }

        @GetMapping("/page")
        public ApiResponse<PageData<String>> page(HttpServletRequest request) {
            return ApiResponse.page(List.of("A", "B"), Pagination.of(1, 20, 2), responseMetaFactory.create(request));
        }

        @GetMapping("/business")
        public ApiResponse<Void> business() {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "identity context conflict");
        }

        @PostMapping("/validation")
        public ApiResponse<String> validation(@Valid @RequestBody TestRequest request, HttpServletRequest servletRequest) {
            return ApiResponse.success(request.name(), responseMetaFactory.create(servletRequest));
        }

        @GetMapping("/tenant-required")
        public ApiResponse<Void> tenantRequired() {
            throw new IllegalStateException("Tenant context is missing X-Tenant-Id");
        }

        @GetMapping("/nested-tenant-required")
        public ApiResponse<Void> nestedTenantRequired() {
            throw new RuntimeException("repository failed",
                    new IllegalStateException("Tenant context is missing X-Tenant-Id"));
        }
    }

    public record TestRequest(@NotBlank(message = "name must not be blank") String name) {
    }
}
