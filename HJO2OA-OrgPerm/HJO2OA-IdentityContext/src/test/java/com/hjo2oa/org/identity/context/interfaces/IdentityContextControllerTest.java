package com.hjo2oa.org.identity.context.interfaces;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.org.identity.context.application.IdentityContextQueryApplicationService;
import com.hjo2oa.org.identity.context.application.IdentityContextRefreshApplicationService;
import com.hjo2oa.org.identity.context.application.IdentityContextSwitchApplicationService;
import com.hjo2oa.org.identity.context.domain.IdentityContextSessionRepository;
import com.hjo2oa.org.identity.context.infrastructure.InMemoryIdentityContextSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IdentityContextControllerTest {

    @Test
    void shouldReturnRecoveredRefreshOutcome() throws Exception {
        MockMvc mockMvc = mockMvc(new InMemoryIdentityContextSessionRepository());

        mockMvc.perform(post("/api/org-perm/identity-context/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "tenant-1",
                                  "personId": "person-1",
                                  "accountId": "account-1",
                                  "invalidatedAssignmentId": "assignment-1",
                                  "fallbackAssignmentId": "assignment-2",
                                  "reasonCode": "POSITION_DISABLED",
                                  "forceLogout": false,
                                  "permissionSnapshotVersion": 42,
                                  "triggerEvent": "org.position.disabled"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("RECOVERED"))
                .andExpect(jsonPath("$.forceLogout").value(false))
                .andExpect(jsonPath("$.currentContext.currentAssignmentId").value("assignment-2"))
                .andExpect(jsonPath("$.currentContext.currentPositionId").value("position-2"))
                .andExpect(jsonPath("$.currentContext.permissionSnapshotVersion").value(42));
    }

    @Test
    void shouldReturnFallbackToPrimaryOutcome() throws Exception {
        IdentityContextSessionRepository sessionRepository = new InMemoryIdentityContextSessionRepository(
                new InMemoryIdentityContextSessionRepository().currentSession()
                        .withCurrentAssignment("assignment-2", 2L, java.time.Instant.parse("2026-04-19T08:30:00Z"))
        );
        MockMvc mockMvc = mockMvc(sessionRepository);

        mockMvc.perform(post("/api/org-perm/identity-context/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "tenant-1",
                                  "personId": "person-1",
                                  "accountId": "account-1",
                                  "invalidatedAssignmentId": "assignment-2",
                                  "reasonCode": "ASSIGNMENT_EXPIRED",
                                  "forceLogout": false,
                                  "permissionSnapshotVersion": 0,
                                  "triggerEvent": "org.assignment.expired"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("FALLBACK_TO_PRIMARY"))
                .andExpect(jsonPath("$.currentContext.currentAssignmentId").value("assignment-1"))
                .andExpect(jsonPath("$.currentContext.currentPositionId").value("position-1"));
    }

    @Test
    void shouldReturnReloginRequiredOutcome() throws Exception {
        MockMvc mockMvc = mockMvc(new InMemoryIdentityContextSessionRepository());

        mockMvc.perform(post("/api/org-perm/identity-context/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "tenant-1",
                                  "personId": "person-1",
                                  "accountId": "account-1",
                                  "invalidatedAssignmentId": "assignment-1",
                                  "reasonCode": "ACCOUNT_LOCKED",
                                  "forceLogout": true,
                                  "permissionSnapshotVersion": 88,
                                  "triggerEvent": "org.account.locked"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("RELOGIN_REQUIRED"))
                .andExpect(jsonPath("$.forceLogout").value(true))
                .andExpect(jsonPath("$.currentContext").value(nullValue()));
    }

    @Test
    void shouldRejectInvalidRefreshRequest() throws Exception {
        MockMvc mockMvc = mockMvc(new InMemoryIdentityContextSessionRepository());

        mockMvc.perform(post("/api/org-perm/identity-context/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "",
                                  "personId": "person-1",
                                  "accountId": "account-1",
                                  "invalidatedAssignmentId": "assignment-1",
                                  "reasonCode": "POSITION_DISABLED",
                                  "forceLogout": false,
                                  "permissionSnapshotVersion": -1,
                                  "triggerEvent": "org.position.disabled"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private static MockMvc mockMvc(IdentityContextSessionRepository sessionRepository) {
        IdentityContextController controller = new IdentityContextController(
                new IdentityContextQueryApplicationService(sessionRepository),
                new IdentityContextSwitchApplicationService(sessionRepository, event -> {
                }),
                new IdentityContextRefreshApplicationService(event -> {
                }, sessionRepository)
        );
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new IdentityContextExceptionHandler())
                .build();
    }
}
