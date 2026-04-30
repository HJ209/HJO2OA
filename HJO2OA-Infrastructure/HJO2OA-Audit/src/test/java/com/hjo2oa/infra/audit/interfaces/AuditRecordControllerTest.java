package com.hjo2oa.infra.audit.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.infra.audit.application.AuditRecordApplicationService;
import com.hjo2oa.infra.audit.application.AuditRecordCommands;
import com.hjo2oa.infra.audit.infrastructure.InMemoryAuditRecordRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuditRecordControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T08:00:00Z");

    @Test
    void shouldRecordAuditUsingSharedWebContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/infra/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-audit-create-1")
                        .content("""
                                {
                                  "moduleCode":"security",
                                  "objectType":"LoginEvent",
                                  "objectId":"login-1",
                                  "actionType":"LOGIN_SUCCEEDED",
                                  "tenantId":"11111111-1111-1111-1111-111111111111",
                                  "traceId":"trace-login-1",
                                  "summary":"用户登录成功",
                                  "fieldChanges":[
                                    {
                                      "fieldName":"loginIp",
                                      "oldValue":"10.0.0.1",
                                      "newValue":"10.0.0.2",
                                      "sensitivityLevel":"LOW"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.moduleCode").value("security"))
                .andExpect(jsonPath("$.data.fieldChanges.length()").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("req-audit-create-1"));
    }

    @Test
    void shouldQueryAuditListWithFilters() throws Exception {
        AuditRecordApplicationService applicationService = applicationService();
        UUID tenantId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID operatorAccountId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        applicationService.recordAudit(new AuditRecordCommands.RecordAuditCommand(
                "config",
                "ConfigItem",
                "config-1",
                "UPDATE",
                operatorAccountId,
                null,
                tenantId,
                "trace-config-1",
                "修改配置",
                List.of()
        ));
        applicationService.recordAudit(new AuditRecordCommands.RecordAuditCommand(
                "cache",
                "CacheNamespace",
                "cache-1",
                "INVALIDATE",
                null,
                null,
                tenantId,
                null,
                "清理缓存",
                List.of()
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/infra/audits")
                        .param("tenantId", tenantId.toString())
                        .param("moduleCode", "config")
                        .param("operatorAccountId", operatorAccountId.toString())
                        .param("requestId", "trace-config-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].moduleCode").value("config"));
    }

    @Test
    void shouldReturnAuditDetailAndArchiveRecord() throws Exception {
        AuditRecordApplicationService applicationService = applicationService();
        UUID recordId = applicationService.recordAudit(new AuditRecordCommands.RecordAuditCommand(
                "tenant",
                "TenantProfile",
                "tenant-42",
                "CREATE",
                null,
                null,
                null,
                null,
                "创建租户",
                List.of()
        )).id();
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(get("/api/v1/infra/audits/{recordId}", recordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.objectId").value("tenant-42"));

        mockMvc.perform(put("/api/v1/infra/audits/{recordId}/archive", recordId)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-audit-archive-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.archiveStatus").value("ARCHIVED"))
                .andExpect(jsonPath("$.meta.requestId").value("req-audit-archive-1"));
    }

    @Test
    void shouldReturnNotFoundForMissingRecord() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(get("/api/v1/infra/audits/{recordId}", UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INFRA_AUDIT_RECORD_NOT_FOUND"));
    }

    private MockMvc buildMockMvc() {
        return buildMockMvc(applicationService());
    }

    private MockMvc buildMockMvc(AuditRecordApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(new AuditRecordController(
                        applicationService,
                        new AuditRecordDtoMapper(),
                        responseMetaFactory
                ))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private AuditRecordApplicationService applicationService() {
        return new AuditRecordApplicationService(
                new InMemoryAuditRecordRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
