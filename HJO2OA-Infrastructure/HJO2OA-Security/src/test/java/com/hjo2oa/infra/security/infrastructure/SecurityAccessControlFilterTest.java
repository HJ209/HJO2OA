package com.hjo2oa.infra.security.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hjo2oa.infra.security.application.SecurityPolicyApplicationService;
import com.hjo2oa.infra.security.domain.RateLimitSubjectType;
import com.hjo2oa.infra.security.domain.SecurityPolicy;
import com.hjo2oa.infra.security.domain.SecurityPolicyRepository;
import com.hjo2oa.infra.security.domain.SecurityPolicyType;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SecurityAccessControlFilterTest {

    @Test
    void shouldEnforceIpWhitelistAndRateLimit() throws Exception {
        InMemorySecurityPolicyRepository repository = new InMemorySecurityPolicyRepository();
        SecurityPolicyApplicationService applicationService = new SecurityPolicyApplicationService(
                repository,
                event -> {
                },
                Clock.fixed(Instant.parse("2026-04-29T08:10:00Z"), ZoneOffset.UTC)
        );
        var policy = applicationService.createPolicy(
                "access-policy",
                SecurityPolicyType.ACCESS_CONTROL,
                "Access policy",
                "{\"paths\":[\"/api/v1/infra/test\"],\"ipWhitelist\":[\"127.0.0.1\"]}",
                null
        );
        applicationService.addRateLimitRule(policy.id(), RateLimitSubjectType.IP, 60, 2);
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        SecurityAccessControlFilter filter = new SecurityAccessControlFilter(
                new SecurityAccessPolicyResolver(repository, objectMapper),
                objectMapper,
                new ResponseMetaFactory(),
                Clock.fixed(Instant.parse("2026-04-29T08:10:00Z"), ZoneOffset.UTC)
        );

        assertThat(execute(filter, "127.0.0.1").getStatus()).isEqualTo(200);
        assertThat(execute(filter, "127.0.0.1").getStatus()).isEqualTo(200);

        MockHttpServletResponse limited = execute(filter, "127.0.0.1");
        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getContentAsString()).contains("SECURITY_RATE_LIMIT_EXCEEDED");

        MockHttpServletResponse forbidden = execute(filter, "10.0.0.8");
        assertThat(forbidden.getStatus()).isEqualTo(403);
        assertThat(forbidden.getContentAsString()).contains("SECURITY_IP_NOT_ALLOWED");
    }

    @Test
    void shouldBypassVersionedAuthEndpointBeforePolicyResolution() throws Exception {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        SecurityAccessControlFilter filter = new SecurityAccessControlFilter(
                new SecurityAccessPolicyResolver(new ThrowingSecurityPolicyRepository(), objectMapper),
                objectMapper,
                new ResponseMetaFactory(),
                Clock.fixed(Instant.parse("2026-04-29T08:10:00Z"), ZoneOffset.UTC)
        );
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServletPath("/api/v1/auth/login");
        request.addHeader("X-Request-Id", "req-auth-filter");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse execute(SecurityAccessControlFilter filter, String remoteAddress) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/infra/test");
        request.setServletPath("/api/v1/infra/test");
        request.setRemoteAddr(remoteAddress);
        request.addHeader("X-Request-Id", "req-access-filter");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private static final class ThrowingSecurityPolicyRepository implements SecurityPolicyRepository {

        @Override
        public Optional<SecurityPolicy> findById(UUID id) {
            throw new AssertionError("auth endpoints must not resolve security policies");
        }

        @Override
        public Optional<SecurityPolicy> findByPolicyCode(String policyCode) {
            throw new AssertionError("auth endpoints must not resolve security policies");
        }

        @Override
        public List<SecurityPolicy> findAll() {
            throw new AssertionError("auth endpoints must not resolve security policies");
        }

        @Override
        public SecurityPolicy save(SecurityPolicy securityPolicy) {
            throw new AssertionError("auth endpoints must not resolve security policies");
        }
    }
}
