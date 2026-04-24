package com.hjo2oa.infra.security.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hjo2oa.infra.security.application.SecurityPolicyApplicationService;
import com.hjo2oa.infra.security.domain.SecurityPolicyType;
import com.hjo2oa.infra.security.domain.SecurityPolicyView;
import com.hjo2oa.infra.security.infrastructure.InMemorySecurityPolicyRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SecurityPolicyControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();

    @Test
    void shouldCreateAndListPoliciesUsingSharedWebContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/infra/security/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-security-create-1")
                        .content(OBJECT_MAPPER.writeValueAsString(new SecurityPolicyDtos.CreatePolicyRequest(
                                "mask-policy",
                                SecurityPolicyType.MASKING,
                                "Masking policy",
                                "{\"scene\":\"portal\"}",
                                null
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.policyCode").value("mask-policy"))
                .andExpect(jsonPath("$.data.policyType").value("MASKING"))
                .andExpect(jsonPath("$.meta.requestId").value("req-security-create-1"));

        mockMvc.perform(get("/api/v1/infra/security/policies")
                        .param("policyType", "MASKING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Masking policy"));
    }

    @Test
    void shouldMaskValueThroughController() throws Exception {
        InMemorySecurityPolicyRepository repository = new InMemorySecurityPolicyRepository();
        SecurityPolicyApplicationService applicationService = new SecurityPolicyApplicationService(
                repository,
                event -> {
                },
                Clock.fixed(Instant.parse("2026-04-24T06:00:00Z"), ZoneOffset.UTC)
        );
        SecurityPolicyView createdPolicy = applicationService.createPolicy(
                "customer-mask",
                SecurityPolicyType.MASKING,
                "Customer masking policy",
                "{\"scene\":\"preview\"}",
                null
        );
        applicationService.addMaskingRule(createdPolicy.id(), "PHONE", "KEEP_SUFFIX(4)");
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(post("/api/v1/infra/security/policies/mask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new SecurityPolicyDtos.MaskValueRequest(
                                "customer-mask",
                                "PHONE",
                                "13812345678"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.maskedValue").value("*******5678"));
    }

    private MockMvc buildMockMvc() {
        InMemorySecurityPolicyRepository repository = new InMemorySecurityPolicyRepository();
        SecurityPolicyApplicationService applicationService = new SecurityPolicyApplicationService(
                repository,
                event -> {
                },
                Clock.fixed(Instant.parse("2026-04-24T06:00:00Z"), ZoneOffset.UTC)
        );
        return buildMockMvc(applicationService);
    }

    private MockMvc buildMockMvc(SecurityPolicyApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(
                        new SecurityPolicyController(
                                applicationService,
                                new SecurityPolicyDtoMapper(),
                                responseMetaFactory
                        )
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }
}
