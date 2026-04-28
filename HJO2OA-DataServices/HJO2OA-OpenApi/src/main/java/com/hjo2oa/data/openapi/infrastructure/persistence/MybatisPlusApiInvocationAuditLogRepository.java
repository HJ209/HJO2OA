package com.hjo2oa.data.openapi.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hjo2oa.data.openapi.domain.ApiInvocationAuditLog;
import com.hjo2oa.data.openapi.domain.ApiInvocationAuditLogRepository;
import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class MybatisPlusApiInvocationAuditLogRepository implements ApiInvocationAuditLogRepository {

    private final ApiInvocationAuditLogMapper mapper;

    public MybatisPlusApiInvocationAuditLogRepository(ApiInvocationAuditLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ApiInvocationAuditLog> findById(String logId) {
        return Optional.ofNullable(mapper.selectById(logId)).map(this::toDomain);
    }

    @Override
    public List<ApiInvocationAuditLog> findAllByTenant(String tenantId) {
        return mapper.selectList(new LambdaQueryWrapper<ApiInvocationAuditLogEntity>()
                        .eq(ApiInvocationAuditLogEntity::getTenantId, tenantId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public ApiInvocationAuditLog save(ApiInvocationAuditLog auditLog) {
        ApiInvocationAuditLogEntity entity = toEntity(auditLog);
        if (mapper.selectById(entity.getId()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return auditLog;
    }

    private ApiInvocationAuditLog toDomain(ApiInvocationAuditLogEntity entity) {
        return new ApiInvocationAuditLog(
                entity.getId(),
                entity.getRequestId(),
                entity.getTenantId(),
                entity.getApiId(),
                entity.getEndpointCode(),
                entity.getEndpointVersion(),
                entity.getPath(),
                entity.getHttpMethod(),
                entity.getClientCode(),
                entity.getAuthType(),
                entity.getOutcome(),
                entity.getResponseStatus(),
                entity.getErrorCode(),
                entity.getDurationMs(),
                entity.getRequestDigest(),
                entity.getRemoteIp(),
                entity.getOccurredAt(),
                entity.isAbnormalFlag(),
                entity.getReviewConclusion(),
                entity.getNote(),
                entity.getReviewedBy(),
                entity.getReviewedAt()
        );
    }

    private ApiInvocationAuditLogEntity toEntity(ApiInvocationAuditLog auditLog) {
        ApiInvocationAuditLogEntity entity = new ApiInvocationAuditLogEntity();
        entity.setId(auditLog.logId());
        entity.setRequestId(auditLog.requestId());
        entity.setTenantId(auditLog.tenantId());
        entity.setApiId(auditLog.apiId());
        entity.setEndpointCode(auditLog.endpointCode());
        entity.setEndpointVersion(auditLog.endpointVersion());
        entity.setPath(auditLog.path());
        entity.setHttpMethod(auditLog.httpMethod());
        entity.setClientCode(auditLog.clientCode());
        entity.setAuthType(auditLog.authType());
        entity.setOutcome(auditLog.outcome());
        entity.setResponseStatus(auditLog.responseStatus());
        entity.setErrorCode(auditLog.errorCode());
        entity.setDurationMs(auditLog.durationMs());
        entity.setRequestDigest(auditLog.requestDigest());
        entity.setRemoteIp(auditLog.remoteIp());
        entity.setOccurredAt(auditLog.occurredAt());
        entity.setAbnormalFlag(auditLog.abnormalFlag());
        entity.setReviewConclusion(auditLog.reviewConclusion());
        entity.setNote(auditLog.note());
        entity.setReviewedBy(auditLog.reviewedBy());
        entity.setReviewedAt(auditLog.reviewedAt());
        return entity;
    }
}
