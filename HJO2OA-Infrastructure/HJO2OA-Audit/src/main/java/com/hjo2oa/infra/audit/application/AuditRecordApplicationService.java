package com.hjo2oa.infra.audit.application;

import com.hjo2oa.infra.audit.domain.AuditFieldChange;
import com.hjo2oa.infra.audit.domain.AuditQuery;
import com.hjo2oa.infra.audit.domain.AuditRecord;
import com.hjo2oa.infra.audit.domain.AuditRecordRepository;
import com.hjo2oa.infra.audit.domain.AuditRecordView;
import com.hjo2oa.shared.kernel.BizException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditRecordApplicationService {

    private final AuditRecordRepository repository;
    private final Clock clock;

    public AuditRecordApplicationService(AuditRecordRepository repository) {
        this(repository, Clock.systemUTC());
    }

    @Autowired
    public AuditRecordApplicationService(AuditRecordRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public AuditRecordView recordAudit(AuditRecordCommands.RecordAuditCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Instant now = clock.instant();
        UUID recordId = UUID.randomUUID();
        List<AuditFieldChange> fieldChanges = command.fieldChanges().stream()
                .map(fieldChange -> AuditFieldChange.create(
                        recordId,
                        fieldChange.fieldName(),
                        fieldChange.oldValue(),
                        fieldChange.newValue(),
                        fieldChange.sensitivityLevel()
                ))
                .toList();
        AuditRecord record = AuditRecord.create(
                recordId,
                command.moduleCode(),
                command.objectType(),
                command.objectId(),
                command.actionType(),
                command.operatorAccountId(),
                command.operatorPersonId(),
                command.tenantId(),
                command.traceId(),
                command.summary(),
                now,
                now,
                fieldChanges
        );
        return repository.save(record).toView();
    }

    public AuditRecordView archiveRecord(UUID recordId) {
        Objects.requireNonNull(recordId, "recordId must not be null");
        AuditRecord record = loadRequiredRecord(recordId);
        AuditRecord archived = record.archive();
        return (archived == record ? record : repository.save(archived)).toView();
    }

    public List<AuditRecordView> queryAudits(AuditQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return repository.findByQuery(query).stream().map(AuditRecord::toView).toList();
    }

    public AuditRecordView getRecord(UUID recordId) {
        Objects.requireNonNull(recordId, "recordId must not be null");
        return loadRequiredRecord(recordId).toView();
    }

    private AuditRecord loadRequiredRecord(UUID recordId) {
        return repository.findById(recordId)
                .orElseThrow(() -> new BizException(
                        AuditErrorDescriptors.AUDIT_RECORD_NOT_FOUND,
                        "审计记录不存在"
                ));
    }
}
