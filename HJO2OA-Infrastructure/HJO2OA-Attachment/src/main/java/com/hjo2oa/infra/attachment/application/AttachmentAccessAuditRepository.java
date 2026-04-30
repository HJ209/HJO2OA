package com.hjo2oa.infra.attachment.application;

import java.util.List;
import java.util.UUID;

public interface AttachmentAccessAuditRepository {

    AttachmentAccessAuditRecord save(AttachmentAccessAuditRecord record);

    List<AttachmentAccessAuditRecord> findByAttachmentId(UUID attachmentId);
}
