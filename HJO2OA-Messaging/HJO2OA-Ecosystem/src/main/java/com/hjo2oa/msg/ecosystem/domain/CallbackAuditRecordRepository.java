package com.hjo2oa.msg.ecosystem.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CallbackAuditRecordRepository {

    CallbackAuditRecord saveCallbackAudit(CallbackAuditRecord record);

    Optional<CallbackAuditRecord> findCallbackAudit(UUID integrationId, String idempotencyKey);

    List<CallbackAuditRecord> findCallbackAudits(UUID integrationId);
}
