package com.hjo2oa.content.permission.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicationScopeRepository {

    void replaceScopes(
            UUID tenantId,
            UUID publicationId,
            UUID articleId,
            List<ContentPermissionApplicationService.PublicationScopeRuleRecord> rules
    );

    List<ContentPermissionApplicationService.PublicationScopeRuleRecord> findByPublication(
            UUID tenantId,
            UUID publicationId
    );

    List<ContentPermissionApplicationService.PublicationScopeRuleRecord> findByArticle(UUID tenantId, UUID articleId);

    Optional<Integer> latestScopeVersion(UUID tenantId, UUID publicationId);
}
