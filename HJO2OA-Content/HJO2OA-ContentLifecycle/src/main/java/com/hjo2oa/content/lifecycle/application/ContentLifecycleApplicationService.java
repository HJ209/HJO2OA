package com.hjo2oa.content.lifecycle.application;

import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.CategoryRecord;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.CategoryStatus;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.PublicationScopeRuleInput;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.ReplacePublicationScopeCommand;
import com.hjo2oa.content.search.application.ContentSearchApplicationService;
import com.hjo2oa.content.search.application.ContentSearchApplicationService.ContentIndexCommand;
import com.hjo2oa.content.statistics.application.ContentStatisticsApplicationService;
import com.hjo2oa.content.storage.application.ContentStorageApplicationService;
import com.hjo2oa.content.storage.application.ContentStorageApplicationService.ContentAttachment;
import com.hjo2oa.content.storage.application.ContentStorageApplicationService.ContentVersionRecord;
import com.hjo2oa.content.storage.application.ContentStorageApplicationService.ContentVersionView;
import com.hjo2oa.content.storage.application.ContentStorageApplicationService.RollbackVersionCommand;
import com.hjo2oa.content.storage.application.ContentStorageApplicationService.SaveDraftCommand;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEvent;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentLifecycleApplicationService {

    private final ContentArticleRepository articleRepository;
    private final ContentPublicationRepository publicationRepository;
    private final ContentCategoryApplicationService categoryService;
    private final ContentStorageApplicationService storageService;
    private final ContentPermissionApplicationService permissionService;
    private final ContentSearchApplicationService searchService;
    private final ContentStatisticsApplicationService statisticsService;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public ContentLifecycleApplicationService(
            ContentArticleRepository articleRepository,
            ContentPublicationRepository publicationRepository,
            ContentCategoryApplicationService categoryService,
            ContentStorageApplicationService storageService,
            ContentPermissionApplicationService permissionService,
            ContentSearchApplicationService searchService,
            ContentStatisticsApplicationService statisticsService
    ) {
        this(
                articleRepository,
                publicationRepository,
                categoryService,
                storageService,
                permissionService,
                searchService,
                statisticsService,
                event -> {
                },
                Clock.systemUTC()
        );
    }

    public ContentLifecycleApplicationService(
            ContentArticleRepository articleRepository,
            ContentPublicationRepository publicationRepository,
            ContentCategoryApplicationService categoryService,
            ContentStorageApplicationService storageService,
            ContentPermissionApplicationService permissionService,
            ContentSearchApplicationService searchService,
            ContentStatisticsApplicationService statisticsService,
            DomainEventPublisher eventPublisher,
            Clock clock
    ) {
        this.articleRepository = Objects.requireNonNull(articleRepository, "articleRepository must not be null");
        this.publicationRepository = Objects.requireNonNull(publicationRepository, "publicationRepository must not be null");
        this.categoryService = Objects.requireNonNull(categoryService, "categoryService must not be null");
        this.storageService = Objects.requireNonNull(storageService, "storageService must not be null");
        this.permissionService = Objects.requireNonNull(permissionService, "permissionService must not be null");
        this.searchService = Objects.requireNonNull(searchService, "searchService must not be null");
        this.statisticsService = Objects.requireNonNull(statisticsService, "statisticsService must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public ArticleDetailView create(CreateArticleCommand command) {
        validateTenant(command.tenantId());
        assertCategoryWritable(command.tenantId(), command.mainCategoryId(), command.operatorId());
        Optional<ContentArticleRecord> sameArticleNo = articleRepository.findByArticleNo(command.tenantId(), command.articleNo());
        if (sameArticleNo.isPresent() && hasKey(command.idempotencyKey())) {
            Optional<ContentVersionView> existingVersion = storageService.findByIdempotencyKey(
                    command.tenantId(),
                    sameArticleNo.get().id(),
                    command.idempotencyKey()
            );
            if (existingVersion.isPresent()) {
                return detail(sameArticleNo.get(), existingVersion.get());
            }
        }
        if (sameArticleNo.isPresent()) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Article number already exists");
        }
        Instant now = clock.instant();
        ContentArticleRecord article = new ContentArticleRecord(
                UUID.randomUUID(),
                requireText(command.articleNo(), "articleNo"),
                requireText(command.title(), "title"),
                command.summary(),
                defaultText(command.contentType(), "ARTICLE"),
                command.mainCategoryId(),
                command.authorId() == null ? command.operatorId() : command.authorId(),
                command.authorName(),
                defaultText(command.sourceType(), "ORIGINAL"),
                command.sourceUrl(),
                null,
                null,
                ArticleStatus.DRAFT,
                command.tenantId(),
                command.operatorId(),
                command.operatorId(),
                now,
                now
        );
        articleRepository.save(article);
        ContentVersionView version = storageService.createDraft(new SaveDraftCommand(
                command.tenantId(),
                article.id(),
                command.title(),
                command.summary(),
                command.bodyFormat(),
                command.bodyText(),
                command.coverAttachmentId(),
                command.attachments(),
                command.tags(),
                command.operatorId(),
                null,
                command.idempotencyKey()
        ));
        article = article.withDraftVersion(version.versionNo(), command.title(), command.summary(), command.operatorId(), now);
        articleRepository.save(article);
        publishArticleEvent("content.article.created", article, command.operatorId(), null);
        return detail(article, version);
    }

    @Transactional
    public ArticleDetailView updateDraft(UUID articleId, UpdateArticleCommand command) {
        validateTenant(command.tenantId());
        ContentArticleRecord existing = requireArticle(command.tenantId(), articleId);
        assertCategoryWritable(command.tenantId(), command.mainCategoryId(), command.operatorId());
        Optional<ContentVersionView> existingVersion = storageService.findByIdempotencyKey(
                command.tenantId(),
                articleId,
                command.idempotencyKey()
        );
        if (existingVersion.isPresent() && Objects.equals(existing.currentDraftVersionNo(), existingVersion.get().versionNo())) {
            return detail(existing, existingVersion.get());
        }
        ContentVersionView version = storageService.createDraft(new SaveDraftCommand(
                command.tenantId(),
                articleId,
                command.title(),
                command.summary(),
                command.bodyFormat(),
                command.bodyText(),
                command.coverAttachmentId(),
                command.attachments(),
                command.tags(),
                command.operatorId(),
                existing.currentDraftVersionNo(),
                command.idempotencyKey()
        ));
        ContentArticleRecord updated = existing.withMetadata(
                requireText(command.title(), "title"),
                command.summary(),
                command.mainCategoryId(),
                command.authorId() == null ? existing.authorId() : command.authorId(),
                command.authorName(),
                defaultText(command.sourceType(), existing.sourceType()),
                command.sourceUrl(),
                version.versionNo(),
                ArticleStatus.DRAFT,
                command.operatorId(),
                clock.instant()
        );
        articleRepository.save(updated);
        publishArticleEvent("content.article.updated", updated, command.operatorId(), null);
        return detail(updated, version);
    }

    @Transactional
    public ArticleDetailView submit(UUID articleId, ReviewCommand command) {
        ContentArticleRecord article = requireArticle(command.tenantId(), articleId);
        int versionNo = article.currentDraftVersionNoOrThrow();
        Optional<ContentPublicationRecord> existingPublication = publicationRepository.findPendingReview(command.tenantId(), articleId, versionNo);
        if (existingPublication.isPresent()
                && article.status() == ArticleStatus.REVIEWING
                && hasReview(command.tenantId(), articleId, existingPublication.get().id(), ReviewAction.SUBMIT)) {
            return detail(article, storageService.get(command.tenantId(), articleId, versionNo));
        }
        ContentPublicationRecord publication = existingPublication
                .orElseGet(() -> ContentPublicationRecord.pendingReview(
                        UUID.randomUUID(),
                        articleId,
                        command.tenantId(),
                        versionNo,
                        clock.instant()
                ));
        publicationRepository.save(publication);
        publicationRepository.appendReview(ContentReviewRecord.create(
                publication.id(),
                articleId,
                command.tenantId(),
                ReviewAction.SUBMIT,
                command.operatorId(),
                command.opinion(),
                clock.instant()
        ));
        ContentArticleRecord reviewing = article.withStatus(ArticleStatus.REVIEWING, command.operatorId(), clock.instant());
        articleRepository.save(reviewing);
        publishArticleEvent("content.article.review.submitted", reviewing, command.operatorId(), publication.id());
        return detail(reviewing, storageService.get(command.tenantId(), articleId, versionNo));
    }

    @Transactional
    public ArticleDetailView approve(UUID articleId, ReviewCommand command) {
        ContentArticleRecord article = requireArticle(command.tenantId(), articleId);
        int versionNo = article.currentDraftVersionNoOrThrow();
        Optional<ContentPublicationRecord> reviewPublication = findReviewPublication(command.tenantId(), articleId, versionNo);
        if (reviewPublication.isPresent()
                && reviewPublication.get().reviewStatus() == ReviewStatus.APPROVED
                && hasReview(command.tenantId(), articleId, reviewPublication.get().id(), ReviewAction.APPROVE)) {
            return detail(article, storageService.get(command.tenantId(), articleId, versionNo));
        }
        ContentPublicationRecord publication = publicationRepository.findPendingReview(command.tenantId(), articleId, versionNo)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "No pending review publication"));
        ContentPublicationRecord approved = publication.withReviewStatus(ReviewStatus.APPROVED, command.operatorId(), clock.instant());
        publicationRepository.save(approved);
        publicationRepository.appendReview(ContentReviewRecord.create(
                approved.id(),
                articleId,
                command.tenantId(),
                ReviewAction.APPROVE,
                command.operatorId(),
                command.opinion(),
                clock.instant()
        ));
        publishArticleEvent("content.article.review.approved", article, command.operatorId(), approved.id());
        return detail(article, storageService.get(command.tenantId(), articleId, versionNo));
    }

    @Transactional
    public ArticleDetailView reject(UUID articleId, ReviewCommand command) {
        ContentArticleRecord article = requireArticle(command.tenantId(), articleId);
        int versionNo = article.currentDraftVersionNoOrThrow();
        Optional<ContentPublicationRecord> reviewPublication = findReviewPublication(command.tenantId(), articleId, versionNo);
        if (reviewPublication.isPresent()
                && reviewPublication.get().reviewStatus() == ReviewStatus.REJECTED
                && hasReview(command.tenantId(), articleId, reviewPublication.get().id(), ReviewAction.REJECT)) {
            return detail(article, storageService.get(command.tenantId(), articleId, versionNo));
        }
        ContentPublicationRecord publication = publicationRepository.findPendingReview(command.tenantId(), articleId, versionNo)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "No pending review publication"));
        ContentPublicationRecord rejected = publication.withReviewStatus(ReviewStatus.REJECTED, command.operatorId(), clock.instant());
        publicationRepository.save(rejected);
        publicationRepository.appendReview(ContentReviewRecord.create(
                rejected.id(),
                articleId,
                command.tenantId(),
                ReviewAction.REJECT,
                command.operatorId(),
                command.opinion(),
                clock.instant()
        ));
        ContentArticleRecord updated = article.withStatus(ArticleStatus.DRAFT, command.operatorId(), clock.instant());
        articleRepository.save(updated);
        publishArticleEvent("content.article.review.rejected", updated, command.operatorId(), rejected.id());
        return detail(updated, storageService.get(command.tenantId(), articleId, versionNo));
    }

    @Transactional
    public ArticleDetailView publish(UUID articleId, PublishArticleCommand command) {
        ContentArticleRecord article = requireArticle(command.tenantId(), articleId);
        assertCategoryWritable(command.tenantId(), article.mainCategoryId(), command.operatorId());
        int versionNo = command.versionNo() == null ? article.currentDraftVersionNoOrThrow() : command.versionNo();
        if (article.status() == ArticleStatus.PUBLISHED && Objects.equals(article.currentPublishedVersionNo(), versionNo)) {
            Optional<ContentPublicationRecord> active = publicationRepository.findActiveByArticle(command.tenantId(), articleId);
            if (active.isPresent()) {
                return detail(article, storageService.get(command.tenantId(), articleId, versionNo));
            }
        }
        ContentVersionRecord version = storageService.requireVersion(command.tenantId(), articleId, versionNo);
        validatePublication(version, command.startAt(), command.endAt());
        ContentPublicationRecord publication = resolvePublicationForPublish(article, command, versionNo);
        ContentPublicationRecord published = publication.publish(command.startAt(), command.endAt(), command.operatorId(), command.reason(), clock.instant());
        publicationRepository.save(published);
        permissionService.replaceScopes(new ReplacePublicationScopeCommand(
                command.tenantId(),
                published.id(),
                articleId,
                command.operatorId(),
                command.scopes(),
                command.idempotencyKey()
        ));
        storageService.markPublished(command.tenantId(), articleId, versionNo, command.operatorId());
        ContentArticleRecord updated = article.withPublishedVersion(versionNo, command.operatorId(), clock.instant());
        articleRepository.save(updated);
        BigDecimal hotScore = statisticsService.snapshot(command.tenantId(), articleId, ContentStatisticsApplicationService.DEFAULT_BUCKET)
                .hotScore();
        searchService.index(new ContentIndexCommand(
                command.tenantId(),
                articleId,
                published.id(),
                updated.mainCategoryId(),
                version.title(),
                version.summary(),
                version.bodyText(),
                updated.authorId(),
                updated.authorName(),
                version.tags(),
                published.publishedAt(),
                hotScore
        ));
        publishArticleEvent("content.article.published", updated, command.operatorId(), published.id());
        return detail(updated, storageService.get(command.tenantId(), articleId, versionNo));
    }

    @Transactional
    public ArticleDetailView unpublish(UUID articleId, OfflineArticleCommand command) {
        ContentArticleRecord article = requireArticle(command.tenantId(), articleId);
        Optional<ContentPublicationRecord> activePublication = publicationRepository.findActiveByArticle(command.tenantId(), articleId);
        if (activePublication.isEmpty() && article.status() == ArticleStatus.OFFLINE) {
            return detailCurrentVersion(command.tenantId(), article);
        }
        ContentPublicationRecord publication = activePublication
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Article is not published"));
        ContentPublicationRecord offline = publication.offline(command.operatorId(), command.reason(), clock.instant());
        publicationRepository.save(offline);
        searchService.remove(command.tenantId(), articleId);
        ContentArticleRecord updated = article.withStatus(ArticleStatus.OFFLINE, command.operatorId(), clock.instant());
        articleRepository.save(updated);
        publishArticleEvent("content.article.unpublished", updated, command.operatorId(), publication.id());
        int versionNo = updated.currentPublishedVersionNo() == null
                ? updated.currentDraftVersionNoOrThrow()
                : updated.currentPublishedVersionNo();
        return detail(updated, storageService.get(command.tenantId(), articleId, versionNo));
    }

    @Transactional
    public ArticleDetailView archive(UUID articleId, OfflineArticleCommand command) {
        ContentArticleRecord article = requireArticle(command.tenantId(), articleId);
        if (article.status() == ArticleStatus.ARCHIVED) {
            return detailCurrentVersion(command.tenantId(), article);
        }
        publicationRepository.findActiveByArticle(command.tenantId(), articleId)
                .ifPresent(publication -> publicationRepository.save(publication.archive(command.operatorId(), command.reason(), clock.instant())));
        searchService.remove(command.tenantId(), articleId);
        ContentArticleRecord updated = article.withStatus(ArticleStatus.ARCHIVED, command.operatorId(), clock.instant());
        articleRepository.save(updated);
        publishArticleEvent("content.article.archived", updated, command.operatorId(), null);
        return detail(updated, storageService.requireLatest(command.tenantId(), articleId));
    }

    @Transactional
    public ArticleDetailView rollback(UUID articleId, RollbackArticleCommand command) {
        ContentArticleRecord article = requireArticle(command.tenantId(), articleId);
        Optional<ContentVersionView> existingVersion = storageService.findByIdempotencyKey(
                command.tenantId(),
                articleId,
                command.idempotencyKey()
        );
        if (existingVersion.isPresent() && Objects.equals(article.currentDraftVersionNo(), existingVersion.get().versionNo())) {
            return detail(article, existingVersion.get());
        }
        ContentVersionView restored = storageService.rollback(new RollbackVersionCommand(
                command.tenantId(),
                articleId,
                command.targetVersionNo(),
                command.operatorId(),
                command.idempotencyKey()
        ));
        ContentArticleRecord updated = article.withDraftVersion(
                restored.versionNo(),
                restored.title(),
                restored.summary(),
                command.operatorId(),
                clock.instant()
        );
        articleRepository.save(updated);
        publishArticleEvent("content.article.rollback", updated, command.operatorId(), null);
        return detail(updated, restored);
    }

    @Transactional(readOnly = true)
    public ArticlePage list(ArticleListQuery query) {
        validateTenant(query.tenantId());
        int page = Math.max(1, query.page());
        int size = Math.max(1, Math.min(query.size(), 100));
        List<ArticleSummaryView> matches = articleRepository.search(query).stream()
                .sorted(Comparator.comparing(ContentArticleRecord::updatedAt).reversed())
                .map(this::summary)
                .toList();
        int from = Math.min((page - 1) * size, matches.size());
        int to = Math.min(from + size, matches.size());
        return new ArticlePage(matches.subList(from, to), page, size, matches.size());
    }

    @Transactional(readOnly = true)
    public ArticleDetailView get(UUID tenantId, UUID articleId) {
        ContentArticleRecord article = requireArticle(tenantId, articleId);
        ContentVersionRecord version = article.currentDraftVersionNo() == null
                ? storageService.requireLatest(tenantId, articleId)
                : storageService.requireVersion(tenantId, articleId, article.currentDraftVersionNo());
        return detail(article, version);
    }

    @Transactional(readOnly = true)
    public List<ContentVersionView> history(UUID tenantId, UUID articleId) {
        requireArticle(tenantId, articleId);
        return storageService.versions(tenantId, articleId);
    }

    @Transactional(readOnly = true)
    public List<ContentPublicationRecord> publications(UUID tenantId, UUID articleId) {
        requireArticle(tenantId, articleId);
        return publicationRepository.findByArticle(tenantId, articleId);
    }

    @Transactional(readOnly = true)
    public List<ContentReviewRecord> reviews(UUID tenantId, UUID articleId) {
        requireArticle(tenantId, articleId);
        return publicationRepository.reviews(tenantId, articleId);
    }

    private ArticleDetailView detailCurrentVersion(UUID tenantId, ContentArticleRecord article) {
        int versionNo = article.currentPublishedVersionNo() == null
                ? article.currentDraftVersionNoOrThrow()
                : article.currentPublishedVersionNo();
        return detail(article, storageService.get(tenantId, article.id(), versionNo));
    }

    private Optional<ContentPublicationRecord> findReviewPublication(UUID tenantId, UUID articleId, int versionNo) {
        return publicationRepository.findByArticle(tenantId, articleId).stream()
                .filter(publication -> publication.targetVersionNo() == versionNo)
                .filter(publication -> publication.reviewMode() == ReviewMode.REVIEW)
                .findFirst();
    }

    private boolean hasReview(UUID tenantId, UUID articleId, UUID publicationId, ReviewAction action) {
        return publicationRepository.reviews(tenantId, articleId).stream()
                .anyMatch(record -> record.publicationId().equals(publicationId) && record.action() == action);
    }

    private ContentPublicationRecord resolvePublicationForPublish(
            ContentArticleRecord article,
            PublishArticleCommand command,
            int versionNo
    ) {
        if (command.reviewMode() == ReviewMode.REVIEW) {
            ContentPublicationRecord publication = publicationRepository.findPendingReview(command.tenantId(), article.id(), versionNo)
                    .orElseThrow(() -> new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Review approval is required"));
            if (publication.reviewStatus() != ReviewStatus.APPROVED) {
                throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Review approval is required");
            }
            return publication;
        }
        return new ContentPublicationRecord(
                UUID.randomUUID(),
                article.id(),
                command.tenantId(),
                versionNo,
                ReviewMode.DIRECT,
                ReviewStatus.NOT_REQUIRED,
                null,
                PublicationStatus.DRAFT,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                command.reason(),
                clock.instant(),
                clock.instant()
        );
    }

    private void validatePublication(ContentVersionRecord version, Instant startAt, Instant endAt) {
        requireText(version.title(), "title");
        requireText(version.bodyText(), "bodyText");
        if (endAt != null && startAt != null && !endAt.isAfter(startAt)) {
            throw new BizException(SharedErrorDescriptors.VALIDATION_ERROR, "Publish end time must be after start time");
        }
    }

    private ContentArticleRecord requireArticle(UUID tenantId, UUID articleId) {
        validateTenant(tenantId);
        return articleRepository.findById(tenantId, articleId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Article not found"));
    }

    private void assertCategoryWritable(UUID tenantId, UUID categoryId, UUID operatorId) {
        CategoryRecord category = categoryService.findRecord(tenantId, categoryId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Category not found"));
        if (category.status() != CategoryStatus.ENABLED) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Category is disabled");
        }
        if (!categoryService.canManage(tenantId, categoryId, operatorId, java.util.Set.of())) {
            throw new BizException(SharedErrorDescriptors.FORBIDDEN, "No permission to manage category");
        }
    }

    private ArticleDetailView detail(ContentArticleRecord article, ContentVersionRecord version) {
        return detail(article, new ContentVersionView(
                version.id(),
                version.articleId(),
                version.tenantId(),
                version.versionNo(),
                version.title(),
                version.summary(),
                version.bodyFormat(),
                version.bodyText(),
                version.bodyChecksum(),
                version.coverAttachmentId(),
                version.attachments(),
                version.tags(),
                version.editorId(),
                version.status(),
                version.sourceVersionNo(),
                version.idempotencyKey(),
                version.createdAt(),
                version.updatedAt()
        ));
    }

    private ArticleDetailView detail(ContentArticleRecord article, ContentVersionView version) {
        return new ArticleDetailView(summary(article), version, publicationRepository.findByArticle(article.tenantId(), article.id()));
    }

    private ArticleSummaryView summary(ContentArticleRecord article) {
        return new ArticleSummaryView(
                article.id(),
                article.articleNo(),
                article.title(),
                article.summary(),
                article.contentType(),
                article.mainCategoryId(),
                article.authorId(),
                article.authorName(),
                article.status(),
                article.currentDraftVersionNo(),
                article.currentPublishedVersionNo(),
                article.createdAt(),
                article.updatedAt()
        );
    }

    private void publishArticleEvent(String eventType, ContentArticleRecord article, UUID operatorId, UUID publicationId) {
        eventPublisher.publish(new ContentArticleLifecycleEvent(
                UUID.randomUUID(),
                eventType,
                clock.instant(),
                article.tenantId().toString(),
                article.id(),
                publicationId,
                article.currentDraftVersionNo(),
                article.currentPublishedVersionNo(),
                operatorId,
                article.status()
        ));
    }

    private static void validateTenant(UUID tenantId) {
        if (tenantId == null) {
            throw new BizException(SharedErrorDescriptors.TENANT_REQUIRED, "Tenant id is required");
        }
    }

    private static boolean hasKey(String idempotencyKey) {
        return idempotencyKey != null && !idempotencyKey.isBlank();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BizException(SharedErrorDescriptors.VALIDATION_ERROR, fieldName + " is required");
        }
        return value.trim();
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public enum ArticleStatus {
        DRAFT,
        REVIEWING,
        PUBLISHED,
        OFFLINE,
        ARCHIVED
    }

    public enum ReviewMode {
        DIRECT,
        REVIEW
    }

    public enum ReviewStatus {
        NOT_REQUIRED,
        PENDING,
        APPROVED,
        REJECTED
    }

    public enum PublicationStatus {
        DRAFT,
        PENDING_REVIEW,
        PUBLISHED,
        OFFLINE,
        ARCHIVED,
        REJECTED
    }

    public enum ReviewAction {
        SUBMIT,
        APPROVE,
        REJECT
    }

    public record ContentArticleRecord(
            UUID id,
            String articleNo,
            String title,
            String summary,
            String contentType,
            UUID mainCategoryId,
            UUID authorId,
            String authorName,
            String sourceType,
            String sourceUrl,
            Integer currentDraftVersionNo,
            Integer currentPublishedVersionNo,
            ArticleStatus status,
            UUID tenantId,
            UUID createdBy,
            UUID updatedBy,
            Instant createdAt,
            Instant updatedAt
    ) {

        ContentArticleRecord withDraftVersion(int versionNo, String newTitle, String newSummary, UUID operatorId, Instant now) {
            return withMetadata(
                    newTitle,
                    newSummary,
                    mainCategoryId,
                    authorId,
                    authorName,
                    sourceType,
                    sourceUrl,
                    versionNo,
                    ArticleStatus.DRAFT,
                    operatorId,
                    now
            );
        }

        ContentArticleRecord withMetadata(
                String newTitle,
                String newSummary,
                UUID newCategoryId,
                UUID newAuthorId,
                String newAuthorName,
                String newSourceType,
                String newSourceUrl,
                Integer draftVersionNo,
                ArticleStatus newStatus,
                UUID operatorId,
                Instant now
        ) {
            return new ContentArticleRecord(
                    id,
                    articleNo,
                    newTitle,
                    newSummary,
                    contentType,
                    newCategoryId,
                    newAuthorId,
                    newAuthorName,
                    newSourceType,
                    newSourceUrl,
                    draftVersionNo,
                    currentPublishedVersionNo,
                    newStatus,
                    tenantId,
                    createdBy,
                    operatorId,
                    createdAt,
                    now
            );
        }

        ContentArticleRecord withStatus(ArticleStatus newStatus, UUID operatorId, Instant now) {
            return new ContentArticleRecord(
                    id,
                    articleNo,
                    title,
                    summary,
                    contentType,
                    mainCategoryId,
                    authorId,
                    authorName,
                    sourceType,
                    sourceUrl,
                    currentDraftVersionNo,
                    currentPublishedVersionNo,
                    newStatus,
                    tenantId,
                    createdBy,
                    operatorId,
                    createdAt,
                    now
            );
        }

        ContentArticleRecord withPublishedVersion(int versionNo, UUID operatorId, Instant now) {
            return new ContentArticleRecord(
                    id,
                    articleNo,
                    title,
                    summary,
                    contentType,
                    mainCategoryId,
                    authorId,
                    authorName,
                    sourceType,
                    sourceUrl,
                    currentDraftVersionNo,
                    versionNo,
                    ArticleStatus.PUBLISHED,
                    tenantId,
                    createdBy,
                    operatorId,
                    createdAt,
                    now
            );
        }

        int currentDraftVersionNoOrThrow() {
            if (currentDraftVersionNo == null) {
                throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Draft version is required");
            }
            return currentDraftVersionNo;
        }
    }

    public record ContentPublicationRecord(
            UUID id,
            UUID articleId,
            UUID tenantId,
            int targetVersionNo,
            ReviewMode reviewMode,
            ReviewStatus reviewStatus,
            UUID workflowInstanceId,
            PublicationStatus publicationStatus,
            Instant startAt,
            Instant endAt,
            Instant publishedAt,
            UUID publishedBy,
            Instant offlineAt,
            UUID offlineBy,
            Instant archiveAt,
            UUID archivedBy,
            String reason,
            Instant createdAt,
            Instant updatedAt
    ) {

        static ContentPublicationRecord pendingReview(UUID id, UUID articleId, UUID tenantId, int versionNo, Instant now) {
            return new ContentPublicationRecord(
                    id,
                    articleId,
                    tenantId,
                    versionNo,
                    ReviewMode.REVIEW,
                    ReviewStatus.PENDING,
                    null,
                    PublicationStatus.PENDING_REVIEW,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    now,
                    now
            );
        }

        ContentPublicationRecord withReviewStatus(ReviewStatus status, UUID operatorId, Instant now) {
            return new ContentPublicationRecord(
                    id,
                    articleId,
                    tenantId,
                    targetVersionNo,
                    reviewMode,
                    status,
                    workflowInstanceId,
                    status == ReviewStatus.REJECTED ? PublicationStatus.REJECTED : publicationStatus,
                    startAt,
                    endAt,
                    publishedAt,
                    publishedBy,
                    offlineAt,
                    offlineBy,
                    archiveAt,
                    archivedBy,
                    reason,
                    createdAt,
                    now
            );
        }

        ContentPublicationRecord publish(Instant newStartAt, Instant newEndAt, UUID operatorId, String newReason, Instant now) {
            return new ContentPublicationRecord(
                    id,
                    articleId,
                    tenantId,
                    targetVersionNo,
                    reviewMode,
                    reviewStatus,
                    workflowInstanceId,
                    PublicationStatus.PUBLISHED,
                    newStartAt,
                    newEndAt,
                    now,
                    operatorId,
                    null,
                    null,
                    null,
                    null,
                    newReason,
                    createdAt,
                    now
            );
        }

        ContentPublicationRecord offline(UUID operatorId, String newReason, Instant now) {
            return new ContentPublicationRecord(
                    id,
                    articleId,
                    tenantId,
                    targetVersionNo,
                    reviewMode,
                    reviewStatus,
                    workflowInstanceId,
                    PublicationStatus.OFFLINE,
                    startAt,
                    endAt,
                    publishedAt,
                    publishedBy,
                    now,
                    operatorId,
                    archiveAt,
                    archivedBy,
                    newReason,
                    createdAt,
                    now
            );
        }

        ContentPublicationRecord archive(UUID operatorId, String newReason, Instant now) {
            return new ContentPublicationRecord(
                    id,
                    articleId,
                    tenantId,
                    targetVersionNo,
                    reviewMode,
                    reviewStatus,
                    workflowInstanceId,
                    PublicationStatus.ARCHIVED,
                    startAt,
                    endAt,
                    publishedAt,
                    publishedBy,
                    offlineAt,
                    offlineBy,
                    now,
                    operatorId,
                    newReason,
                    createdAt,
                    now
            );
        }
    }

    public record ContentReviewRecord(
            UUID id,
            UUID publicationId,
            UUID articleId,
            UUID tenantId,
            ReviewAction action,
            UUID operatorId,
            String opinion,
            Instant createdAt
    ) {

        static ContentReviewRecord create(
                UUID publicationId,
                UUID articleId,
                UUID tenantId,
                ReviewAction action,
                UUID operatorId,
                String opinion,
                Instant now
        ) {
            return new ContentReviewRecord(UUID.randomUUID(), publicationId, articleId, tenantId, action, operatorId, opinion, now);
        }
    }

    public record CreateArticleCommand(
            UUID tenantId,
            UUID operatorId,
            String articleNo,
            String title,
            String summary,
            String bodyFormat,
            String bodyText,
            UUID coverAttachmentId,
            List<ContentAttachment> attachments,
            List<String> tags,
            String contentType,
            UUID mainCategoryId,
            UUID authorId,
            String authorName,
            String sourceType,
            String sourceUrl,
            String idempotencyKey
    ) {
    }

    public record UpdateArticleCommand(
            UUID tenantId,
            UUID operatorId,
            String title,
            String summary,
            String bodyFormat,
            String bodyText,
            UUID coverAttachmentId,
            List<ContentAttachment> attachments,
            List<String> tags,
            UUID mainCategoryId,
            UUID authorId,
            String authorName,
            String sourceType,
            String sourceUrl,
            String idempotencyKey
    ) {
    }

    public record ReviewCommand(UUID tenantId, UUID operatorId, String opinion, String idempotencyKey) {
    }

    public record PublishArticleCommand(
            UUID tenantId,
            UUID operatorId,
            Integer versionNo,
            ReviewMode reviewMode,
            Instant startAt,
            Instant endAt,
            String reason,
            List<PublicationScopeRuleInput> scopes,
            String idempotencyKey
    ) {

        public PublishArticleCommand {
            reviewMode = reviewMode == null ? ReviewMode.DIRECT : reviewMode;
        }
    }

    public record OfflineArticleCommand(UUID tenantId, UUID operatorId, String reason, String idempotencyKey) {
    }

    public record RollbackArticleCommand(
            UUID tenantId,
            UUID operatorId,
            int targetVersionNo,
            String idempotencyKey
    ) {
    }

    public record ArticleListQuery(
            UUID tenantId,
            UUID categoryId,
            ArticleStatus status,
            UUID authorId,
            Instant from,
            Instant to,
            String keyword,
            int page,
            int size
    ) {
    }

    public record ArticleSummaryView(
            UUID id,
            String articleNo,
            String title,
            String summary,
            String contentType,
            UUID mainCategoryId,
            UUID authorId,
            String authorName,
            ArticleStatus status,
            Integer currentDraftVersionNo,
            Integer currentPublishedVersionNo,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ArticleDetailView(
            ArticleSummaryView article,
            ContentVersionView currentVersion,
            List<ContentPublicationRecord> publications
    ) {

        public ArticleDetailView {
            publications = publications == null ? List.of() : List.copyOf(publications);
        }
    }

    public record ArticlePage(List<ArticleSummaryView> items, int page, int size, long total) {

        public ArticlePage {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record ContentArticleLifecycleEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID articleId,
            UUID publicationId,
            Integer draftVersionNo,
            Integer publishedVersionNo,
            UUID operatorId,
            ArticleStatus status
    ) implements DomainEvent {
    }
}
