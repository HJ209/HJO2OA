package com.hjo2oa.org.org.sync.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrgSyncAuditDomainTest {

    @Test
    void sourceConfigShouldKeepCredentialsAsConfigReference() {
        Instant now = Instant.parse("2026-04-27T00:00:00Z");
        SyncSourceConfig source = SyncSourceConfig.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "hr",
                "HR",
                "HR",
                "https://example.invalid",
                "config/org/hr",
                "org=*",
                now
        );

        assertThat(source.status()).isEqualTo(SourceStatus.DISABLED);
        assertThat(source.configRef()).isEqualTo("config/org/hr");
        assertThat(source.enable(now.plusSeconds(1)).status()).isEqualTo(SourceStatus.ENABLED);
    }

    @Test
    void diffResolutionShouldRequireExplicitStatusTransition() {
        Instant now = Instant.parse("2026-04-27T00:00:00Z");
        DiffRecord diffRecord = DiffRecord.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PERSON",
                "P001",
                DiffType.CONFLICT,
                "{\"name\":\"source\"}",
                "{\"name\":\"local\"}",
                "manual review",
                now
        );

        UUID operatorId = UUID.randomUUID();
        DiffRecord resolved = diffRecord.resolve(operatorId, now.plusSeconds(10));

        assertThat(diffRecord.status()).isEqualTo(DiffStatus.PENDING);
        assertThat(resolved.status()).isEqualTo(DiffStatus.RESOLVED);
        assertThat(resolved.resolvedBy()).isEqualTo(operatorId);
        assertThat(resolved.resolvedAt()).isEqualTo(now.plusSeconds(10));
    }

    @Test
    void auditRecordShouldRetainSnapshotsAndTaskContext() {
        UUID taskId = UUID.randomUUID();
        AuditRecord auditRecord = AuditRecord.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                AuditCategory.DIFF_GOVERNANCE,
                "RESOLVE",
                "DEPARTMENT",
                "D001",
                taskId,
                "MANUAL",
                UUID.randomUUID(),
                "{\"name\":\"old\"}",
                "{\"name\":\"new\"}",
                "resolved",
                Instant.parse("2026-04-27T00:00:00Z")
        );

        assertThat(auditRecord.beforeSnapshot()).contains("old");
        assertThat(auditRecord.afterSnapshot()).contains("new");
        assertThat(auditRecord.taskId()).isEqualTo(taskId);
    }
}
