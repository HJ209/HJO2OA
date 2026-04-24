package com.hjo2oa.data.governance.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.data.governance.application.GovernanceProfileApplicationService;
import com.hjo2oa.data.governance.application.GovernanceMonitoringApplicationService;
import com.hjo2oa.data.governance.infrastructure.InMemoryGovernanceProfileRepository;
import com.hjo2oa.data.governance.infrastructure.InMemoryGovernanceRuntimeRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GovernanceVersionControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T03:00:00Z");

    @Test
    void shouldRegisterPublishAndDeprecateVersionUsingSharedWebContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/data/governance/versions/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-version-register")
                        .content("""
                                {
                                  "tenantId":"tenant-1",
                                  "profileCode":"gov-api-order",
                                  "targetType":"API",
                                  "targetCode":"api.order.query",
                                  "version":"v1",
                                  "compatibilityNote":"兼容历史调用",
                                  "changeSummary":"首次登记",
                                  "approvalNote":"同意登记",
                                  "operatorId":"admin-1",
                                  "operatorName":"管理员",
                                  "requestId":"req-version-register"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("REGISTERED"))
                .andExpect(jsonPath("$.meta.requestId").value("req-version-register"));

        mockMvc.perform(post("/api/v1/data/governance/versions/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"tenant-1",
                                  "profileCode":"gov-api-order",
                                  "version":"v1",
                                  "approvalNote":"允许发布",
                                  "operatorId":"admin-1",
                                  "operatorName":"管理员",
                                  "requestId":"req-version-publish"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(post("/api/v1/data/governance/versions/deprecate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"tenant-1",
                                  "profileCode":"gov-api-order",
                                  "version":"v1",
                                  "approvalNote":"切换到 v2",
                                  "operatorId":"admin-1",
                                  "operatorName":"管理员",
                                  "requestId":"req-version-deprecate"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DEPRECATED"));

        mockMvc.perform(get("/api/v1/data/governance/versions")
                        .param("tenantId", "tenant-1")
                        .param("targetType", "API")
                        .param("targetCode", "api.order.query"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].version").value("v1"))
                .andExpect(jsonPath("$.data.items[0].status").value("DEPRECATED"));
    }

    private MockMvc buildMockMvc() {
        InMemoryGovernanceProfileRepository profileRepository = new InMemoryGovernanceProfileRepository();
        InMemoryGovernanceRuntimeRepository runtimeRepository = new InMemoryGovernanceRuntimeRepository();
        GovernanceProfileApplicationService profileApplicationService = new GovernanceProfileApplicationService(
                profileRepository,
                runtimeRepository,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        profileApplicationService.upsertProfile(new com.hjo2oa.data.governance.application.GovernanceCommands.UpsertGovernanceProfileCommand(
                "tenant-1",
                "gov-api-order",
                com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceScopeType.API,
                "api.order.query",
                null,
                "{\"allowedActions\":[]}",
                com.hjo2oa.data.governance.domain.GovernanceTypes.GovernanceProfileStatus.ACTIVE,
                "admin-1",
                "管理员",
                "req-profile-version"
        ));
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(new GovernanceVersionController(profileApplicationService, responseMetaFactory))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }
}
