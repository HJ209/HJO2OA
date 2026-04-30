package com.hjo2oa.infra.security.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hjo2oa.infra.security.application.MaskingService;
import com.hjo2oa.infra.security.application.PasswordPolicyService;
import com.hjo2oa.infra.security.application.SecurityPolicyApplicationService;
import com.hjo2oa.infra.security.domain.SecurityPolicyType;
import com.hjo2oa.infra.security.infrastructure.DefaultCryptoService;
import com.hjo2oa.infra.security.infrastructure.InMemorySecurityPolicyRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SecurityRuntimeControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();

    @Test
    void shouldEncryptDecryptMaskAndValidatePassword() throws Exception {
        InMemorySecurityPolicyRepository repository = new InMemorySecurityPolicyRepository();
        SecurityPolicyApplicationService policyService = new SecurityPolicyApplicationService(
                repository,
                event -> {
                },
                Clock.fixed(Instant.parse("2026-04-29T08:00:00Z"), ZoneOffset.UTC)
        );
        var keyPolicy = policyService.createPolicy(
                "key-policy",
                SecurityPolicyType.KEY_MANAGEMENT,
                "Key policy",
                "{}",
                null
        );
        policyService.addSecretKey(keyPolicy.id(), "customer-data", "AES");
        var maskingPolicy = policyService.createPolicy(
                "mask-policy",
                SecurityPolicyType.MASKING,
                "Mask policy",
                "{}",
                null
        );
        policyService.addMaskingRule(maskingPolicy.id(), "phone", "KEEP_SUFFIX(4)");
        policyService.createPolicy(
                "password-policy",
                SecurityPolicyType.PASSWORD,
                "Password policy",
                "{\"minLength\":10,\"requireUppercase\":true,\"requireLowercase\":true,"
                        + "\"requireDigit\":true,\"requireSpecial\":true,\"historyCount\":2}",
                null
        );
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        SecurityRuntimeController controller = new SecurityRuntimeController(
                new DefaultCryptoService(repository, "unit-test-master-secret"),
                new MaskingService(repository, OBJECT_MAPPER),
                new PasswordPolicyService(
                        repository,
                        OBJECT_MAPPER,
                        PasswordEncoderFactories.createDelegatingPasswordEncoder()
                ),
                responseMetaFactory
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();

        String encryptedPayload = mockMvc.perform(post("/api/v1/infra/security/crypto/encrypt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "keyRef":"customer-data",
                                  "algorithm":"AES",
                                  "value":"secret-value"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.keyRef").value("customer-data"))
                .andExpect(jsonPath("$.data.algorithm").value("AES"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String ciphertext = OBJECT_MAPPER.readTree(encryptedPayload).path("data").path("value").asText();

        mockMvc.perform(post("/api/v1/infra/security/crypto/decrypt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new SecurityPolicyDtos.CryptoRequest(
                                "customer-data",
                                "AES",
                                ciphertext
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value("secret-value"));

        mockMvc.perform(post("/api/v1/infra/security/masking/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "policyCode":"mask-policy",
                                  "dataType":"phone",
                                  "value":"13812345678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maskedValue").value("*******5678"));

        mockMvc.perform(post("/api/v1/infra/security/password/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "policyCode":"password-policy",
                                  "username":"alice",
                                  "password":"Short1!",
                                  "passwordHistory":["OldPassword1!"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(false))
                .andExpect(jsonPath("$.data.violations[0]").value("PASSWORD_TOO_SHORT"));
    }
}
