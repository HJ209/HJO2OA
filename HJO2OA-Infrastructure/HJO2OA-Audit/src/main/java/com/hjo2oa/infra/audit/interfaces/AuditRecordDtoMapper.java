package com.hjo2oa.infra.audit.interfaces;

import com.hjo2oa.infra.audit.domain.AuditFieldChangeView;
import com.hjo2oa.infra.audit.domain.AuditRecordView;
import org.springframework.stereotype.Component;

@Component
public class AuditRecordDtoMapper {

    public AuditRecordDtos.SummaryResponse toSummaryResponse(AuditRecordView view) {
        return new AuditRecordDtos.SummaryResponse(
                view.id(),
                view.moduleCode(),
                view.objectType(),
                view.objectId(),
                view.actionType(),
                view.operatorAccountId(),
                view.operatorPersonId(),
                view.tenantId(),
                view.traceId(),
                view.summary(),
                view.occurredAt(),
                view.archiveStatus(),
                view.createdAt()
        );
    }

    public AuditRecordDtos.DetailResponse toDetailResponse(AuditRecordView view) {
        return new AuditRecordDtos.DetailResponse(
                view.id(),
                view.moduleCode(),
                view.objectType(),
                view.objectId(),
                view.actionType(),
                view.operatorAccountId(),
                view.operatorPersonId(),
                view.tenantId(),
                view.traceId(),
                view.summary(),
                view.occurredAt(),
                view.archiveStatus(),
                view.createdAt(),
                view.fieldChanges().stream().map(this::toFieldChangeResponse).toList()
        );
    }

    public AuditRecordDtos.FieldChangeResponse toFieldChangeResponse(AuditFieldChangeView view) {
        return new AuditRecordDtos.FieldChangeResponse(
                view.id(),
                view.auditRecordId(),
                view.fieldName(),
                view.oldValue(),
                view.newValue(),
                view.sensitivityLevel()
        );
    }
}
