package com.hjo2oa.msg.ecosystem.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.msg.ecosystem.domain.CallbackAuditRecord;
import com.hjo2oa.msg.ecosystem.domain.CallbackAuditRecordRepository;
import com.hjo2oa.msg.ecosystem.domain.VerifyResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisCallbackAuditRecordRepository implements CallbackAuditRecordRepository {

    private final CallbackAuditRecordMapper auditMapper;

    public MybatisCallbackAuditRecordRepository(CallbackAuditRecordMapper auditMapper) {
        this.auditMapper = Objects.requireNonNull(auditMapper);
    }

    @Override
    public CallbackAuditRecord saveCallbackAudit(CallbackAuditRecord record) {
        CallbackAuditRecordEntity entity = toAuditEntity(record);
        if (auditMapper.selectById(record.id()) == null) {
            auditMapper.insert(entity);
        } else {
            auditMapper.updateById(entity);
        }
        return toAudit(auditMapper.selectById(record.id()));
    }

    @Override
    public Optional<CallbackAuditRecord> findCallbackAudit(UUID integrationId, String idempotencyKey) {
        LambdaQueryWrapper<CallbackAuditRecordEntity> wrapper = Wrappers.<CallbackAuditRecordEntity>lambdaQuery()
                .eq(CallbackAuditRecordEntity::getIntegrationId, integrationId)
                .eq(CallbackAuditRecordEntity::getIdempotencyKey, idempotencyKey);
        return Optional.ofNullable(auditMapper.selectOne(wrapper)).map(this::toAudit);
    }

    @Override
    public List<CallbackAuditRecord> findCallbackAudits(UUID integrationId) {
        return auditMapper.selectList(Wrappers.<CallbackAuditRecordEntity>lambdaQuery()
                        .eq(CallbackAuditRecordEntity::getIntegrationId, integrationId)
                        .orderByDesc(CallbackAuditRecordEntity::getOccurredAt))
                .stream()
                .map(this::toAudit)
                .toList();
    }

    private CallbackAuditRecordEntity toAuditEntity(CallbackAuditRecord record) {
        CallbackAuditRecordEntity entity = new CallbackAuditRecordEntity();
        entity.setId(record.id());
        entity.setIntegrationId(record.integrationId());
        entity.setCallbackType(record.callbackType());
        entity.setVerifyResult(record.verifyResult().name());
        entity.setPayloadSummary(record.payloadSummary());
        entity.setErrorMessage(record.errorMessage());
        entity.setIdempotencyKey(record.idempotencyKey());
        entity.setPayloadDigest(record.payloadDigest());
        entity.setOccurredAt(record.occurredAt());
        return entity;
    }

    private CallbackAuditRecord toAudit(CallbackAuditRecordEntity entity) {
        return new CallbackAuditRecord(
                entity.getId(),
                entity.getIntegrationId(),
                entity.getCallbackType(),
                VerifyResult.valueOf(entity.getVerifyResult()),
                entity.getPayloadSummary(),
                entity.getErrorMessage(),
                entity.getIdempotencyKey(),
                entity.getPayloadDigest(),
                entity.getOccurredAt()
        );
    }
}
