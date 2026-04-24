package com.hjo2oa.data.common.infrastructure.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Slf4jDataAuditLogWriter implements DataAuditLogWriter {

    @Override
    public void write(DataAuditRecord record) {
        log.info(
                "data-audit module={} action={} targetType={} method={} tenantId={} operatorId={} requestId={} success={} durationMs={} errorCode={} message={} detail={}",
                record.module(),
                record.action(),
                record.targetType(),
                record.methodName(),
                record.tenantId(),
                record.operatorId(),
                record.requestId(),
                record.success(),
                record.durationMs(),
                record.errorCode(),
                record.message(),
                record.detail()
        );
    }
}
