package com.hjo2oa.infra.security.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.infra.security.application.SecurityErrorDescriptors;
import com.hjo2oa.infra.security.domain.RateLimitRule;
import com.hjo2oa.infra.security.domain.RateLimitSubjectType;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMeta;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 35)
public class SecurityAccessControlFilter extends OncePerRequestFilter {

    private final SecurityAccessPolicyResolver accessPolicyResolver;
    private final ObjectMapper objectMapper;
    private final ResponseMetaFactory responseMetaFactory;
    private final Clock clock;
    private final Map<String, CounterBucket> counters = new ConcurrentHashMap<>();

    @Autowired
    public SecurityAccessControlFilter(
            SecurityAccessPolicyResolver accessPolicyResolver,
            ObjectMapper objectMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this(accessPolicyResolver, objectMapper, responseMetaFactory, Clock.systemUTC());
    }

    SecurityAccessControlFilter(
            SecurityAccessPolicyResolver accessPolicyResolver,
            ObjectMapper objectMapper,
            ResponseMetaFactory responseMetaFactory,
            Clock clock
    ) {
        this.accessPolicyResolver = Objects.requireNonNull(
                accessPolicyResolver,
                "accessPolicyResolver must not be null"
        );
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.responseMetaFactory = Objects.requireNonNull(responseMetaFactory, "responseMetaFactory must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith("/api/")
                || path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/auth/")
                || path.startsWith("/actuator/")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        List<SecurityAccessPolicy> policies = accessPolicyResolver.resolve().stream()
                .filter(policy -> policy.matchesPath(request.getServletPath()))
                .toList();
        if (policies.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        String clientIp = clientIp(request);
        if (policies.stream().anyMatch(policy -> !policy.ipWhitelist().isEmpty())
                && policies.stream()
                        .filter(policy -> !policy.ipWhitelist().isEmpty())
                        .noneMatch(policy -> matchesAnyIp(clientIp, policy.ipWhitelist()))) {
            writeFailure(response, request, SecurityErrorDescriptors.SECURITY_IP_NOT_ALLOWED, 0);
            return;
        }
        for (SecurityAccessPolicy policy : policies) {
            for (RateLimitRule rule : policy.rateLimitRules()) {
                RateLimitDecision decision = checkRateLimit(rule, request, clientIp);
                if (!decision.allowed()) {
                    writeFailure(
                            response,
                            request,
                            SecurityErrorDescriptors.SECURITY_RATE_LIMIT_EXCEEDED,
                            decision.retryAfterSeconds()
                    );
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private RateLimitDecision checkRateLimit(RateLimitRule rule, HttpServletRequest request, String clientIp) {
        String subject = subject(rule.subjectType(), request, clientIp);
        String key = rule.id() + ":" + subject + ":" + request.getServletPath();
        long nowEpoch = clock.instant().getEpochSecond();
        CounterBucket bucket = counters.compute(key, (ignored, existing) -> {
            if (existing == null || existing.expiresAtEpochSecond() <= nowEpoch) {
                return new CounterBucket(nowEpoch + rule.windowSeconds(), 1);
            }
            return existing.increment();
        });
        if (bucket.count() <= rule.maxRequests()) {
            return RateLimitDecision.allow();
        }
        return RateLimitDecision.rejected(Math.max(1, bucket.expiresAtEpochSecond() - nowEpoch));
    }

    private String subject(RateLimitSubjectType subjectType, HttpServletRequest request, String clientIp) {
        return switch (subjectType) {
            case IP -> clientIp;
            case USER -> nullToAnonymous(request.getUserPrincipal() == null ? null : request.getUserPrincipal().getName());
            case TENANT -> nullToAnonymous(request.getHeader("X-Tenant-Id"));
            case API_CLIENT -> nullToAnonymous(request.getHeader("X-Api-Client-Id"));
        };
    }

    private boolean matchesAnyIp(String clientIp, List<String> whitelist) {
        return whitelist.stream().anyMatch(entry -> ipMatches(clientIp, entry));
    }

    private boolean ipMatches(String clientIp, String whitelistEntry) {
        if (whitelistEntry == null || whitelistEntry.isBlank()) {
            return false;
        }
        String normalized = whitelistEntry.trim();
        if (!normalized.contains("/")) {
            return clientIp.equals(normalized);
        }
        String[] parts = normalized.split("/", 2);
        int prefix = Integer.parseInt(parts[1]);
        int mask = prefix == 0 ? 0 : -1 << (32 - prefix);
        return (ipv4ToInt(clientIp) & mask) == (ipv4ToInt(parts[0]) & mask);
    }

    private int ipv4ToInt(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return 0;
        }
        int value = 0;
        for (String part : parts) {
            value = (value << 8) + Integer.parseInt(part);
        }
        return value;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeFailure(
            HttpServletResponse response,
            HttpServletRequest request,
            com.hjo2oa.shared.kernel.ErrorDescriptor descriptor,
            long retryAfterSeconds
    ) throws IOException {
        response.setStatus(descriptor.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (retryAfterSeconds > 0) {
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        }
        ResponseMeta meta = responseMetaFactory.create(request);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failure(
                descriptor,
                descriptor.defaultMessage(),
                null,
                new ResponseMeta(
                        meta.requestId(),
                        Instant.now(clock),
                        meta.serverTimezone(),
                        meta.tenantId(),
                        meta.language(),
                        meta.timezone(),
                        meta.idempotencyKey()
                )
        )));
    }

    private String nullToAnonymous(String value) {
        return value == null || value.isBlank() ? "anonymous" : value;
    }

    private record CounterBucket(
            long expiresAtEpochSecond,
            int count
    ) {

        CounterBucket increment() {
            return new CounterBucket(expiresAtEpochSecond, count + 1);
        }
    }

    private record RateLimitDecision(
            boolean allowed,
            long retryAfterSeconds
    ) {

        static RateLimitDecision allow() {
            return new RateLimitDecision(true, 0);
        }

        static RateLimitDecision rejected(long retryAfterSeconds) {
            return new RateLimitDecision(false, retryAfterSeconds);
        }
    }
}
