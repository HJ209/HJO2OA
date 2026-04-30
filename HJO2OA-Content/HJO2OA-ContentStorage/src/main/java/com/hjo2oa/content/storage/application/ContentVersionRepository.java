package com.hjo2oa.content.storage.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentVersionRepository {

    int nextVersionNo(UUID tenantId, UUID articleId);

    void save(ContentStorageApplicationService.ContentVersionRecord version);

    Optional<ContentStorageApplicationService.ContentVersionRecord> findByVersionNo(
            UUID tenantId,
            UUID articleId,
            int versionNo
    );

    Optional<ContentStorageApplicationService.ContentVersionRecord> findByIdempotencyKey(
            UUID tenantId,
            UUID articleId,
            String idempotencyKey
    );

    Optional<ContentStorageApplicationService.ContentVersionRecord> latest(UUID tenantId, UUID articleId);

    List<ContentStorageApplicationService.ContentVersionRecord> findByArticle(UUID tenantId, UUID articleId);
}
