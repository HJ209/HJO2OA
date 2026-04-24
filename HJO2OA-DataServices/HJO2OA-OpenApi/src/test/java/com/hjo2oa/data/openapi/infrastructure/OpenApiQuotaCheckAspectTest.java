package com.hjo2oa.data.openapi.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.data.openapi.OpenApiTestFixtures;
import com.hjo2oa.data.openapi.application.OpenApiQuotaChecked;
import com.hjo2oa.data.openapi.application.OpenApiQuotaEnforcementService;
import com.hjo2oa.data.openapi.domain.ApiCredentialGrant;
import com.hjo2oa.data.openapi.domain.ApiPolicyType;
import com.hjo2oa.data.openapi.domain.ApiRateLimitPolicy;
import com.hjo2oa.data.openapi.domain.ApiWindowUnit;
import com.hjo2oa.data.openapi.domain.AuthenticatedOpenApiInvocationContext;
import com.hjo2oa.data.openapi.domain.OpenApiAuthType;
import com.hjo2oa.data.openapi.domain.OpenApiEndpoint;
import com.hjo2oa.data.openapi.domain.OpenApiHttpMethod;
import com.hjo2oa.shared.kernel.BizException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

class OpenApiQuotaCheckAspectTest {

    @Test
    void shouldRejectWhenRateLimitThresholdExceeded() {
        OpenApiInvocationContextHolder contextHolder = new OpenApiInvocationContextHolder();
        InMemoryApiQuotaUsageCounterRepository counterRepository = new InMemoryApiQuotaUsageCounterRepository();
        OpenApiQuotaEnforcementService quotaEnforcementService = new OpenApiQuotaEnforcementService(
                counterRepository,
                Clock.fixed(OpenApiTestFixtures.FIXED_TIME, ZoneOffset.UTC)
        );
        OpenApiQuotaCheckAspect aspect = new OpenApiQuotaCheckAspect(contextHolder, quotaEnforcementService);
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new AnnotatedInvoker());
        proxyFactory.addAspect(aspect);
        AnnotatedInvoker proxy = proxyFactory.getProxy();

        ApiCredentialGrant credentialGrant = ApiCredentialGrant.create(
                "api-1",
                OpenApiTestFixtures.TENANT_ID.toString(),
                "partner-app",
                "secret-001",
                List.of("employee.read"),
                OpenApiTestFixtures.FIXED_TIME.plusSeconds(3600),
                OpenApiTestFixtures.FIXED_TIME
        );
        ApiRateLimitPolicy rateLimitPolicy = ApiRateLimitPolicy.create(
                "api-1",
                OpenApiTestFixtures.TENANT_ID.toString(),
                "burst-1",
                "partner-app",
                ApiPolicyType.RATE_LIMIT,
                1,
                ApiWindowUnit.MINUTE,
                1,
                "burst control",
                OpenApiTestFixtures.FIXED_TIME
        );
        OpenApiEndpoint endpoint = OpenApiEndpoint.create(
                OpenApiTestFixtures.TENANT_ID.toString(),
                "employee-directory",
                "Employee Directory",
                UUID.randomUUID().toString(),
                "employee.query",
                "Employee Query",
                "/api/open/employees",
                OpenApiHttpMethod.GET,
                "v1",
                OpenApiAuthType.APP_KEY,
                null,
                OpenApiTestFixtures.FIXED_TIME
        ).withCredential(credentialGrant).withPolicy(rateLimitPolicy).publish(OpenApiTestFixtures.FIXED_TIME);
        contextHolder.set(new AuthenticatedOpenApiInvocationContext(
                "req-1",
                OpenApiTestFixtures.TENANT_ID.toString(),
                "partner-app",
                endpoint,
                credentialGrant,
                OpenApiTestFixtures.FIXED_TIME
        ));

        assertThat(proxy.invoke()).isEqualTo("ok");
        assertThatThrownBy(proxy::invoke)
                .isInstanceOf(BizException.class)
                .hasMessageContaining("rate limit");
        assertThat(counterRepository.findAllByTenant(OpenApiTestFixtures.TENANT_ID.toString()))
                .singleElement()
                .satisfies(counter -> assertThat(counter.usedCount()).isEqualTo(1L));
    }

    static class AnnotatedInvoker {

        @OpenApiQuotaChecked
        String invoke() {
            return "ok";
        }
    }
}
