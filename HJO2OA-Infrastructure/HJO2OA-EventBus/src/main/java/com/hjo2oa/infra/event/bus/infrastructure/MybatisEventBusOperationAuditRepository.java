package com.hjo2oa.infra.event.bus.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.infra.event.bus.application.EventBusOperationAudit;
import com.hjo2oa.infra.event.bus.application.EventBusOperationAuditRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisEventBusOperationAuditRepository implements EventBusOperationAuditRepository {

    private final EventOperationAuditMapper mapper;

    public MybatisEventBusOperationAuditRepository(EventOperationAuditMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public EventBusOperationAudit save(EventBusOperationAudit audit) {
        Objects.requireNonNull(audit, "audit must not be null");
        try {
            mapper.insert(toEntity(audit));
            return audit;
        } catch (DuplicateKeyException ex) {
            return findByIdempotencyKey(audit.idempotencyKey()).orElseThrow(() -> ex);
        }
    }

    @Override
    public Optional<EventBusOperationAudit> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        EventOperationAuditEntity entity = mapper.selectOne(Wrappers.<EventOperationAuditEntity>lambdaQuery()
                .eq(EventOperationAuditEntity::getIdempotencyKey, idempotencyKey));
        return Optional.ofNullable(entity).map(this::toAudit);
    }

    private EventOperationAuditEntity toEntity(EventBusOperationAudit audit) {
        return new EventOperationAuditEntity()
                .setId(audit.id())
                .setEventId(audit.eventId())
                .setOperationType(audit.operationType())
                .setOperatorAccountId(audit.operatorAccountId())
                .setOperatorPersonId(audit.operatorPersonId())
                .setTenantId(audit.tenantId())
                .setTraceId(audit.traceId())
                .setRequestId(audit.requestId())
                .setIdempotencyKey(audit.idempotencyKey())
                .setReason(audit.reason())
                .setDetailJson(audit.detailJson())
                .setCreatedAt(audit.createdAt());
    }

    private EventBusOperationAudit toAudit(EventOperationAuditEntity entity) {
        return new EventBusOperationAudit(
                entity.getId(),
                entity.getEventId(),
                entity.getOperationType(),
                entity.getOperatorAccountId(),
                entity.getOperatorPersonId(),
                entity.getTenantId(),
                entity.getTraceId(),
                entity.getRequestId(),
                entity.getIdempotencyKey(),
                entity.getReason(),
                entity.getDetailJson(),
                entity.getCreatedAt()
        );
    }
}
