package com.hjo2oa.msg.mobile.support.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.msg.mobile.support.application.MobileSupportApplicationService;
import com.hjo2oa.msg.mobile.support.infrastructure.InMemoryMobileSupportRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MobileSupportControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-27T01:00:00Z");

    @Test
    void shouldBindDeviceAndCreateSession() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(post("/api/v1/msg/mobile-support/devices/bind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"11111111-1111-1111-1111-111111111111",
                                  "personId":"22222222-2222-2222-2222-222222222222",
                                  "accountId":"33333333-3333-3333-3333-333333333333",
                                  "deviceId":"device-1",
                                  "deviceFingerprint":"fingerprint-1",
                                  "platform":"IOS",
                                  "appType":"NATIVE_APP",
                                  "pushToken":"push-token-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.deviceId").value("device-1"))
                .andExpect(jsonPath("$.data.bindStatus").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/msg/mobile-support/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"11111111-1111-1111-1111-111111111111",
                                  "personId":"22222222-2222-2222-2222-222222222222",
                                  "accountId":"33333333-3333-3333-3333-333333333333",
                                  "deviceId":"device-1",
                                  "currentAssignmentId":"44444444-4444-4444-4444-444444444444",
                                  "currentPositionId":"55555555-5555-5555-5555-555555555555",
                                  "ttlSeconds":7200
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.sessionStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.refreshVersion").value(0));
    }

    private static MockMvc mockMvc() {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        MobileSupportApplicationService service = new MobileSupportApplicationService(
                new InMemoryMobileSupportRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        return MockMvcBuilders.standaloneSetup(new MobileSupportController(
                        service,
                        new MobileSupportDtoMapper(),
                        responseMetaFactory
                ))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }
}
