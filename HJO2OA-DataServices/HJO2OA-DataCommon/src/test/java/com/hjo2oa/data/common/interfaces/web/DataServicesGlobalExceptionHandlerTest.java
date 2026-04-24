package com.hjo2oa.data.common.interfaces.web;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.data.common.domain.exception.DataServicesErrorCode;
import com.hjo2oa.data.common.domain.exception.DataServicesException;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.PageData;
import com.hjo2oa.shared.web.Pagination;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
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

class DataServicesGlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController(responseMetaFactory))
                .setControllerAdvice(new DataServicesGlobalExceptionHandler(responseMetaFactory))
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void shouldReturnDataServicesBusinessErrorResponse() throws Exception {
        mockMvc.perform(get("/test/data/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(DataServicesErrorCode.DATA_SERVICE_NOT_FOUND.code()))
                .andExpect(jsonPath("$.message").value("service definition missing"))
                .andExpect(jsonPath("$.meta.requestId").exists());
    }

    @Test
    void shouldReturnValidationErrorResponse() throws Exception {
        mockMvc.perform(post("/test/data/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR.code()))
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.meta.requestId").exists());
    }

    @Test
    void shouldReturnPagedSuccessResponse() throws Exception {
        mockMvc.perform(get("/test/data/page")
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "data-req-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items[0]").value("service-a"))
                .andExpect(jsonPath("$.data.pagination.totalPages").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("data-req-1"));
    }

    @RestController
    @Validated
    @RequestMapping("/test/data")
    static class TestController {

        private final ResponseMetaFactory responseMetaFactory;

        TestController(ResponseMetaFactory responseMetaFactory) {
            this.responseMetaFactory = responseMetaFactory;
        }

        @GetMapping("/business")
        ApiResponse<Void> business() {
            throw new DataServicesException(DataServicesErrorCode.DATA_SERVICE_NOT_FOUND, "service definition missing");
        }

        @PostMapping("/validation")
        ApiResponse<String> validation(@Valid @RequestBody TestRequest request, HttpServletRequest servletRequest) {
            return ApiResponse.success(request.name(), responseMetaFactory.create(servletRequest));
        }

        @GetMapping("/page")
        ApiResponse<PageData<String>> page(HttpServletRequest request) {
            return ApiResponse.page(
                    List.of("service-a", "service-b"),
                    Pagination.of(1, 20, 2),
                    responseMetaFactory.create(request)
            );
        }
    }

    record TestRequest(@NotBlank(message = "name must not be blank") String name) {
    }
}
