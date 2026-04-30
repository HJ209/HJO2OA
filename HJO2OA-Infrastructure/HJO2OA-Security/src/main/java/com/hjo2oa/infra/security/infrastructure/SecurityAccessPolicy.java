package com.hjo2oa.infra.security.infrastructure;

import com.hjo2oa.infra.security.domain.RateLimitRule;
import java.util.List;

record SecurityAccessPolicy(
        List<String> paths,
        List<String> ipWhitelist,
        List<RateLimitRule> rateLimitRules
) {

    SecurityAccessPolicy {
        paths = paths == null ? List.of() : List.copyOf(paths);
        ipWhitelist = ipWhitelist == null ? List.of() : List.copyOf(ipWhitelist);
        rateLimitRules = rateLimitRules == null ? List.of() : List.copyOf(rateLimitRules);
    }

    boolean matchesPath(String path) {
        if (paths.isEmpty()) {
            return true;
        }
        return paths.stream().anyMatch(path::startsWith);
    }
}
