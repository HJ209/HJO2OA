package com.hjo2oa.org.org.sync.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.org.org.sync.audit.application.OrgSyncAuditApplicationService;
import com.hjo2oa.org.org.sync.audit.application.OrgSyncAuditCommands;
import com.hjo2oa.org.org.sync.audit.application.TargetMasterDataResult;
import com.hjo2oa.org.org.sync.audit.domain.AuditCategory;
import com.hjo2oa.org.org.sync.audit.domain.AuditRecord;
import com.hjo2oa.org.org.sync.audit.domain.AuditRecordRepository;
import com.hjo2oa.org.org.sync.audit.domain.AuditRecordView;
import com.hjo2oa.org.org.sync.audit.domain.CompensationRecord;
import com.hjo2oa.org.org.sync.audit.domain.CompensationRecordRepository;
import com.hjo2oa.org.org.sync.audit.domain.CompensationRecordView;
import com.hjo2oa.org.org.sync.audit.domain.CompensationStatus;
import com.hjo2oa.org.org.sync.audit.domain.DiffRecord;
import com.hjo2oa.org.org.sync.audit.domain.DiffRecordRepository;
import com.hjo2oa.org.org.sync.audit.domain.DiffRecordView;
import com.hjo2oa.org.org.sync.audit.domain.DiffStatus;
import com.hjo2oa.org.org.sync.audit.domain.DiffType;
import com.hjo2oa.org.org.sync.audit.domain.SourceStatus;
import com.hjo2oa.org.org.sync.audit.domain.SyncSourceConfig;
import com.hjo2oa.org.org.sync.audit.domain.SyncSourceConfigRepository;
import com.hjo2oa.org.org.sync.audit.domain.SyncSourceConfigView;
import com.hjo2oa.org.org.sync.audit.domain.SyncTask;
import com.hjo2oa.org.org.sync.audit.domain.SyncTaskRepository;
import com.hjo2oa.org.org.sync.audit.domain.SyncTaskStatus;
import com.hjo2oa.org.org.sync.audit.domain.SyncTaskType;
import com.hjo2oa.org.org.sync.audit.domain.SyncTaskView;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrgSyncAuditIntegrationTest {

    private final InMemorySourceRepository sourceRepository = new InMemorySourceRepository();
    private final InMemoryTaskRepository taskRepository = new InMemoryTaskRepository();
    private final InMemoryDiffRepository diffRepository = new InMemoryDiffRepository();
    private final InMemoryCompensationRepository compensationRepository = new InMemoryCompensationRepository();
    private final InMemoryAuditRepository auditRepository = new InMemoryAuditRepository();
    private final OrgSyncAuditApplicationService service = new OrgSyncAuditApplicationService(
            sourceRepository,
            taskRepository,
            diffRepository,
            compensationRepository,
            auditRepository,
            (diff, actionType, requestPayload) -> new TargetMasterDataResult(true, "applied:" + requestPayload),
            Clock.fixed(Instant.parse("2026-04-27T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void shouldCoverSourceTaskDiffAuditAndCompensationChain() {
        UUID tenantId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();

        SyncSourceConfigView createdSource = service.createSource(new OrgSyncAuditCommands.CreateSourceCommand(
                tenantId,
                "ldap-main",
                "LDAP Main",
                "LDAP",
                "ldap://directory.local",
                "config/org-sync/ldap-main",
                "ou=users",
                operatorId
        ));
        SyncSourceConfigView enabledSource = service.enableSource(createdSource.id(), operatorId);

        assertThat(createdSource.status()).isEqualTo(SourceStatus.DISABLED);
        assertThat(enabledSource.status()).isEqualTo(SourceStatus.ENABLED);
        assertThat(enabledSource.configRef()).isEqualTo("config/org-sync/ldap-main");

        SyncTaskView task = service.startFullTask(new OrgSyncAuditCommands.StartTaskCommand(
                tenantId,
                enabledSource.id(),
                "MANUAL",
                operatorId
        ));
        assertThat(task.taskType()).isEqualTo(SyncTaskType.FULL);
        assertThat(task.status()).isEqualTo(SyncTaskStatus.COMPLETED);
        assertThat(task.startedAt()).isNotNull();
        assertThat(task.finishedAt()).isNotNull();

        DiffRecordView diff = service.createDiff(new OrgSyncAuditCommands.CreateDiffCommand(
                tenantId,
                task.id(),
                "PERSON",
                "u1001",
                DiffType.FIELD_MISMATCH,
                "{\"name\":\"Alice Source\"}",
                "{\"name\":\"Alice Local\"}",
                "USE_SOURCE"
        ));
        assertThat(diff.diffType()).isEqualTo(DiffType.FIELD_MISMATCH);
        assertThat(diff.status()).isEqualTo(DiffStatus.PENDING);
        assertThat(diff.sourceSnapshot()).contains("Alice Source");
        assertThat(diff.localSnapshot()).contains("Alice Local");

        List<AuditRecordView> audits = service.queryAudits(new OrgSyncAuditCommands.AuditQuery(
                tenantId,
                null,
                null,
                null,
                task.id(),
                null,
                null
        ));
        assertThat(audits)
                .extracting(AuditRecordView::triggerSource)
                .contains("MANUAL", "SYNC_TASK");
        assertThat(audits)
                .extracting(AuditRecordView::afterSnapshot)
                .anySatisfy(snapshot -> assertThat(snapshot).contains("Alice Source"));
        assertThat(audits).isSortedAccordingTo(Comparator.comparing(AuditRecordView::occurredAt));

        CompensationRecordView compensation = service.resolveDiff(new OrgSyncAuditCommands.ResolveDiffCommand(
                diff.id(),
                "APPLY_SOURCE",
                "{\"entityKey\":\"u1001\"}",
                operatorId
        ));
        assertThat(compensation.status()).isEqualTo(CompensationStatus.APPLIED);
        assertThat(compensation.taskId()).isEqualTo(task.id());
        assertThat(compensation.diffRecordId()).isEqualTo(diff.id());
        assertThat(compensation.resultPayload()).contains("applied");
    }

    private static final class InMemorySourceRepository implements SyncSourceConfigRepository {
        private final Map<UUID, SyncSourceConfig> sources = new LinkedHashMap<>();

        @Override
        public SyncSourceConfig save(SyncSourceConfig config) {
            sources.put(config.id(), config);
            return config;
        }

        @Override
        public Optional<SyncSourceConfig> findById(UUID sourceId) {
            return Optional.ofNullable(sources.get(sourceId));
        }

        @Override
        public Optional<SyncSourceConfig> findByTenantIdAndSourceCode(UUID tenantId, String sourceCode) {
            return sources.values().stream()
                    .filter(source -> source.tenantId().equals(tenantId))
                    .filter(source -> source.sourceCode().equals(sourceCode))
                    .findFirst();
        }

        @Override
        public List<SyncSourceConfig> findByTenantId(UUID tenantId) {
            return sources.values().stream().filter(source -> source.tenantId().equals(tenantId)).toList();
        }
    }

    private static final class InMemoryTaskRepository implements SyncTaskRepository {
        private final Map<UUID, SyncTask> tasks = new LinkedHashMap<>();

        @Override
        public SyncTask save(SyncTask task) {
            tasks.put(task.id(), task);
            return task;
        }

        @Override
        public Optional<SyncTask> findById(UUID taskId) {
            return Optional.ofNullable(tasks.get(taskId));
        }

        @Override
        public boolean existsActiveTask(UUID tenantId, UUID sourceId) {
            return tasks.values().stream()
                    .filter(task -> task.tenantId().equals(tenantId) && task.sourceId().equals(sourceId))
                    .anyMatch(task -> task.status() == SyncTaskStatus.CREATED || task.status() == SyncTaskStatus.RUNNING);
        }

        @Override
        public List<SyncTask> findByTenantId(UUID tenantId) {
            return tasks.values().stream().filter(task -> task.tenantId().equals(tenantId)).toList();
        }

        @Override
        public List<SyncTask> findByTenantIdAndSourceId(UUID tenantId, UUID sourceId) {
            return tasks.values().stream()
                    .filter(task -> task.tenantId().equals(tenantId) && task.sourceId().equals(sourceId))
                    .toList();
        }
    }

    private static final class InMemoryDiffRepository implements DiffRecordRepository {
        private final Map<UUID, DiffRecord> diffs = new LinkedHashMap<>();

        @Override
        public DiffRecord save(DiffRecord diffRecord) {
            diffs.put(diffRecord.id(), diffRecord);
            return diffRecord;
        }

        @Override
        public Optional<DiffRecord> findById(UUID diffRecordId) {
            return Optional.ofNullable(diffs.get(diffRecordId));
        }

        @Override
        public List<DiffRecord> findByQuery(UUID tenantId, UUID taskId, DiffStatus status) {
            return diffs.values().stream()
                    .filter(diff -> diff.tenantId().equals(tenantId))
                    .filter(diff -> taskId == null || diff.taskId().equals(taskId))
                    .filter(diff -> status == null || diff.status() == status)
                    .toList();
        }
    }

    private static final class InMemoryCompensationRepository implements CompensationRecordRepository {
        private final Map<UUID, CompensationRecord> compensations = new LinkedHashMap<>();

        @Override
        public CompensationRecord save(CompensationRecord compensationRecord) {
            compensations.put(compensationRecord.id(), compensationRecord);
            return compensationRecord;
        }

        @Override
        public Optional<CompensationRecord> findById(UUID compensationRecordId) {
            return Optional.ofNullable(compensations.get(compensationRecordId));
        }

        @Override
        public List<CompensationRecord> findByTenantIdAndTaskId(UUID tenantId, UUID taskId) {
            return compensations.values().stream()
                    .filter(record -> record.tenantId().equals(tenantId) && record.taskId().equals(taskId))
                    .toList();
        }
    }

    private static final class InMemoryAuditRepository implements AuditRecordRepository {
        private final List<AuditRecord> records = new ArrayList<>();

        @Override
        public AuditRecord save(AuditRecord auditRecord) {
            records.add(auditRecord);
            return auditRecord;
        }

        @Override
        public List<AuditRecord> findByQuery(
                UUID tenantId,
                AuditCategory category,
                String entityType,
                String entityId,
                UUID taskId,
                Instant from,
                Instant to
        ) {
            return records.stream()
                    .filter(record -> record.tenantId().equals(tenantId))
                    .filter(record -> category == null || record.category() == category)
                    .filter(record -> entityType == null || record.entityType().equals(entityType))
                    .filter(record -> entityId == null || record.entityId().equals(entityId))
                    .filter(record -> taskId == null || taskId.equals(record.taskId()))
                    .filter(record -> from == null || !record.occurredAt().isBefore(from))
                    .filter(record -> to == null || !record.occurredAt().isAfter(to))
                    .sorted(Comparator.comparing(AuditRecord::occurredAt))
                    .toList();
        }
    }
}
