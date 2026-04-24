package com.hjo2oa.data.openapi.domain;

import com.hjo2oa.shared.kernel.ErrorDescriptor;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import org.springframework.http.HttpStatus;

public final class OpenApiErrorDescriptors {

    public static final ErrorDescriptor DATA_SERVICE_NOT_FOUND =
            SharedErrorDescriptors.of("DATA_SERVICE_NOT_FOUND", HttpStatus.UNPROCESSABLE_ENTITY, "引用的数据服务不存在或不可用");
    public static final ErrorDescriptor API_PATH_CONFLICT =
            SharedErrorDescriptors.of("API_PATH_CONFLICT", HttpStatus.CONFLICT, "开放接口路径与方法存在冲突");
    public static final ErrorDescriptor API_VERSION_NOT_FOUND =
            SharedErrorDescriptors.of("API_VERSION_NOT_FOUND", HttpStatus.NOT_FOUND, "开放接口版本不存在");
    public static final ErrorDescriptor API_VERSION_IMMUTABLE =
            SharedErrorDescriptors.of("API_VERSION_IMMUTABLE", HttpStatus.CONFLICT, "已发布接口版本不允许原地覆盖");
    public static final ErrorDescriptor API_STATUS_INVALID =
            SharedErrorDescriptors.of("API_STATUS_INVALID", HttpStatus.UNPROCESSABLE_ENTITY, "接口当前状态不允许执行该操作");
    public static final ErrorDescriptor AUTH_TYPE_UNSUPPORTED =
            SharedErrorDescriptors.of("AUTH_TYPE_UNSUPPORTED", HttpStatus.UNAUTHORIZED, "当前接口鉴权方式暂不支持");
    public static final ErrorDescriptor CREDENTIAL_NOT_FOUND =
            SharedErrorDescriptors.of("CREDENTIAL_NOT_FOUND", HttpStatus.UNAUTHORIZED, "调用凭证不存在");
    public static final ErrorDescriptor CREDENTIAL_EXPIRED =
            SharedErrorDescriptors.of("CREDENTIAL_EXPIRED", HttpStatus.UNAUTHORIZED, "调用凭证已过期");
    public static final ErrorDescriptor SIGNATURE_INVALID =
            SharedErrorDescriptors.of("SIGNATURE_INVALID", HttpStatus.UNAUTHORIZED, "请求签名无效");
    public static final ErrorDescriptor RATE_LIMIT_EXCEEDED =
            SharedErrorDescriptors.of("RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS, "接口已触发限流阈值");
    public static final ErrorDescriptor QUOTA_EXCEEDED =
            SharedErrorDescriptors.of("QUOTA_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS, "接口调用配额已超限");
    public static final ErrorDescriptor AUDIT_LOG_NOT_FOUND =
            SharedErrorDescriptors.of("AUDIT_LOG_NOT_FOUND", HttpStatus.NOT_FOUND, "调用审计日志不存在");
    public static final ErrorDescriptor OPEN_API_NOT_CALLABLE =
            SharedErrorDescriptors.of("OPEN_API_NOT_CALLABLE", HttpStatus.NOT_FOUND, "接口版本不存在或当前不可调用");

    private OpenApiErrorDescriptors() {
    }
}
