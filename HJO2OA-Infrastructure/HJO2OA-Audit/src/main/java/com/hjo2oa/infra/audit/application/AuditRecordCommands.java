package com.hjo2oa.infra.audit.application;

import com.hjo2oa.infra.audit.domain.SensitivityLevel;
import java.util.List;
import java.util.UUID;

public final class AuditRecordCommands {

    private AuditRecordCommands() {
    }

    public record RecordAuditCommand(
            String moduleCode,
            String objectType,
            String objectId,
            String actionType,
            UUID operatorAccountId,
            UUID operatorPersonId,
            UUID tenantId,
            String traceId,
            String summary,
            List<FieldChangeCommand> fieldChanges
    ) {

        public RecordAuditCommand {
            fieldChanges = fieldChanges == null ? List.of() : List.copyOf(fieldChanges);
        }
    }

    public record FieldChangeCommand(
            String fieldName,
            String oldValue,
            String newValue,
            SensitivityLevel sensitivityLevel
    ) {
    }
}
