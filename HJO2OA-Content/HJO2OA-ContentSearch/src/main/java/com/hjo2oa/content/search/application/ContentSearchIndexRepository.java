package com.hjo2oa.content.search.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentSearchIndexRepository {

    void save(ContentSearchApplicationService.ContentSearchDocument document);

    void remove(UUID tenantId, UUID articleId);

    Optional<ContentSearchApplicationService.ContentSearchDocument> findByArticleId(UUID tenantId, UUID articleId);

    List<ContentSearchApplicationService.ContentSearchDocument> search(ContentSearchApplicationService.ContentSearchCriteria criteria);
}
