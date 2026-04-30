package com.hjo2oa.content.lifecycle.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentPublicationRepository {

    void save(ContentLifecycleApplicationService.ContentPublicationRecord publication);

    Optional<ContentLifecycleApplicationService.ContentPublicationRecord> findById(UUID tenantId, UUID publicationId);

    Optional<ContentLifecycleApplicationService.ContentPublicationRecord> findActiveByArticle(UUID tenantId, UUID articleId);

    Optional<ContentLifecycleApplicationService.ContentPublicationRecord> findPendingReview(
            UUID tenantId,
            UUID articleId,
            int versionNo
    );

    List<ContentLifecycleApplicationService.ContentPublicationRecord> findByArticle(UUID tenantId, UUID articleId);

    void appendReview(ContentLifecycleApplicationService.ContentReviewRecord record);

    List<ContentLifecycleApplicationService.ContentReviewRecord> reviews(UUID tenantId, UUID articleId);
}
