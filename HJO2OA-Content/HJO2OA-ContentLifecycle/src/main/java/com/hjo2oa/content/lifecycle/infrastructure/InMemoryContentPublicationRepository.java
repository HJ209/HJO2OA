package com.hjo2oa.content.lifecycle.infrastructure;

import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ContentPublicationRecord;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ContentReviewRecord;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.PublicationStatus;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ReviewMode;
import com.hjo2oa.content.lifecycle.application.ContentPublicationRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryContentPublicationRepository implements ContentPublicationRepository {

    private final Map<UUID, ContentPublicationRecord> publications = new ConcurrentHashMap<>();
    private final List<ContentReviewRecord> reviews = new CopyOnWriteArrayList<>();

    @Override
    public void save(ContentPublicationRecord publication) {
        publications.put(publication.id(), publication);
    }

    @Override
    public Optional<ContentPublicationRecord> findById(UUID tenantId, UUID publicationId) {
        return Optional.ofNullable(publications.get(publicationId)).filter(publication -> publication.tenantId().equals(tenantId));
    }

    @Override
    public Optional<ContentPublicationRecord> findActiveByArticle(UUID tenantId, UUID articleId) {
        return publications.values().stream()
                .filter(publication -> publication.tenantId().equals(tenantId))
                .filter(publication -> publication.articleId().equals(articleId))
                .filter(publication -> publication.publicationStatus() == PublicationStatus.PUBLISHED)
                .max(Comparator.comparing(ContentPublicationRecord::updatedAt));
    }

    @Override
    public Optional<ContentPublicationRecord> findPendingReview(UUID tenantId, UUID articleId, int versionNo) {
        return publications.values().stream()
                .filter(publication -> publication.tenantId().equals(tenantId))
                .filter(publication -> publication.articleId().equals(articleId))
                .filter(publication -> publication.targetVersionNo() == versionNo)
                .filter(publication -> publication.reviewMode() == ReviewMode.REVIEW)
                .max(Comparator.comparing(ContentPublicationRecord::updatedAt));
    }

    @Override
    public List<ContentPublicationRecord> findByArticle(UUID tenantId, UUID articleId) {
        return publications.values().stream()
                .filter(publication -> publication.tenantId().equals(tenantId))
                .filter(publication -> publication.articleId().equals(articleId))
                .sorted(Comparator.comparing(ContentPublicationRecord::updatedAt).reversed())
                .toList();
    }

    @Override
    public void appendReview(ContentReviewRecord record) {
        reviews.add(record);
    }

    @Override
    public List<ContentReviewRecord> reviews(UUID tenantId, UUID articleId) {
        return reviews.stream()
                .filter(record -> record.tenantId().equals(tenantId))
                .filter(record -> record.articleId().equals(articleId))
                .sorted(Comparator.comparing(ContentReviewRecord::createdAt))
                .toList();
    }
}
