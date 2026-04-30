package com.hjo2oa.content.lifecycle.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.CategoryView;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.CreateCategoryCommand;
import com.hjo2oa.content.category.management.infrastructure.InMemoryContentCategoryRepository;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ArticleDetailView;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ArticleStatus;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.CreateArticleCommand;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.OfflineArticleCommand;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.PublishArticleCommand;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ReviewCommand;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ReviewMode;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.RollbackArticleCommand;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.UpdateArticleCommand;
import com.hjo2oa.content.lifecycle.infrastructure.InMemoryContentArticleRepository;
import com.hjo2oa.content.lifecycle.infrastructure.InMemoryContentPublicationRepository;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.ContentSubjectContext;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.PublicationScopeRuleInput;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.ScopeEffect;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.ScopeSubjectType;
import com.hjo2oa.content.permission.infrastructure.InMemoryPublicationScopeRepository;
import com.hjo2oa.content.search.application.ContentSearchApplicationService;
import com.hjo2oa.content.search.application.ContentSearchApplicationService.ContentSearchCriteria;
import com.hjo2oa.content.search.application.ContentSearchApplicationService.SearchDocumentStatus;
import com.hjo2oa.content.search.infrastructure.InMemoryContentSearchIndexRepository;
import com.hjo2oa.content.statistics.application.ContentStatisticsApplicationService;
import com.hjo2oa.content.statistics.infrastructure.InMemoryContentStatisticsRepository;
import com.hjo2oa.content.storage.application.ContentStorageApplicationService;
import com.hjo2oa.content.storage.infrastructure.InMemoryContentVersionRepository;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContentLifecycleApplicationServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OPERATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID READER_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID OTHER_READER_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private static final Instant NOW = Instant.parse("2026-04-20T10:00:00Z");

    private List<DomainEvent> events;
    private ContentLifecycleApplicationService lifecycleService;
    private ContentSearchApplicationService searchService;
    private CategoryView category;

    @BeforeEach
    void setUp() {
        events = new ArrayList<>();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        ContentCategoryApplicationService categoryService = new ContentCategoryApplicationService(
                new InMemoryContentCategoryRepository(),
                events::add,
                clock
        );
        category = categoryService.create(new CreateCategoryCommand(
                TENANT_ID,
                OPERATOR_ID,
                "news",
                "News",
                "GENERAL",
                null,
                "/news",
                1,
                "INHERIT",
                null,
                null
        ));
        ContentPermissionApplicationService permissionService = new ContentPermissionApplicationService(
                new InMemoryPublicationScopeRepository(),
                events::add,
                clock
        );
        searchService = new ContentSearchApplicationService(
                new InMemoryContentSearchIndexRepository(),
                permissionService,
                events::add,
                clock
        );
        ContentStatisticsApplicationService statisticsService = new ContentStatisticsApplicationService(
                new InMemoryContentStatisticsRepository(),
                events::add,
                clock
        );
        lifecycleService = new ContentLifecycleApplicationService(
                new InMemoryContentArticleRepository(),
                new InMemoryContentPublicationRepository(),
                categoryService,
                new ContentStorageApplicationService(new InMemoryContentVersionRepository(), events::add, clock),
                permissionService,
                searchService,
                statisticsService,
                events::add,
                clock
        );
    }

    @Test
    void shouldPublishUnpublishAndMaintainSearchIndex() {
        ArticleDetailView created = lifecycleService.create(createCommand("ART-001", "发布测试", "正文"));

        ArticleDetailView published = lifecycleService.publish(
                created.article().id(),
                publishCommand(null, List.of(allAllowRule()))
        );

        assertThat(published.article().status()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(search(null).total()).isEqualTo(1);
        assertThat(events).extracting(DomainEvent::eventType).contains("content.article.published");

        ArticleDetailView offline = lifecycleService.unpublish(
                created.article().id(),
                new OfflineArticleCommand(TENANT_ID, OPERATOR_ID, "expired", "idem-offline")
        );

        assertThat(offline.article().status()).isEqualTo(ArticleStatus.OFFLINE);
        assertThat(search(null).total()).isZero();
        assertThat(events).extracting(DomainEvent::eventType).contains("content.article.unpublished");
    }

    @Test
    void shouldCreateRollbackDraftFromPreviousVersion() {
        ArticleDetailView created = lifecycleService.create(createCommand("ART-002", "原始标题", "第一版"));
        lifecycleService.updateDraft(created.article().id(), updateCommand("第二版标题", "第二版正文"));

        ArticleDetailView restored = lifecycleService.rollback(
                created.article().id(),
                new RollbackArticleCommand(TENANT_ID, OPERATOR_ID, 1, "idem-rollback")
        );

        assertThat(restored.currentVersion().versionNo()).isEqualTo(3);
        assertThat(restored.currentVersion().title()).isEqualTo("原始标题");
        assertThat(restored.currentVersion().bodyText()).isEqualTo("第一版");
        assertThat(restored.article().status()).isEqualTo(ArticleStatus.DRAFT);
    }

    @Test
    void shouldRequireApprovalWhenReviewModeIsReview() {
        ArticleDetailView created = lifecycleService.create(createCommand("ART-003", "审批发布", "待审批正文"));

        lifecycleService.submit(created.article().id(), new ReviewCommand(TENANT_ID, OPERATOR_ID, "submit", "idem-submit"));
        lifecycleService.approve(created.article().id(), new ReviewCommand(TENANT_ID, OPERATOR_ID, "approved", "idem-approve"));
        ArticleDetailView published = lifecycleService.publish(
                created.article().id(),
                publishCommand(ReviewMode.REVIEW, List.of(allAllowRule()))
        );

        assertThat(published.article().status()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(lifecycleService.reviews(TENANT_ID, created.article().id()))
                .extracting(ContentLifecycleApplicationService.ContentReviewRecord::action)
                .containsExactly(
                        ContentLifecycleApplicationService.ReviewAction.SUBMIT,
                        ContentLifecycleApplicationService.ReviewAction.APPROVE
                );
    }

    @Test
    void shouldFilterSearchResultsByPublicationPermission() {
        ArticleDetailView created = lifecycleService.create(createCommand("ART-004", "权限过滤", "只给一个人看"));
        lifecycleService.publish(
                created.article().id(),
                publishCommand(null, List.of(new PublicationScopeRuleInput(
                        ScopeSubjectType.PERSON,
                        READER_ID,
                        ScopeEffect.ALLOW,
                        0
                )))
        );

        assertThat(search(new ContentSubjectContext(READER_ID, null, null, null, Set.of())).total()).isEqualTo(1);
        assertThat(search(new ContentSubjectContext(OTHER_READER_ID, null, null, null, Set.of())).total()).isZero();
    }

    @Test
    void shouldHandleRepeatedLifecycleCommandsIdempotently() {
        ArticleDetailView created = lifecycleService.create(createCommand("ART-IDEM", "Idempotent article", "body"));
        int afterCreateEvents = events.size();

        ArticleDetailView repeatedCreate = lifecycleService.create(createCommand("ART-IDEM", "Idempotent article", "body"));

        assertThat(repeatedCreate.article().id()).isEqualTo(created.article().id());
        assertThat(repeatedCreate.currentVersion().versionNo()).isEqualTo(created.currentVersion().versionNo());
        assertThat(events).hasSize(afterCreateEvents);

        UpdateArticleCommand update = updateCommand("Idempotent article v2", "body v2");
        ArticleDetailView updated = lifecycleService.updateDraft(created.article().id(), update);
        int afterUpdateEvents = events.size();

        ArticleDetailView repeatedUpdate = lifecycleService.updateDraft(created.article().id(), update);

        assertThat(repeatedUpdate.currentVersion().versionNo()).isEqualTo(updated.currentVersion().versionNo());
        assertThat(events).hasSize(afterUpdateEvents);

        PublishArticleCommand publish = new PublishArticleCommand(
                TENANT_ID,
                OPERATOR_ID,
                null,
                ReviewMode.DIRECT,
                NOW,
                NOW.plusSeconds(3600),
                "publish",
                List.of(allAllowRule()),
                "idem-publish-repeat"
        );
        ArticleDetailView published = lifecycleService.publish(created.article().id(), publish);
        int afterPublishEvents = events.size();

        ArticleDetailView repeatedPublish = lifecycleService.publish(created.article().id(), publish);

        assertThat(repeatedPublish.article().status()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(repeatedPublish.article().currentPublishedVersionNo()).isEqualTo(published.article().currentPublishedVersionNo());
        assertThat(events).hasSize(afterPublishEvents);

        OfflineArticleCommand offlineCommand = new OfflineArticleCommand(TENANT_ID, OPERATOR_ID, "expired", "idem-offline-repeat");
        lifecycleService.unpublish(created.article().id(), offlineCommand);
        int afterOfflineEvents = events.size();

        ArticleDetailView repeatedOffline = lifecycleService.unpublish(created.article().id(), offlineCommand);

        assertThat(repeatedOffline.article().status()).isEqualTo(ArticleStatus.OFFLINE);
        assertThat(events).hasSize(afterOfflineEvents);
    }

    private ContentSearchApplicationService.ContentSearchPage search(ContentSubjectContext subject) {
        return searchService.search(new ContentSearchCriteria(
                TENANT_ID,
                null,
                SearchDocumentStatus.PUBLISHED,
                null,
                null,
                null,
                null,
                1,
                20,
                subject,
                "PUBLISHED_AT_DESC"
        ));
    }

    private CreateArticleCommand createCommand(String articleNo, String title, String body) {
        return new CreateArticleCommand(
                TENANT_ID,
                OPERATOR_ID,
                articleNo,
                title,
                "summary",
                "MARKDOWN",
                body,
                null,
                List.of(),
                List.of("tag"),
                "ARTICLE",
                category.id(),
                OPERATOR_ID,
                "Editor",
                "ORIGINAL",
                null,
                "idem-create-" + articleNo
        );
    }

    private UpdateArticleCommand updateCommand(String title, String body) {
        return new UpdateArticleCommand(
                TENANT_ID,
                OPERATOR_ID,
                title,
                "updated summary",
                "MARKDOWN",
                body,
                null,
                List.of(),
                List.of("tag"),
                category.id(),
                OPERATOR_ID,
                "Editor",
                "ORIGINAL",
                null,
                "idem-update-" + title
        );
    }

    private PublishArticleCommand publishCommand(ReviewMode reviewMode, List<PublicationScopeRuleInput> scopes) {
        return new PublishArticleCommand(
                TENANT_ID,
                OPERATOR_ID,
                null,
                reviewMode,
                NOW,
                NOW.plusSeconds(3600),
                "publish",
                scopes,
                "idem-publish-" + UUID.randomUUID()
        );
    }

    private static PublicationScopeRuleInput allAllowRule() {
        return new PublicationScopeRuleInput(ScopeSubjectType.ALL, null, ScopeEffect.ALLOW, 0);
    }
}
