package com.hjo2oa.content.search.application;

import com.hjo2oa.content.permission.application.ContentPermissionApplicationService;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.ContentSubjectContext;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.PublicationScopeRuleRecord;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.ScopeEffect;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.ScopeSubjectType;
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
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentSearchApplicationService {

    private final ContentSearchIndexRepository repository;
    private final ContentPermissionApplicationService permissionService;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public ContentSearchApplicationService(
            ContentSearchIndexRepository repository,
            ContentPermissionApplicationService permissionService
    ) {
        this(repository, permissionService, event -> {
        }, Clock.systemUTC());
    }

    public ContentSearchApplicationService(
            ContentSearchIndexRepository repository,
            ContentPermissionApplicationService permissionService,
            DomainEventPublisher eventPublisher,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.permissionService = Objects.requireNonNull(permissionService, "permissionService must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public ContentSearchDocument index(ContentIndexCommand command) {
        validateTenant(command.tenantId());
        ContentSearchDocument document = new ContentSearchDocument(
                command.articleId(),
                command.publicationId(),
                command.tenantId(),
                command.categoryId(),
                requireText(command.title(), "title"),
                command.summary(),
                command.bodyText(),
                command.authorId(),
                command.authorName(),
                command.tags(),
                SearchDocumentStatus.PUBLISHED,
                command.publishedAt() == null ? clock.instant() : command.publishedAt(),
                clock.instant(),
                toVisibilityRules(permissionService.articleScopeRecords(command.tenantId(), command.articleId())),
                command.hotScore() == null ? BigDecimal.ZERO : command.hotScore()
        );
        repository.save(document);
        eventPublisher.publish(new ContentSearchIndexChangedEvent(
                UUID.randomUUID(),
                "content.search.indexed",
                clock.instant(),
                command.tenantId().toString(),
                command.articleId()
        ));
        return document;
    }

    @Transactional
    public void remove(UUID tenantId, UUID articleId) {
        validateTenant(tenantId);
        repository.remove(tenantId, articleId);
        eventPublisher.publish(new ContentSearchIndexChangedEvent(
                UUID.randomUUID(),
                "content.search.removed",
                clock.instant(),
                tenantId.toString(),
                articleId
        ));
    }

    @Transactional(readOnly = true)
    public ContentSearchPage search(ContentSearchCriteria criteria) {
        validateTenant(criteria.tenantId());
        int page = Math.max(1, criteria.page());
        int size = Math.max(1, Math.min(criteria.size(), 100));
        List<ContentSearchDocument> visible = repository.search(criteria).stream()
                .filter(document -> canRead(document, criteria.subject()))
                .sorted(defaultComparator(criteria.sort()))
                .toList();
        int from = Math.min((page - 1) * size, visible.size());
        int to = Math.min(from + size, visible.size());
        return new ContentSearchPage(
                visible.subList(from, to).stream().map(this::toItem).toList(),
                page,
                size,
                visible.size()
        );
    }

    @Transactional(readOnly = true)
    public PortalContentFeed latestForPortal(PortalContentQuery query) {
        ContentSearchPage page = search(new ContentSearchCriteria(
                query.tenantId(),
                query.categoryId(),
                SearchDocumentStatus.PUBLISHED,
                null,
                null,
                null,
                query.keyword(),
                1,
                Math.max(1, Math.min(query.limit(), 20)),
                query.subject(),
                "PUBLISHED_AT_DESC"
        ));
        List<PortalContentArticleSummary> latest = page.items().stream()
                .map(item -> new PortalContentArticleSummary(
                        item.articleId(),
                        item.publicationId(),
                        item.categoryId(),
                        item.title(),
                        item.summary(),
                        item.authorName(),
                        item.tags(),
                        item.publishedAt(),
                        item.hotScore()
                ))
                .toList();
        return new PortalContentFeed(latest, latest.size());
    }

    @Transactional(readOnly = true)
    public ContentSearchDocument requireDocument(UUID tenantId, UUID articleId) {
        validateTenant(tenantId);
        return repository.findByArticleId(tenantId, articleId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Search document not found"));
    }

    private ContentSearchItem toItem(ContentSearchDocument document) {
        return new ContentSearchItem(
                document.articleId(),
                document.publicationId(),
                document.categoryId(),
                document.title(),
                document.summary(),
                document.authorId(),
                document.authorName(),
                document.tags(),
                document.status(),
                document.publishedAt(),
                document.updatedAt(),
                document.hotScore()
        );
    }

    private boolean canRead(ContentSearchDocument document, ContentSubjectContext subject) {
        if (document.visibilityRules().isEmpty()) {
            return permissionService.canRead(document.tenantId(), document.articleId(), subject);
        }
        boolean deny = document.visibilityRules().stream()
                .anyMatch(rule -> rule.effect() == ScopeEffect.DENY && matches(rule, subject));
        if (deny) {
            return false;
        }
        boolean allow = document.visibilityRules().stream()
                .anyMatch(rule -> rule.effect() == ScopeEffect.ALLOW && matches(rule, subject));
        return allow || permissionService.canRead(document.tenantId(), document.articleId(), subject);
    }

    private static boolean matches(VisibilityRule rule, ContentSubjectContext subject) {
        if (rule.subjectType() == ScopeSubjectType.ALL) {
            return true;
        }
        if (subject == null || rule.subjectId() == null) {
            return false;
        }
        if (rule.subjectType() == ScopeSubjectType.PERSON) {
            return rule.subjectId().equals(subject.personId());
        }
        if (rule.subjectType() == ScopeSubjectType.ASSIGNMENT) {
            return rule.subjectId().equals(subject.assignmentId());
        }
        if (rule.subjectType() == ScopeSubjectType.POSITION) {
            return rule.subjectId().equals(subject.positionId());
        }
        if (rule.subjectType() == ScopeSubjectType.DEPARTMENT) {
            return rule.subjectId().equals(subject.departmentId());
        }
        if (rule.subjectType() == ScopeSubjectType.ROLE) {
            return subject.roleIds().contains(rule.subjectId());
        }
        return false;
    }

    private static Comparator<ContentSearchDocument> defaultComparator(String sort) {
        if ("HOT_DESC".equalsIgnoreCase(sort)) {
            return Comparator.comparing(ContentSearchDocument::hotScore).reversed()
                    .thenComparing(ContentSearchDocument::publishedAt, Comparator.reverseOrder());
        }
        return Comparator.comparing(ContentSearchDocument::publishedAt, Comparator.reverseOrder());
    }

    private static List<VisibilityRule> toVisibilityRules(List<PublicationScopeRuleRecord> records) {
        return records == null ? List.of() : records.stream()
                .map(rule -> new VisibilityRule(rule.subjectType(), rule.subjectId(), rule.effect(), rule.sortOrder()))
                .toList();
    }

    private static void validateTenant(UUID tenantId) {
        if (tenantId == null) {
            throw new BizException(SharedErrorDescriptors.TENANT_REQUIRED, "Tenant id is required");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BizException(SharedErrorDescriptors.VALIDATION_ERROR, fieldName + " is required");
        }
        return value.trim();
    }

    public enum SearchDocumentStatus {
        PUBLISHED,
        OFFLINE
    }

    public record VisibilityRule(
            ScopeSubjectType subjectType,
            UUID subjectId,
            ScopeEffect effect,
            int sortOrder
    ) {
    }

    public record ContentSearchDocument(
            UUID articleId,
            UUID publicationId,
            UUID tenantId,
            UUID categoryId,
            String title,
            String summary,
            String bodyText,
            UUID authorId,
            String authorName,
            List<String> tags,
            SearchDocumentStatus status,
            Instant publishedAt,
            Instant updatedAt,
            List<VisibilityRule> visibilityRules,
            BigDecimal hotScore
    ) {

        public ContentSearchDocument {
            tags = tags == null ? List.of() : List.copyOf(tags);
            visibilityRules = visibilityRules == null ? List.of() : List.copyOf(visibilityRules);
            hotScore = hotScore == null ? BigDecimal.ZERO : hotScore;
        }
    }

    public record ContentIndexCommand(
            UUID tenantId,
            UUID articleId,
            UUID publicationId,
            UUID categoryId,
            String title,
            String summary,
            String bodyText,
            UUID authorId,
            String authorName,
            List<String> tags,
            Instant publishedAt,
            BigDecimal hotScore
    ) {
    }

    public record ContentSearchCriteria(
            UUID tenantId,
            UUID categoryId,
            SearchDocumentStatus status,
            UUID authorId,
            Instant publishedFrom,
            Instant publishedTo,
            String keyword,
            int page,
            int size,
            ContentSubjectContext subject,
            String sort
    ) {
    }

    public record ContentSearchItem(
            UUID articleId,
            UUID publicationId,
            UUID categoryId,
            String title,
            String summary,
            UUID authorId,
            String authorName,
            List<String> tags,
            SearchDocumentStatus status,
            Instant publishedAt,
            Instant updatedAt,
            BigDecimal hotScore
    ) {

        public ContentSearchItem {
            tags = tags == null ? List.of() : List.copyOf(tags);
        }
    }

    public record ContentSearchPage(
            List<ContentSearchItem> items,
            int page,
            int size,
            long total
    ) {

        public ContentSearchPage {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record PortalContentQuery(
            UUID tenantId,
            UUID categoryId,
            String keyword,
            int limit,
            ContentSubjectContext subject
    ) {
    }

    public record PortalContentFeed(
            List<PortalContentArticleSummary> latestArticles,
            long total
    ) {

        public PortalContentFeed {
            latestArticles = latestArticles == null ? List.of() : List.copyOf(latestArticles);
        }

        public static PortalContentFeed empty() {
            return new PortalContentFeed(List.of(), 0);
        }
    }

    public record PortalContentArticleSummary(
            UUID articleId,
            UUID publicationId,
            UUID categoryId,
            String title,
            String summary,
            String authorName,
            List<String> tags,
            Instant publishedAt,
            BigDecimal hotScore
    ) {

        public PortalContentArticleSummary {
            tags = tags == null ? List.of() : List.copyOf(tags);
        }
    }

    public record ContentSearchIndexChangedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID articleId
    ) implements DomainEvent {
    }
}
