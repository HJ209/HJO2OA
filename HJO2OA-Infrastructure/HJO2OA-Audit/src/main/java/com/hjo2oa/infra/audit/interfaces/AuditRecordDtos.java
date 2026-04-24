package com.hjo2oa.infra.audit.interfaces;

import com.hjo2oa.infra.audit.application.AuditRecordCommands;
import com.hjo2oa.infra.audit.domain.ArchiveStatus;
import com.hjo2oa.infra.audit.domain.SensitivityLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AuditRecordDtos {

    private AuditRecordDtos() {
    }

    public record RecordRequest(
            @NotBlank @Size(max = 64) String moduleCode,
            @NotBlank @Size(max = 64) String objectType,
            @NotBlank @Size(max = 64) String objectId,
            @NotBlank @Size(max = 64) String actionType,
            UUID operatorAccountId,
            UUID operatorPersonId,
            UUID tenantId,
            @Size(max = 128) String traceId,
            @Size(max = 512) String summary,
            @Valid List<FieldChangeRequest> fieldChanges
    ) {

        public AuditRecordCommands.RecordAuditCommand toCommand() {
            return new AuditRecordCommands.RecordAuditCommand(
                    moduleCode,
                    objectType,
                    objectId,
                    actionType,
                    operatorAccountId,
                    operatorPersonId,
                    tenantId,
                    traceId,
                    summary,
                    fieldChanges == null ? List.of() : fieldChanges.stream().map(FieldChangeRequest::toCommand).toList()
            );
        }
    }

    public record FieldChangeRequest(
            @NotBlank @Size(max = 128) String fieldName,
            String oldValue,
            String newValue,
            SensitivityLevel sensitivityLevel
    ) {

        public AuditRecordCommands.FieldChangeCommand toCommand() {
            return new AuditRecordCommands.FieldChangeCommand(fieldName, oldValue, newValue, sensitivityLevel);
        }
    }

    public record SummaryResponse(
            UUID id,
            String moduleCode,
            String objectType,
            String objectId,
            String actionType,
            UUID operatorAccountId,
            UUID operatorPersonId,
            UUID tenantId,
            String traceId,
            String summary,
            Instant occurredAt,
            ArchiveStatus archiveStatus,
            Instant createdAt
    ) {
    }

    public record DetailResponse(
            UUID id,
            String moduleCode,
            String objectType,
            String objectId,
            String actionType,
            UUID operatorAccountId,
            UUID operatorPersonId,
            UUID tenantId,
            String traceId,
            String summary,
            Instant occurredAt,
            ArchiveStatus archiveStatus,
            Instant createdAt,
            List<FieldChangeResponse> fieldChanges
    ) {
    }

    public record FieldChangeResponse(
            UUID id,
            UUID auditRecordId,
            String fieldName,
            String oldValue,
            String newValue,
            SensitivityLevel sensitivityLevel
    ) {
    }
}
