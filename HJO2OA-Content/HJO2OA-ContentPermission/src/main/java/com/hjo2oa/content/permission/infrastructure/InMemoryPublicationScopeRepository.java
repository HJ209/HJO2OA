package com.hjo2oa.content.permission.infrastructure;

import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.PublicationScopeRuleRecord;
import com.hjo2oa.content.permission.application.PublicationScopeRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPublicationScopeRepository implements PublicationScopeRepository {

    private final Map<UUID, List<PublicationScopeRuleRecord>> byPublication = new ConcurrentHashMap<>();

    @Override
    public void replaceScopes(UUID tenantId, UUID publicationId, UUID articleId, List<PublicationScopeRuleRecord> rules) {
        byPublication.put(publicationId, rules == null ? List.of() : List.copyOf(rules));
    }

    @Override
    public List<PublicationScopeRuleRecord> findByPublication(UUID tenantId, UUID publicationId) {
        return byPublication.getOrDefault(publicationId, List.of()).stream()
                .filter(rule -> rule.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(PublicationScopeRuleRecord::sortOrder))
                .toList();
    }

    @Override
    public List<PublicationScopeRuleRecord> findByArticle(UUID tenantId, UUID articleId) {
        return byPublication.values()
                .stream()
                .flatMap(List::stream)
                .filter(rule -> rule.tenantId().equals(tenantId))
                .filter(rule -> rule.articleId().equals(articleId))
                .sorted(Comparator.comparing(PublicationScopeRuleRecord::sortOrder))
                .toList();
    }

    @Override
    public Optional<Integer> latestScopeVersion(UUID tenantId, UUID publicationId) {
        return findByPublication(tenantId, publicationId).stream()
                .map(PublicationScopeRuleRecord::scopeVersion)
                .max(Integer::compareTo);
    }
}
