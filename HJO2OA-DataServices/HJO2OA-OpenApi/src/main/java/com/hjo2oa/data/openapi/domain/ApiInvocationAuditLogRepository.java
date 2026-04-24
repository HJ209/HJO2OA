package com.hjo2oa.data.openapi.domain;

import java.util.List;
import java.util.Optional;

public interface ApiInvocationAuditLogRepository {

    Optional<ApiInvocationAuditLog> findById(String logId);

    List<ApiInvocationAuditLog> findAllByTenant(String tenantId);

    ApiInvocationAuditLog save(ApiInvocationAuditLog auditLog);
}
