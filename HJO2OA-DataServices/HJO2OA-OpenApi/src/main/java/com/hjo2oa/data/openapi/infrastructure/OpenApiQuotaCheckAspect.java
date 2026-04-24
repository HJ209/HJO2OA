package com.hjo2oa.data.openapi.infrastructure;

import com.hjo2oa.data.openapi.application.OpenApiQuotaEnforcementService;
import com.hjo2oa.data.openapi.domain.ApiInvocationOutcome;
import com.hjo2oa.data.openapi.domain.OpenApiErrorDescriptors;
import com.hjo2oa.shared.kernel.BizException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class OpenApiQuotaCheckAspect {

    private final OpenApiInvocationContextHolder contextHolder;
    private final OpenApiQuotaEnforcementService quotaEnforcementService;

    public OpenApiQuotaCheckAspect(
            OpenApiInvocationContextHolder contextHolder,
            OpenApiQuotaEnforcementService quotaEnforcementService
    ) {
        this.contextHolder = contextHolder;
        this.quotaEnforcementService = quotaEnforcementService;
    }

    @Around("@annotation(com.hjo2oa.data.openapi.application.OpenApiQuotaChecked)")
    public Object enforceQuota(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            quotaEnforcementService.checkAndConsume(contextHolder.getRequired());
            return joinPoint.proceed();
        } catch (BizException ex) {
            if (OpenApiErrorDescriptors.RATE_LIMIT_EXCEEDED.code().equals(ex.errorCode())) {
                OpenApiInvocationRequestAttributes.mark(ApiInvocationOutcome.RATE_LIMITED, ex.errorCode());
            } else if (OpenApiErrorDescriptors.QUOTA_EXCEEDED.code().equals(ex.errorCode())) {
                OpenApiInvocationRequestAttributes.mark(ApiInvocationOutcome.QUOTA_EXCEEDED, ex.errorCode());
            }
            throw ex;
        }
    }
}
