package com.hjo2oa.data.openapi.infrastructure;

import com.hjo2oa.data.openapi.domain.ApiInvocationAuditLog;
import com.hjo2oa.data.openapi.domain.ApiInvocationAuditLogRepository;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryApiInvocationAuditLogRepository implements ApiInvocationAuditLogRepository {

    private final Map<String, ApiInvocationAuditLog> logsById = new ConcurrentHashMap<>();

    @Override
    public Optional<ApiInvocationAuditLog> findById(String logId) {
        return Optional.ofNullable(logsById.get(logId));
    }

    @Override
    public List<ApiInvocationAuditLog> findAllByTenant(String tenantId) {
        return logsById.values().stream()
                .filter(log -> log.tenantId().equals(tenantId))
                .toList();
    }

    @Override
    public ApiInvocationAuditLog save(ApiInvocationAuditLog auditLog) {
        logsById.put(auditLog.logId(), auditLog);
        return auditLog;
    }
}
