package com.hjo2oa.infra.cache.application;

import com.hjo2oa.shared.kernel.ErrorDescriptor;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import org.springframework.http.HttpStatus;

public final class CacheErrorDescriptors {

    public static final ErrorDescriptor POLICY_NOT_FOUND =
            SharedErrorDescriptors.of("INFRA_CACHE_POLICY_NOT_FOUND", HttpStatus.NOT_FOUND, "缓存策略不存在");
    public static final ErrorDescriptor NAMESPACE_CONFLICT =
            SharedErrorDescriptors.of("INFRA_CACHE_NAMESPACE_CONFLICT", HttpStatus.CONFLICT, "缓存命名空间已存在");
    public static final ErrorDescriptor POLICY_INACTIVE =
            SharedErrorDescriptors.of("INFRA_CACHE_POLICY_INACTIVE", HttpStatus.UNPROCESSABLE_ENTITY, "缓存策略已停用");
    public static final ErrorDescriptor INVALIDATION_MODE_MISMATCH =
            SharedErrorDescriptors.of(
                    "INFRA_CACHE_INVALIDATION_MODE_MISMATCH",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "缓存策略失效模式与当前操作不匹配"
            );

    private CacheErrorDescriptors() {
    }
}
