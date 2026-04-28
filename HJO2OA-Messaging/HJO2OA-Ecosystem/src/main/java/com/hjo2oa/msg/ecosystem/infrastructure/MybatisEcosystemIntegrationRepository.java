package com.hjo2oa.msg.ecosystem.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.msg.ecosystem.domain.AuthMode;
import com.hjo2oa.msg.ecosystem.domain.CallbackAuditRecord;
import com.hjo2oa.msg.ecosystem.domain.CallbackAuditRecordRepository;
import com.hjo2oa.msg.ecosystem.domain.EcosystemIntegration;
import com.hjo2oa.msg.ecosystem.domain.EcosystemIntegrationRepository;
import com.hjo2oa.msg.ecosystem.domain.HealthStatus;
import com.hjo2oa.msg.ecosystem.domain.IntegrationStatus;
import com.hjo2oa.msg.ecosystem.domain.IntegrationType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisEcosystemIntegrationRepository implements EcosystemIntegrationRepository {

    private final EcosystemIntegrationMapper integrationMapper;
    private final CallbackAuditRecordRepository callbackAuditRecordRepository;

    public MybatisEcosystemIntegrationRepository(
            EcosystemIntegrationMapper integrationMapper,
            CallbackAuditRecordRepository callbackAuditRecordRepository
    ) {
        this.integrationMapper = Objects.requireNonNull(integrationMapper);
        this.callbackAuditRecordRepository = Objects.requireNonNull(callbackAuditRecordRepository);
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
        return callbackAuditRecordRepository.saveCallbackAudit(record);
    }

    @Override
    public Optional<CallbackAuditRecord> findCallbackAudit(UUID integrationId, String idempotencyKey) {
        return callbackAuditRecordRepository.findCallbackAudit(integrationId, idempotencyKey);
    }

    @Override
    public List<CallbackAuditRecord> findCallbackAudits(UUID integrationId) {
        return callbackAuditRecordRepository.findCallbackAudits(integrationId);
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

}
