package com.hjo2oa.infra.attachment.infrastructure;

import com.hjo2oa.infra.attachment.application.AttachmentAccessAuditRecord;
import com.hjo2oa.infra.attachment.application.AttachmentAccessAuditRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryAttachmentAccessAuditRepository implements AttachmentAccessAuditRepository {

    private final Map<UUID, AttachmentAccessAuditRecord> records = new ConcurrentHashMap<>();

    @Override
    public AttachmentAccessAuditRecord save(AttachmentAccessAuditRecord record) {
        records.put(record.id(), record);
        return record;
    }

    @Override
    public List<AttachmentAccessAuditRecord> findByAttachmentId(UUID attachmentId) {
        return records.values().stream()
                .filter(record -> record.attachmentId().equals(attachmentId))
                .sorted(Comparator.comparing(AttachmentAccessAuditRecord::occurredAt).reversed())
                .toList();
    }
}
