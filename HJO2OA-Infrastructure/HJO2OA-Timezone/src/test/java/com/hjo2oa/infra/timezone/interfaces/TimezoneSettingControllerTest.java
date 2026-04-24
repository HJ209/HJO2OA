package com.hjo2oa.infra.timezone.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.infra.timezone.application.TimezoneSettingApplicationService;
import com.hjo2oa.infra.timezone.infrastructure.InMemoryTimezoneSettingRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TimezoneSettingControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T08:00:00Z");

    @Test
    void shouldSetSystemDefaultUsingSharedWebContract() throws Exception {
        MockMvc mockMvc = buildMockMvc(applicationService());

        mockMvc.perform(put("/api/v1/infra/timezone/settings/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-timezone-system-1")
                        .content("""
                                {"timezoneId":"Asia/Shanghai"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.scopeType").value("SYSTEM"))
                .andExpect(jsonPath("$.data.timezoneId").value("Asia/Shanghai"))
                .andExpect(jsonPath("$.meta.requestId").value("req-timezone-system-1"));
    }

    @Test
    void shouldResolveEffectiveTimezone() throws Exception {
        TimezoneSettingApplicationService applicationService = applicationService();
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID personId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        applicationService.setSystemDefault("UTC");
        applicationService.setTenantTimezone(tenantId, "Europe/Berlin");
        applicationService.setPersonTimezone(personId, "America/New_York");
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/infra/timezone/settings/resolve")
                        .param("tenantId", tenantId.toString())
                        .param("personId", personId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.scopeType").value("PERSON"))
                .andExpect(jsonPath("$.data.timezoneId").value("America/New_York"));
    }

    @Test
    void shouldConvertToUtcAndFromUtc() throws Exception {
        MockMvc mockMvc = buildMockMvc(applicationService());

        mockMvc.perform(post("/api/v1/infra/timezone/settings/convert/to-utc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"localDateTime":"2026-04-20T16:00:00","timezoneId":"Asia/Shanghai"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.utcInstant").value("2026-04-20T08:00:00Z"));

        mockMvc.perform(post("/api/v1/infra/timezone/settings/convert/from-utc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"utcInstant":"2026-04-20T08:00:00Z","timezoneId":"Asia/Shanghai"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.localDateTime").value("2026-04-20T16:00:00"));
    }

    @Test
    void shouldReturnBadRequestWhenTenantIdIsInvalid() throws Exception {
        MockMvc mockMvc = buildMockMvc(applicationService());

        mockMvc.perform(put("/api/v1/infra/timezone/settings/tenant/not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"timezoneId":"Asia/Shanghai"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private MockMvc buildMockMvc(TimezoneSettingApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return MockMvcBuilders.standaloneSetup(new TimezoneSettingController(
                        applicationService,
                        new TimezoneSettingDtoMapper(),
                        responseMetaFactory
                ))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private TimezoneSettingApplicationService applicationService() {
        Clock fixedClock = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);
        return new TimezoneSettingApplicationService(new InMemoryTimezoneSettingRepository(fixedClock), fixedClock);
    }
}
