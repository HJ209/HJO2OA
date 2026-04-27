package com.hjo2oa.org.org.sync.audit.application;

import com.hjo2oa.org.org.sync.audit.domain.AuditCategory;
import com.hjo2oa.org.org.sync.audit.domain.AuditRecord;
import com.hjo2oa.org.org.sync.audit.domain.AuditRecordRepository;
import com.hjo2oa.org.org.sync.audit.domain.AuditRecordView;
import com.hjo2oa.org.org.sync.audit.domain.CompensationRecord;
import com.hjo2oa.org.org.sync.audit.domain.CompensationRecordRepository;
import com.hjo2oa.org.org.sync.audit.domain.CompensationRecordView;
import com.hjo2oa.org.org.sync.audit.domain.DiffRecord;
import com.hjo2oa.org.org.sync.audit.domain.DiffRecordRepository;
import com.hjo2oa.org.org.sync.audit.domain.DiffRecordView;
import com.hjo2oa.org.org.sync.audit.domain.SourceStatus;
import com.hjo2oa.org.org.sync.audit.domain.SyncSourceConfig;
import com.hjo2oa.org.org.sync.audit.domain.SyncSourceConfigRepository;
import com.hjo2oa.org.org.sync.audit.domain.SyncSourceConfigView;
import com.hjo2oa.org.org.sync.audit.domain.SyncTask;
import com.hjo2oa.org.org.sync.audit.domain.SyncTaskRepository;
import com.hjo2oa.org.org.sync.audit.domain.SyncTaskType;
import com.hjo2oa.org.org.sync.audit.domain.SyncTaskView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgSyncAuditApplicationService {

    private static final String TRIGGER_MANUAL = "MANUAL";

    private final SyncSourceConfigRepository sourceRepository;
    private final SyncTaskRepository taskRepository;
    private final DiffRecordRepository diffRecordRepository;
    private final CompensationRecordRepository compensationRecordRepository;
    private final AuditRecordRepository auditRecordRepository;
    private final TargetMasterDataGateway targetMasterDataGateway;
    private final Clock clock;

    public OrgSyncAuditApplicationService(
            SyncSourceConfigRepository sourceRepository,
            SyncTaskRepository taskRepository,
            DiffRecordRepository diffRecordRepository,
            CompensationRecordRepository compensationRecordRepository,
            AuditRecordRepository auditRecordRepository,
            TargetMasterDataGateway targetMasterDataGateway
    ) {
        this(
                sourceRepository,
                taskRepository,
                diffRecordRepository,
                compensationRecordRepository,
                auditRecordRepository,
                targetMasterDataGateway,
                Clock.systemUTC()
        );
    }

    public OrgSyncAuditApplicationService(
            SyncSourceConfigRepository sourceRepository,
            SyncTaskRepository taskRepository,
            DiffRecordRepository diffRecordRepository,
            CompensationRecordRepository compensationRecordRepository,
            AuditRecordRepository auditRecordRepository,
            TargetMasterDataGateway targetMasterDataGateway,
            Clock clock
    ) {
        this.sourceRepository = Objects.requireNonNull(sourceRepository, "sourceRepository must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.diffRecordRepository = Objects.requireNonNull(diffRecordRepository, "diffRecordRepository must not be null");
        this.compensationRecordRepository = Objects.requireNonNull(
                compensationRecordRepository,
                "compensationRecordRepository must not be null"
        );
        this.auditRecordRepository = Objects.requireNonNull(
                auditRecordRepository,
                "auditRecordRepository must not be null"
        );
        this.targetMasterDataGateway = Objects.requireNonNull(
                targetMasterDataGateway,
                "targetMasterDataGateway must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public SyncSourceConfigView createSource(OrgSyncAuditCommands.CreateSourceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        sourceRepository.findByTenantIdAndSourceCode(command.tenantId(), command.sourceCode())
                .ifPresent(existing -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Sync source code already exists");
                });
        Instant now = now();
        SyncSourceConfig saved = sourceRepository.save(SyncSourceConfig.create(
                UUID.randomUUID(),
                command.tenantId(),
                command.sourceCode(),
                command.sourceName(),
                command.sourceType(),
                command.endpoint(),
                command.configRef(),
                command.scopeExpression(),
                now
        ));
        recordAudit(saved.tenantId(), AuditCategory.SYNC_SOURCE, "CREATE", "SYNC_SOURCE", saved.id().toString(),
                null, TRIGGER_MANUAL, command.operatorId(), null, saved.toString(), "Sync source created");
        return saved.toView();
    }

    @Transactional
    public SyncSourceConfigView updateSource(OrgSyncAuditCommands.UpdateSourceCommand command) {
        SyncSourceConfig source = loadSource(command.sourceId());
        SyncSourceConfig updated = source.update(
                command.sourceName(),
                command.sourceType(),
                command.endpoint(),
                command.configRef(),
                command.scopeExpression(),
                now()
        );
        SyncSourceConfig saved = sourceRepository.save(updated);
        recordAudit(saved.tenantId(), AuditCategory.SYNC_SOURCE, "UPDATE", "SYNC_SOURCE", saved.id().toString(),
                null, TRIGGER_MANUAL, command.operatorId(), source.toString(), saved.toString(), "Sync source updated");
        return saved.toView();
    }

    @Transactional
    public SyncSourceConfigView enableSource(UUID sourceId, UUID operatorId) {
        SyncSourceConfig source = loadSource(sourceId);
        SyncSourceConfig saved = sourceRepository.save(source.enable(now()));
        recordAudit(saved.tenantId(), AuditCategory.SYNC_SOURCE, "ENABLE", "SYNC_SOURCE", saved.id().toString(),
                null, TRIGGER_MANUAL, operatorId, source.toString(), saved.toString(), "Sync source enabled");
        return saved.toView();
    }

    @Transactional
    public SyncSourceConfigView disableSource(UUID sourceId, UUID operatorId) {
        SyncSourceConfig source = loadSource(sourceId);
        SyncSourceConfig saved = sourceRepository.save(source.disable(now()));
        recordAudit(saved.tenantId(), AuditCategory.SYNC_SOURCE, "DISABLE", "SYNC_SOURCE", saved.id().toString(),
                null, TRIGGER_MANUAL, operatorId, source.toString(), saved.toString(), "Sync source disabled");
        return saved.toView();
    }

    public List<SyncSourceConfigView> listSources(UUID tenantId) {
        return sourceRepository.findByTenantId(tenantId).stream().map(SyncSourceConfig::toView).toList();
    }

    @Transactional
    public SyncTaskView startFullTask(OrgSyncAuditCommands.StartTaskCommand command) {
        return startTask(command, SyncTaskType.FULL, null).toView();
    }

    @Transactional
    public SyncTaskView startIncrementalTask(OrgSyncAuditCommands.StartTaskCommand command) {
        return startTask(command, SyncTaskType.INCREMENTAL, null).toView();
    }

    @Transactional
    public SyncTaskView retryTask(UUID failedTaskId, UUID operatorId) {
        SyncTask failed = loadTask(failedTaskId);
        if (failed.status() != com.hjo2oa.org.org.sync.audit.domain.SyncTaskStatus.FAILED) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Only failed task can be retried");
        }
        OrgSyncAuditCommands.StartTaskCommand command = new OrgSyncAuditCommands.StartTaskCommand(
                failed.tenantId(),
                failed.sourceId(),
                TRIGGER_MANUAL,
                operatorId
        );
        return startTask(command, SyncTaskType.RETRY, failed.id()).toView();
    }

    public List<SyncTaskView> listTasks(UUID tenantId, UUID sourceId) {
        List<SyncTask> tasks = sourceId == null
                ? taskRepository.findByTenantId(tenantId)
                : taskRepository.findByTenantIdAndSourceId(tenantId, sourceId);
        return tasks.stream().map(SyncTask::toView).toList();
    }

    @Transactional
    public DiffRecordView createDiff(OrgSyncAuditCommands.CreateDiffCommand command) {
        SyncTask task = loadTask(command.taskId());
        if (!task.tenantId().equals(command.tenantId())) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "Diff tenant does not match task");
        }
        DiffRecord saved = diffRecordRepository.save(DiffRecord.create(
                UUID.randomUUID(),
                command.tenantId(),
                command.taskId(),
                command.entityType(),
                command.entityKey(),
                command.diffType(),
                command.sourceSnapshot(),
                command.localSnapshot(),
                command.suggestion(),
                now()
        ));
        recordAudit(saved.tenantId(), AuditCategory.DIFF_GOVERNANCE, "CREATE", saved.entityType(), saved.entityKey(),
                saved.taskId(), "SYNC_TASK", null, saved.localSnapshot(), saved.sourceSnapshot(), "Diff created");
        return saved.toView();
    }

    @Transactional
    public CompensationRecordView resolveDiff(OrgSyncAuditCommands.ResolveDiffCommand command) {
        DiffRecord diffRecord = loadDiffRecord(command.diffRecordId());
        TargetMasterDataResult result = targetMasterDataGateway.applyResolution(
                diffRecord,
                command.actionType(),
                command.requestPayload()
        );
        Instant now = now();
        CompensationRecord requested = CompensationRecord.request(
                UUID.randomUUID(),
                diffRecord.tenantId(),
                diffRecord.taskId(),
                diffRecord.id(),
                command.actionType(),
                command.requestPayload(),
                command.operatorId(),
                now
        );
        CompensationRecord saved = compensationRecordRepository.save(result.applied()
                ? requested.applied(result.resultPayload(), now)
                : requested.failed(result.resultPayload(), now));
        diffRecordRepository.save(result.applied()
                ? diffRecord.resolve(command.operatorId(), now)
                : diffRecord.confirm(command.operatorId(), now));
        recordAudit(diffRecord.tenantId(), AuditCategory.COMPENSATION, command.actionType(), diffRecord.entityType(),
                diffRecord.entityKey(), diffRecord.taskId(), TRIGGER_MANUAL, command.operatorId(),
                diffRecord.localSnapshot(), diffRecord.sourceSnapshot(), result.resultPayload());
        return saved.toView();
    }

    public List<DiffRecordView> queryDiffs(OrgSyncAuditCommands.DiffQuery query) {
        return diffRecordRepository.findByQuery(query.tenantId(), query.taskId(), query.status())
                .stream()
                .map(DiffRecord::toView)
                .toList();
    }

    public List<AuditRecordView> queryAudits(OrgSyncAuditCommands.AuditQuery query) {
        return auditRecordRepository.findByQuery(
                query.tenantId(),
                query.category(),
                query.entityType(),
                query.entityId(),
                query.taskId(),
                query.from(),
                query.to()
        ).stream().map(AuditRecord::toView).toList();
    }

    private SyncTask startTask(
            OrgSyncAuditCommands.StartTaskCommand command,
            SyncTaskType taskType,
            UUID retryOfTaskId
    ) {
        SyncSourceConfig source = loadSource(command.sourceId());
        if (!source.tenantId().equals(command.tenantId())) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "Task tenant does not match source");
        }
        if (source.status() != SourceStatus.ENABLED) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Sync source is disabled");
        }
        if (taskRepository.existsActiveTask(command.tenantId(), command.sourceId())) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Source has active sync task");
        }
        Instant now = now();
        SyncTask task = SyncTask.create(
                UUID.randomUUID(),
                command.tenantId(),
                command.sourceId(),
                taskType,
                retryOfTaskId,
                command.triggerSource() == null ? TRIGGER_MANUAL : command.triggerSource(),
                command.operatorId(),
                now
        ).start(now).complete(0, 0, 0, now);
        SyncTask saved = taskRepository.save(task);
        recordAudit(saved.tenantId(), AuditCategory.SYNC_TASK, saved.taskType().name(), "SYNC_TASK",
                saved.id().toString(), saved.id(), saved.triggerSource(), saved.operatorId(), null,
                saved.toString(), "Sync task completed with no adapter payload");
        return saved;
    }

    private SyncSourceConfig loadSource(UUID sourceId) {
        return sourceRepository.findById(sourceId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Sync source not found"));
    }

    private SyncTask loadTask(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Sync task not found"));
    }

    private DiffRecord loadDiffRecord(UUID diffRecordId) {
        return diffRecordRepository.findById(diffRecordId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Diff not found"));
    }

    private void recordAudit(
            UUID tenantId,
            AuditCategory category,
            String actionType,
            String entityType,
            String entityId,
            UUID taskId,
            String triggerSource,
            UUID operatorId,
            String beforeSnapshot,
            String afterSnapshot,
            String summary
    ) {
        auditRecordRepository.save(AuditRecord.create(
                UUID.randomUUID(),
                tenantId,
                category,
                actionType,
                entityType,
                entityId,
                taskId,
                triggerSource,
                operatorId,
                beforeSnapshot,
                afterSnapshot,
                summary,
                now()
        ));
    }

    private Instant now() {
        return clock.instant();
    }
}
