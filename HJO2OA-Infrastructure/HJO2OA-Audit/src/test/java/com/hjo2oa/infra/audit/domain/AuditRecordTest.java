package com.hjo2oa.infra.audit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditRecordTest {

    @Test
    void shouldCreateActiveAuditRecordAndProjectView() {
        UUID recordId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-24T06:00:00Z");
        AuditFieldChange fieldChange = AuditFieldChange.create(
                recordId,
                "status",
                "DRAFT",
                "ACTIVE",
                SensitivityLevel.LOW
        );

        AuditRecord record = AuditRecord.create(
                recordId,
                "config",
                "ConfigItem",
                "feature.toggle",
                "UPDATE",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "trace-audit-1",
                "切换配置开关",
                now,
                now,
                List.of(fieldChange)
        );

        AuditRecordView view = record.toView();

        assertEquals(ArchiveStatus.ACTIVE, record.archiveStatus());
        assertEquals("config", record.moduleCode());
        assertEquals(1, view.fieldChanges().size());
        assertEquals("status", view.fieldChanges().get(0).fieldName());
        assertNotNull(view.fieldChanges().get(0).id());
    }

    @Test
    void shouldArchiveRecordWithoutChangingFactData() {
        UUID recordId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-24T06:10:00Z");
        AuditRecord record = AuditRecord.create(
                recordId,
                "tenant",
                "TenantProfile",
                "tenant-1",
                "ARCHIVE_REQUEST",
                null,
                null,
                UUID.randomUUID(),
                null,
                null,
                now,
                now,
                List.of()
        );

        AuditRecord archived = record.archive();

        assertEquals(ArchiveStatus.ARCHIVED, archived.archiveStatus());
        assertEquals(record.id(), archived.id());
        assertEquals(record.occurredAt(), archived.occurredAt());
        assertEquals(record.createdAt(), archived.createdAt());
    }
}
