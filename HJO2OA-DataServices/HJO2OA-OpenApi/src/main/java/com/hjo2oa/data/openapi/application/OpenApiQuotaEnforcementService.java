package com.hjo2oa.data.openapi.application;

import com.hjo2oa.data.openapi.domain.ApiPolicyType;
import com.hjo2oa.data.openapi.domain.ApiQuotaUsageCounter;
import com.hjo2oa.data.openapi.domain.ApiQuotaUsageCounterRepository;
import com.hjo2oa.data.openapi.domain.ApiRateLimitPolicy;
import com.hjo2oa.data.openapi.domain.AuthenticatedOpenApiInvocationContext;
import com.hjo2oa.data.openapi.domain.OpenApiErrorDescriptors;
import com.hjo2oa.shared.kernel.BizException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenApiQuotaEnforcementService {

    private final ApiQuotaUsageCounterRepository quotaUsageCounterRepository;
    private final Clock clock;
    @Autowired
    public OpenApiQuotaEnforcementService(ApiQuotaUsageCounterRepository quotaUsageCounterRepository) {
        this(quotaUsageCounterRepository, Clock.systemUTC());
    }
    public OpenApiQuotaEnforcementService(ApiQuotaUsageCounterRepository quotaUsageCounterRepository, Clock clock) {
        this.quotaUsageCounterRepository = Objects.requireNonNull(
                quotaUsageCounterRepository,
                "quotaUsageCounterRepository must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public void checkAndConsume(AuthenticatedOpenApiInvocationContext context) {
        Instant now = clock.instant();
        for (ApiRateLimitPolicy policy : context.endpoint().activePoliciesFor(context.clientCode())) {
            String counterClientCode = policy.clientCode() == null ? "*" : context.clientCode();
            Instant windowStartedAt = policy.windowStartedAt(now);
            ApiQuotaUsageCounter counter = quotaUsageCounterRepository.findByWindow(
                            context.tenantId(),
                            policy.policyId(),
                            counterClientCode,
                            windowStartedAt
                    )
                    .orElseGet(() -> ApiQuotaUsageCounter.create(
                            context.tenantId(),
                            policy.policyId(),
                            context.endpoint().apiId(),
                            counterClientCode,
                            windowStartedAt,
                            now
                    ));
            if (counter.usedCount() >= policy.threshold()) {
                if (policy.policyType() == ApiPolicyType.QUOTA) {
                    throw new BizException(OpenApiErrorDescriptors.QUOTA_EXCEEDED, "API quota exceeded");
                }
                throw new BizException(OpenApiErrorDescriptors.RATE_LIMIT_EXCEEDED, "API rate limit exceeded");
            }
            quotaUsageCounterRepository.save(counter.increment(now));
        }
    }
}
