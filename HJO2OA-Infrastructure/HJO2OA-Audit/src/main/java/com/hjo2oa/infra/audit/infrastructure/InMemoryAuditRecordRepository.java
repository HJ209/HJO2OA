package com.hjo2oa.infra.audit.infrastructure;

import com.hjo2oa.infra.audit.domain.AuditQuery;
import com.hjo2oa.infra.audit.domain.AuditRecord;
import com.hjo2oa.infra.audit.domain.AuditRecordRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryAuditRecordRepository implements AuditRecordRepository {

    private final Map<UUID, AuditRecord> recordsById = new ConcurrentHashMap<>();

    @Override
    public AuditRecord save(AuditRecord record) {
        recordsById.put(record.id(), record);
        return record;
    }

    @Override
    public Optional<AuditRecord> findById(UUID id) {
        return Optional.ofNullable(recordsById.get(id));
    }

    @Override
    public List<AuditRecord> findByQuery(AuditQuery query) {
        return recordsById.values().stream()
                .filter(record -> query.tenantId() == null || query.tenantId().equals(record.tenantId()))
                .filter(record -> query.moduleCode() == null || query.moduleCode().equals(record.moduleCode()))
                .filter(record -> query.objectType() == null || query.objectType().equals(record.objectType()))
                .filter(record -> query.objectId() == null || query.objectId().equals(record.objectId()))
                .filter(record -> query.actionType() == null || query.actionType().equals(record.actionType()))
                .filter(record -> query.operatorAccountId() == null
                        || query.operatorAccountId().equals(record.operatorAccountId()))
                .filter(record -> query.operatorPersonId() == null
                        || query.operatorPersonId().equals(record.operatorPersonId()))
                .filter(record -> query.traceId() == null || query.traceId().equals(record.traceId()))
                .filter(record -> query.from() == null || !record.occurredAt().isBefore(query.from()))
                .filter(record -> query.to() == null || !record.occurredAt().isAfter(query.to()))
                .sorted(Comparator.comparing(AuditRecord::occurredAt)
                        .thenComparing(AuditRecord::createdAt)
                        .reversed())
                .toList();
    }

    @Override
    public List<AuditRecord> findByTenantAndTimeRange(UUID tenantId, Instant from, Instant to) {
        return recordsById.values().stream()
                .filter(record -> tenantId.equals(record.tenantId()))
                .filter(record -> from == null || !record.occurredAt().isBefore(from))
                .filter(record -> to == null || !record.occurredAt().isAfter(to))
                .sorted(Comparator.comparing(AuditRecord::occurredAt))
                .toList();
    }
}
