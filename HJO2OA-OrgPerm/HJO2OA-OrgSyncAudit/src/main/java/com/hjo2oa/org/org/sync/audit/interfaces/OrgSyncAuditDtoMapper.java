package com.hjo2oa.org.org.sync.audit.interfaces;

import com.hjo2oa.org.org.sync.audit.domain.AuditRecordView;
import com.hjo2oa.org.org.sync.audit.domain.CompensationRecordView;
import com.hjo2oa.org.org.sync.audit.domain.DiffRecordView;
import com.hjo2oa.org.org.sync.audit.domain.SyncSourceConfigView;
import com.hjo2oa.org.org.sync.audit.domain.SyncTaskView;
import org.springframework.stereotype.Component;

@Component
public class OrgSyncAuditDtoMapper {

    public OrgSyncAuditDtos.SourceResponse toResponse(SyncSourceConfigView view) {
        return new OrgSyncAuditDtos.SourceResponse(
                view.id(),
                view.tenantId(),
                view.sourceCode(),
                view.sourceName(),
                view.sourceType(),
                view.endpoint(),
                view.configRef(),
                view.scopeExpression(),
                view.status(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public OrgSyncAuditDtos.TaskResponse toResponse(SyncTaskView view) {
        return new OrgSyncAuditDtos.TaskResponse(
                view.id(),
                view.tenantId(),
                view.sourceId(),
                view.taskType(),
                view.status(),
                view.retryOfTaskId(),
                view.triggerSource(),
                view.operatorId(),
                view.startedAt(),
                view.finishedAt(),
                view.successCount(),
                view.failureCount(),
                view.diffCount(),
                view.failureReason(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public OrgSyncAuditDtos.DiffResponse toResponse(DiffRecordView view) {
        return new OrgSyncAuditDtos.DiffResponse(
                view.id(),
                view.tenantId(),
                view.taskId(),
                view.entityType(),
                view.entityKey(),
                view.diffType(),
                view.status(),
                view.sourceSnapshot(),
                view.localSnapshot(),
                view.suggestion(),
                view.resolvedBy(),
                view.resolvedAt(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public OrgSyncAuditDtos.CompensationResponse toResponse(CompensationRecordView view) {
        return new OrgSyncAuditDtos.CompensationResponse(
                view.id(),
                view.tenantId(),
                view.taskId(),
                view.diffRecordId(),
                view.actionType(),
                view.status(),
                view.requestPayload(),
                view.resultPayload(),
                view.operatorId(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public OrgSyncAuditDtos.AuditResponse toResponse(AuditRecordView view) {
        return new OrgSyncAuditDtos.AuditResponse(
                view.id(),
                view.tenantId(),
                view.category(),
                view.actionType(),
                view.entityType(),
                view.entityId(),
                view.taskId(),
                view.triggerSource(),
                view.operatorId(),
                view.beforeSnapshot(),
                view.afterSnapshot(),
                view.summary(),
                view.occurredAt(),
                view.createdAt()
        );
    }
}
