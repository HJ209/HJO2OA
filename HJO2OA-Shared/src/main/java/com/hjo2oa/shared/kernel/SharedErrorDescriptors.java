package com.hjo2oa.shared.kernel;

import org.springframework.http.HttpStatus;

public final class SharedErrorDescriptors {

    public static final ErrorDescriptor BAD_REQUEST =
            of("BAD_REQUEST", HttpStatus.BAD_REQUEST, "请求格式错误");
    public static final ErrorDescriptor VALIDATION_ERROR =
            of("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "请求参数校验失败");
    public static final ErrorDescriptor UNAUTHORIZED =
            of("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "未认证");
    public static final ErrorDescriptor FORBIDDEN =
            of("FORBIDDEN", HttpStatus.FORBIDDEN, "无权限");
    public static final ErrorDescriptor TENANT_REQUIRED =
            of("TENANT_REQUIRED", HttpStatus.BAD_REQUEST, "缺少租户上下文");
    public static final ErrorDescriptor TENANT_ACCESS_DENIED =
            of("TENANT_ACCESS_DENIED", HttpStatus.FORBIDDEN, "无权访问当前租户数据");
    public static final ErrorDescriptor RESOURCE_NOT_FOUND =
            of("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "资源不存在");
    public static final ErrorDescriptor CONFLICT =
            of("CONFLICT", HttpStatus.CONFLICT, "资源冲突");
    public static final ErrorDescriptor IDEMPOTENCY_CONFLICT =
            of("IDEMPOTENCY_CONFLICT", HttpStatus.CONFLICT, "幂等键冲突");
    public static final ErrorDescriptor BUSINESS_RULE_VIOLATION =
            of("BUSINESS_RULE_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY, "业务规则校验失败");
    public static final ErrorDescriptor INTERNAL_ERROR =
            of("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");

    private SharedErrorDescriptors() {
    }

    public static ErrorDescriptor of(String code, HttpStatus httpStatus, String defaultMessage) {
        return new ErrorDescriptor(code, httpStatus, defaultMessage);
    }
}
