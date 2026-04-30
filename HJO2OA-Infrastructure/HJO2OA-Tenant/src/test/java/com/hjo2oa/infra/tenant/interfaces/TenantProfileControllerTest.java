package com.hjo2oa.infra.tenant.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.infra.tenant.application.TenantProfileApplicationService;
import com.hjo2oa.infra.tenant.domain.IsolationMode;
import com.hjo2oa.infra.tenant.domain.QuotaType;
import com.hjo2oa.infra.tenant.domain.TenantProfileView;
import com.hjo2oa.infra.tenant.infrastructure.InMemoryTenantProfileRepository;
import com.hjo2oa.infra.tenant.infrastructure.InMemoryTenantQuotaRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TenantProfileControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T10:00:00Z");

    @Test
    void shouldCreateTenantUsingSharedWebContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/infra/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-tenant-create-1")
                        .content("""
                                {
                                  "code":"tenant-alpha",
                                  "name":"Tenant Alpha",
                                  "isolationMode":"SHARED_DB",
                                  "packageCode":"basic"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.tenantCode").value("tenant-alpha"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.meta.requestId").value("req-tenant-create-1"));
    }

    @Test
    void shouldActivateTenantAndReturnTenantList() throws Exception {
        TenantProfileApplicationService applicationService = applicationService();
        TenantProfileView createdTenant = applicationService.createTenant(
                "tenant-beta",
                "Tenant Beta",
                IsolationMode.DEDICATED_DB,
                "enterprise"
        );
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(put("/api/v1/infra/tenants/" + createdTenant.id() + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/infra/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].tenantCode").value("tenant-beta"));
    }

    @Test
    void shouldReturnTenantDetailWithQuotaSnapshot() throws Exception {
        TenantProfileApplicationService applicationService = applicationService();
        TenantProfileView createdTenant = applicationService.createTenant(
                "tenant-gamma",
                "Tenant Gamma",
                IsolationMode.SHARED_DB,
                null
        );
        applicationService.updateQuota(createdTenant.id(), QuotaType.API_CALL, 1000, 700L);
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/infra/tenants/" + createdTenant.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantCode").value("tenant-gamma"))
                .andExpect(jsonPath("$.data.quotas.length()").value(5))
                .andExpect(jsonPath("$.data.quotas[0].quotaType").value("API_CALL"))
                .andExpect(jsonPath("$.data.quotas[0].limitValue").value(1000));
    }

    @Test
    void shouldUpdateQuotaUsingSharedWebContract() throws Exception {
        TenantProfileApplicationService applicationService = applicationService();
        TenantProfileView createdTenant = applicationService.createTenant(
                "tenant-delta",
                "Tenant Delta",
                IsolationMode.SHARED_DB,
                null
        );
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(put("/api/v1/infra/tenants/" + createdTenant.id() + "/quotas/JOB_COUNT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-tenant-quota-1")
                        .content("""
                                {
                                  "limitValue":200,
                                  "warningThreshold":150
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.quotaType").value("JOB_COUNT"))
                .andExpect(jsonPath("$.data.limitValue").value(200))
                .andExpect(jsonPath("$.meta.requestId").value("req-tenant-quota-1"));
    }

    @Test
    void shouldUpdateProfileDisableAndConsumeQuota() throws Exception {
        TenantProfileApplicationService applicationService = applicationService();
        TenantProfileView createdTenant = applicationService.createTenant(
                "tenant-epsilon",
                "Tenant Epsilon",
                IsolationMode.SHARED_DB,
                "basic"
        );
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(put("/api/v1/infra/tenants/" + createdTenant.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Tenant Epsilon Updated",
                                  "packageCode":"enterprise",
                                  "defaultLocale":"en-US",
                                  "defaultTimezone":"UTC"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Tenant Epsilon Updated"))
                .andExpect(jsonPath("$.data.defaultLocale").value("en-US"))
                .andExpect(jsonPath("$.data.defaultTimezone").value("UTC"));

        mockMvc.perform(put("/api/v1/infra/tenants/" + createdTenant.id() + "/activate"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/infra/tenants/" + createdTenant.id() + "/quota-usages/API_CALL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "delta":5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quotaType").value("API_CALL"))
                .andExpect(jsonPath("$.data.usedValue").value(5));

        mockMvc.perform(post("/api/v1/infra/tenants/" + createdTenant.id() + "/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }

    private MockMvc buildMockMvc() {
        return buildMockMvc(applicationService());
    }

    private MockMvc buildMockMvc(TenantProfileApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(new TenantProfileController(
                        applicationService,
                        new TenantProfileDtoMapper(),
                        responseMetaFactory
                ))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private TenantProfileApplicationService applicationService() {
        return new TenantProfileApplicationService(
                new InMemoryTenantProfileRepository(),
                new InMemoryTenantQuotaRepository(),
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
