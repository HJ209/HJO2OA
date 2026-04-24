package com.hjo2oa.data.common.infrastructure.audit;

public interface DataAuditLogWriter {

    void write(DataAuditRecord record);
}
