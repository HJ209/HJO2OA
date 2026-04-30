package com.hjo2oa.infra.security.application;

import com.hjo2oa.shared.kernel.ErrorDescriptor;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import org.springframework.http.HttpStatus;

public final class SecurityErrorDescriptors {

    public static final ErrorDescriptor SECURITY_POLICY_NOT_FOUND =
            SharedErrorDescriptors.of("SECURITY_POLICY_NOT_FOUND", HttpStatus.NOT_FOUND, "Security policy not found");
    public static final ErrorDescriptor SECURITY_POLICY_CODE_DUPLICATE =
            SharedErrorDescriptors.of("SECURITY_POLICY_CODE_DUPLICATE", HttpStatus.CONFLICT, "Security policy code exists");
    public static final ErrorDescriptor SECURITY_POLICY_RULE_VIOLATION =
            SharedErrorDescriptors.of(
                    "SECURITY_POLICY_RULE_VIOLATION",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Security policy rule is invalid"
            );
    public static final ErrorDescriptor SECURITY_SECRET_KEY_NOT_FOUND =
            SharedErrorDescriptors.of("SECURITY_SECRET_KEY_NOT_FOUND", HttpStatus.NOT_FOUND, "Security key not found");
    public static final ErrorDescriptor SECURITY_CRYPTO_FAILED =
            SharedErrorDescriptors.of(
                    "SECURITY_CRYPTO_FAILED",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Security crypto operation failed"
            );
    public static final ErrorDescriptor SECURITY_PASSWORD_POLICY_REJECTED =
            SharedErrorDescriptors.of(
                    "SECURITY_PASSWORD_POLICY_REJECTED",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Password policy rejected the candidate"
            );
    public static final ErrorDescriptor SECURITY_IP_NOT_ALLOWED =
            SharedErrorDescriptors.of("SECURITY_IP_NOT_ALLOWED", HttpStatus.FORBIDDEN, "IP address is not allowed");
    public static final ErrorDescriptor SECURITY_RATE_LIMIT_EXCEEDED =
            SharedErrorDescriptors.of(
                    "SECURITY_RATE_LIMIT_EXCEEDED",
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Request rate limit exceeded"
            );

    private SecurityErrorDescriptors() {
    }
}
