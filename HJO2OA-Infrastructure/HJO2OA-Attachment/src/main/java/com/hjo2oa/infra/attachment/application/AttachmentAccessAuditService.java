package com.hjo2oa.infra.attachment.application;

import com.hjo2oa.shared.tenant.TenantContextHolder;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AttachmentAccessAuditService {

    private final AttachmentAccessAuditRepository auditRepository;
    private final Clock clock;

    @Autowired
    public AttachmentAccessAuditService(AttachmentAccessAuditRepository auditRepository) {
        this(auditRepository, Clock.systemUTC());
    }

    AttachmentAccessAuditService(AttachmentAccessAuditRepository auditRepository, Clock clock) {
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public void record(
            UUID attachmentId,
            Integer versionNo,
            String action,
            UUID operatorId,
            String clientIp
    ) {
        UUID tenantId = TenantContextHolder.currentTenantId().orElse(null);
        auditRepository.save(new AttachmentAccessAuditRecord(
                UUID.randomUUID(),
                attachmentId,
                versionNo,
                action,
                tenantId,
                operatorId,
                clientIp,
                clock.instant()
        ));
    }

    public List<AttachmentAccessAuditRecord> list(UUID attachmentId) {
        return auditRepository.findByAttachmentId(attachmentId);
    }
}
