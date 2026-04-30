package com.hjo2oa.infra.audit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hjo2oa.infra.audit.domain.ArchiveStatus;
import com.hjo2oa.infra.audit.domain.AuditQuery;
import com.hjo2oa.infra.audit.domain.AuditRecordView;
import com.hjo2oa.infra.audit.domain.SensitivityLevel;
import com.hjo2oa.infra.audit.infrastructure.InMemoryAuditRecordRepository;
import com.hjo2oa.shared.kernel.BizException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditRecordApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T07:00:00Z");

    @Test
    void shouldRecordAuditWithFieldChanges() {
        AuditRecordApplicationService applicationService = applicationService();

        AuditRecordView view = applicationService.recordAudit(new AuditRecordCommands.RecordAuditCommand(
                "cache",
                "CacheNamespace",
                "portal-home",
                "INVALIDATE",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "trace-cache-1",
                "手工清理首页缓存",
                List.of(new AuditRecordCommands.FieldChangeCommand(
                        "ttlSeconds",
                        "300",
                        "0",
                        SensitivityLevel.MEDIUM
                ))
        ));

        assertEquals("cache", view.moduleCode());
        assertEquals(ArchiveStatus.ACTIVE, view.archiveStatus());
        assertEquals(FIXED_TIME, view.occurredAt());
        assertEquals(1, view.fieldChanges().size());
    }

    @Test
    void shouldArchiveExistingRecord() {
        AuditRecordApplicationService applicationService = applicationService();
        AuditRecordView created = applicationService.recordAudit(new AuditRecordCommands.RecordAuditCommand(
                "config",
                "ConfigItem",
                "feature-x",
                "UPDATE",
                null,
                null,
                null,
                null,
                "更新配置",
                List.of()
        ));

        AuditRecordView archived = applicationService.archiveRecord(created.id());

        assertEquals(ArchiveStatus.ARCHIVED, archived.archiveStatus());
        assertEquals(created.id(), archived.id());
    }

    @Test
    void shouldQueryAuditsByTenantAndModule() {
        AuditRecordApplicationService applicationService = applicationService();
        UUID tenantId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        applicationService.recordAudit(new AuditRecordCommands.RecordAuditCommand(
                "tenant",
                "TenantProfile",
                "tenant-1",
                "CREATE",
                null,
                null,
                tenantId,
                null,
                null,
                List.of()
        ));
        applicationService.recordAudit(new AuditRecordCommands.RecordAuditCommand(
                "config",
                "ConfigItem",
                "config-1",
                "UPDATE",
                null,
                null,
                tenantId,
                null,
                null,
                List.of()
        ));

        List<AuditRecordView> result = applicationService.queryAudits(new AuditQuery(
                tenantId,
                "tenant",
                null,
                null,
                null,
                null,
                null,
                null,
                FIXED_TIME.minusSeconds(1),
                FIXED_TIME.plusSeconds(1)
        ));

        assertEquals(1, result.size());
        assertEquals("tenant", result.get(0).moduleCode());
    }

    @Test
    void shouldThrowWhenArchivingMissingRecord() {
        AuditRecordApplicationService applicationService = applicationService();

        BizException exception = assertThrows(
                BizException.class,
                () -> applicationService.archiveRecord(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"))
        );

        assertEquals(AuditErrorDescriptors.AUDIT_RECORD_NOT_FOUND.code(), exception.errorCode());
    }

    private AuditRecordApplicationService applicationService() {
        return new AuditRecordApplicationService(
                new InMemoryAuditRecordRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
