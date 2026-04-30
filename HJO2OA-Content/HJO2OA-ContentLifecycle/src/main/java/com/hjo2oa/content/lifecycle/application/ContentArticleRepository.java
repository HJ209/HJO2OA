package com.hjo2oa.content.lifecycle.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentArticleRepository {

    void save(ContentLifecycleApplicationService.ContentArticleRecord article);

    Optional<ContentLifecycleApplicationService.ContentArticleRecord> findById(UUID tenantId, UUID articleId);

    Optional<ContentLifecycleApplicationService.ContentArticleRecord> findByArticleNo(UUID tenantId, String articleNo);

    List<ContentLifecycleApplicationService.ContentArticleRecord> search(
            ContentLifecycleApplicationService.ArticleListQuery query
    );
}
