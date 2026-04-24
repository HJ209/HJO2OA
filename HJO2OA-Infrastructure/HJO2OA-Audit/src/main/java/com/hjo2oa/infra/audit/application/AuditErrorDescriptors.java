package com.hjo2oa.infra.audit.application;

import com.hjo2oa.shared.kernel.ErrorDescriptor;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import org.springframework.http.HttpStatus;

public final class AuditErrorDescriptors {

    public static final ErrorDescriptor AUDIT_RECORD_NOT_FOUND =
            SharedErrorDescriptors.of("INFRA_AUDIT_RECORD_NOT_FOUND", HttpStatus.NOT_FOUND, "审计记录不存在");

    private AuditErrorDescriptors() {
    }
}
