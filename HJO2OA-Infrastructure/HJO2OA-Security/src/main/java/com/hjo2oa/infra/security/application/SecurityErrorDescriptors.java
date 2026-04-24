package com.hjo2oa.infra.security.application;

import com.hjo2oa.shared.kernel.ErrorDescriptor;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import org.springframework.http.HttpStatus;

public final class SecurityErrorDescriptors {

    public static final ErrorDescriptor SECURITY_POLICY_NOT_FOUND =
            SharedErrorDescriptors.of("SECURITY_POLICY_NOT_FOUND", HttpStatus.NOT_FOUND, "安全策略不存在");
    public static final ErrorDescriptor SECURITY_POLICY_CODE_DUPLICATE =
            SharedErrorDescriptors.of("SECURITY_POLICY_CODE_DUPLICATE", HttpStatus.CONFLICT, "安全策略编码冲突");
    public static final ErrorDescriptor SECURITY_POLICY_RULE_VIOLATION =
            SharedErrorDescriptors.of("SECURITY_POLICY_RULE_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY, "安全策略规则不合法");
    public static final ErrorDescriptor SECURITY_SECRET_KEY_NOT_FOUND =
            SharedErrorDescriptors.of("SECURITY_SECRET_KEY_NOT_FOUND", HttpStatus.NOT_FOUND, "安全密钥不存在");

    private SecurityErrorDescriptors() {
    }
}
