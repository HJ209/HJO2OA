package com.hjo2oa.infra.attachment.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hjo2oa.infra.attachment.application.AttachmentAccessAuditRecord;
import com.hjo2oa.infra.attachment.application.AttachmentAccessAuditRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisAttachmentAccessAuditRepository implements AttachmentAccessAuditRepository {

    private final AttachmentAccessAuditMapper mapper;

    public MybatisAttachmentAccessAuditRepository(AttachmentAccessAuditMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AttachmentAccessAuditRecord save(AttachmentAccessAuditRecord record) {
        mapper.insert(toEntity(record));
        return record;
    }

    @Override
    public List<AttachmentAccessAuditRecord> findByAttachmentId(UUID attachmentId) {
        return mapper.selectList(new LambdaQueryWrapper<AttachmentAccessAuditEntity>()
                        .eq(AttachmentAccessAuditEntity::getAttachmentAssetId, attachmentId.toString())
                        .orderByDesc(AttachmentAccessAuditEntity::getOccurredAt))
                .stream()
                .map(this::toRecord)
                .toList();
    }

    private AttachmentAccessAuditEntity toEntity(AttachmentAccessAuditRecord record) {
        AttachmentAccessAuditEntity entity = new AttachmentAccessAuditEntity();
        entity.setId(record.id().toString());
        entity.setAttachmentAssetId(record.attachmentId().toString());
        entity.setVersionNo(record.versionNo());
        entity.setAction(record.action());
        entity.setTenantId(toString(record.tenantId()));
        entity.setOperatorId(toString(record.operatorId()));
        entity.setClientIp(record.clientIp());
        entity.setOccurredAt(LocalDateTime.ofInstant(record.occurredAt(), ZoneOffset.UTC));
        return entity;
    }

    private AttachmentAccessAuditRecord toRecord(AttachmentAccessAuditEntity entity) {
        return new AttachmentAccessAuditRecord(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getAttachmentAssetId()),
                entity.getVersionNo(),
                entity.getAction(),
                toUuid(entity.getTenantId()),
                toUuid(entity.getOperatorId()),
                entity.getClientIp(),
                toInstant(entity.getOccurredAt())
        );
    }

    private String toString(UUID value) {
        return value == null ? null : value.toString();
    }

    private UUID toUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.toInstant(ZoneOffset.UTC);
    }
}
