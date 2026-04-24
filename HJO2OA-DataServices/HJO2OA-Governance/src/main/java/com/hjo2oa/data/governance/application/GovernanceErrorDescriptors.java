package com.hjo2oa.data.governance.application;

import com.hjo2oa.shared.kernel.ErrorDescriptor;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import org.springframework.http.HttpStatus;

public final class GovernanceErrorDescriptors {

    public static final ErrorDescriptor RULE_CONFLICT =
            SharedErrorDescriptors.of("GOVERNANCE_RULE_CONFLICT", HttpStatus.CONFLICT, "治理规则冲突");
    public static final ErrorDescriptor INVALID_STATUS_TRANSITION =
            SharedErrorDescriptors.of("GOVERNANCE_INVALID_STATUS", HttpStatus.UNPROCESSABLE_ENTITY, "治理状态流转非法");
    public static final ErrorDescriptor DEPENDENCY_NOT_READY =
            SharedErrorDescriptors.of("GOVERNANCE_DEPENDENCY_NOT_READY", HttpStatus.PRECONDITION_FAILED, "治理依赖状态未满足");
    public static final ErrorDescriptor STRATEGY_NOT_ALLOWED =
            SharedErrorDescriptors.of("GOVERNANCE_STRATEGY_NOT_ALLOWED", HttpStatus.FORBIDDEN, "治理策略未显式允许该动作");
    public static final ErrorDescriptor AUDIT_REQUIRED =
            SharedErrorDescriptors.of("GOVERNANCE_AUDIT_REQUIRED", HttpStatus.BAD_REQUEST, "治理动作缺少审计标识");

    private GovernanceErrorDescriptors() {
    }
}
