package com.hjo2oa.msg.ecosystem.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.msg.ecosystem.domain.AuthMode;
import com.hjo2oa.msg.ecosystem.domain.CallbackAuditRecord;
import com.hjo2oa.msg.ecosystem.domain.EcosystemIntegration;
import com.hjo2oa.msg.ecosystem.domain.EcosystemIntegrationRepository;
import com.hjo2oa.msg.ecosystem.domain.HealthStatus;
import com.hjo2oa.msg.ecosystem.domain.IntegrationStatus;
import com.hjo2oa.msg.ecosystem.domain.IntegrationType;
import com.hjo2oa.msg.ecosystem.domain.VerifyResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisEcosystemIntegrationRepository implements EcosystemIntegrationRepository {

    private final EcosystemIntegrationMapper integrationMapper;
    private final CallbackAuditRecordMapper auditMapper;

    public MybatisEcosystemIntegrationRepository(
            EcosystemIntegrationMapper integrationMapper,
            CallbackAuditRecordMapper auditMapper
    ) {
        this.integrationMapper = Objects.requireNonNull(integrationMapper);
        this.auditMapper = Objects.requireNonNull(auditMapper);
    }

    @Override
    public EcosystemIntegration saveIntegration(EcosystemIntegration integration) {
        EcosystemIntegrationEntity entity = toIntegrationEntity(integration);
        if (integrationMapper.selectById(integration.id()) == null) {
            integrationMapper.insert(entity);
        } else {
            integrationMapper.updateById(entity);
        }
        return toIntegration(integrationMapper.selectById(integration.id()));
    }

    @Override
    public Optional<EcosystemIntegration> findIntegrationById(UUID integrationId) {
        return Optional.ofNullable(integrationMapper.selectById(integrationId)).map(this::toIntegration);
    }

    @Override
    public List<EcosystemIntegration> findIntegrations(UUID tenantId, IntegrationType integrationType) {
        LambdaQueryWrapper<EcosystemIntegrationEntity> wrapper = Wrappers.<EcosystemIntegrationEntity>lambdaQuery()
                .eq(EcosystemIntegrationEntity::getTenantId, tenantId)
                .orderByAsc(EcosystemIntegrationEntity::getIntegrationType)
                .orderByAsc(EcosystemIntegrationEntity::getDisplayName);
        if (integrationType != null) {
            wrapper.eq(EcosystemIntegrationEntity::getIntegrationType, integrationType.name());
        }
        return integrationMapper.selectList(wrapper).stream().map(this::toIntegration).toList();
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

    private EcosystemIntegrationEntity toIntegrationEntity(EcosystemIntegration integration) {
        EcosystemIntegrationEntity entity = new EcosystemIntegrationEntity();
        entity.setId(integration.id());
        entity.setIntegrationType(integration.integrationType().name());
        entity.setDisplayName(integration.displayName());
        entity.setAuthMode(integration.authMode() == null ? null : integration.authMode().name());
        entity.setCallbackUrl(integration.callbackUrl());
        entity.setSignAlgorithm(integration.signAlgorithm());
        entity.setConfigRef(integration.configRef());
        entity.setStatus(integration.status().name());
        entity.setHealthStatus(integration.healthStatus().name());
        entity.setLastCheckAt(integration.lastCheckAt());
        entity.setLastErrorSummary(integration.lastErrorSummary());
        entity.setTenantId(integration.tenantId());
        entity.setCreatedAt(integration.createdAt());
        entity.setUpdatedAt(integration.updatedAt());
        return entity;
    }

    private EcosystemIntegration toIntegration(EcosystemIntegrationEntity entity) {
        return new EcosystemIntegration(
                entity.getId(),
                IntegrationType.valueOf(entity.getIntegrationType()),
                entity.getDisplayName(),
                entity.getAuthMode() == null ? null : AuthMode.valueOf(entity.getAuthMode()),
                entity.getCallbackUrl(),
                entity.getSignAlgorithm(),
                entity.getConfigRef(),
                IntegrationStatus.valueOf(entity.getStatus()),
                HealthStatus.valueOf(entity.getHealthStatus()),
                entity.getLastCheckAt(),
                entity.getLastErrorSummary(),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
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
