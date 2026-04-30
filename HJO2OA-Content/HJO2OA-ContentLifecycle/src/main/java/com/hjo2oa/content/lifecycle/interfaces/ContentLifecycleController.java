package com.hjo2oa.content.lifecycle.interfaces;

import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ArticleDetailView;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ArticleListQuery;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ArticlePage;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ArticleStatus;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ContentPublicationRecord;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ContentReviewRecord;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.CreateArticleCommand;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.OfflineArticleCommand;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.PublishArticleCommand;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ReviewCommand;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.ReviewMode;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.RollbackArticleCommand;
import com.hjo2oa.content.lifecycle.application.ContentLifecycleApplicationService.UpdateArticleCommand;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.PublicationScopeRuleInput;
import com.hjo2oa.content.storage.application.ContentStorageApplicationService.ContentAttachment;
import com.hjo2oa.content.storage.application.ContentStorageApplicationService.ContentVersionView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedRequestContextHolder;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/content/articles")
public class ContentLifecycleController {

    private final ContentLifecycleApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public ContentLifecycleController(
            ContentLifecycleApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping
    public ApiResponse<ArticlePage> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(required = false) UUID authorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.list(new ArticleListQuery(tenantId(), categoryId, status, authorId, from, to, keyword, page, size)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{articleId}")
    public ApiResponse<ArticleDetailView> get(@PathVariable UUID articleId, HttpServletRequest request) {
        return ApiResponse.success(applicationService.get(tenantId(), articleId), responseMetaFactory.create(request));
    }

    @PostMapping
    public ApiResponse<ArticleDetailView> create(
            @Valid @RequestBody UpsertArticleRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.create(body.toCreateCommand(tenantId(), idempotencyKey())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{articleId}")
    public ApiResponse<ArticleDetailView> update(
            @PathVariable UUID articleId,
            @Valid @RequestBody UpsertArticleRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.updateDraft(articleId, body.toUpdateCommand(tenantId(), idempotencyKey())),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{articleId}/history")
    public ApiResponse<List<ContentVersionView>> history(
            @PathVariable UUID articleId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.history(tenantId(), articleId), responseMetaFactory.create(request));
    }

    @GetMapping("/{articleId}/publications")
    public ApiResponse<List<ContentPublicationRecord>> publications(
            @PathVariable UUID articleId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.publications(tenantId(), articleId), responseMetaFactory.create(request));
    }

    @GetMapping("/{articleId}/reviews")
    public ApiResponse<List<ContentReviewRecord>> reviews(
            @PathVariable UUID articleId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.reviews(tenantId(), articleId), responseMetaFactory.create(request));
    }

    @PostMapping("/{articleId}/submit")
    public ApiResponse<ArticleDetailView> submit(
            @PathVariable UUID articleId,
            @Valid @RequestBody ReviewRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.submit(articleId, body.toCommand(tenantId(), idempotencyKey())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{articleId}/approve")
    public ApiResponse<ArticleDetailView> approve(
            @PathVariable UUID articleId,
            @Valid @RequestBody ReviewRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.approve(articleId, body.toCommand(tenantId(), idempotencyKey())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{articleId}/reject")
    public ApiResponse<ArticleDetailView> reject(
            @PathVariable UUID articleId,
            @Valid @RequestBody ReviewRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.reject(articleId, body.toCommand(tenantId(), idempotencyKey())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{articleId}/publish")
    public ApiResponse<ArticleDetailView> publish(
            @PathVariable UUID articleId,
            @Valid @RequestBody PublishRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.publish(articleId, body.toCommand(tenantId(), idempotencyKey())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{articleId}/unpublish")
    public ApiResponse<ArticleDetailView> unpublish(
            @PathVariable UUID articleId,
            @Valid @RequestBody OfflineRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.unpublish(articleId, body.toCommand(tenantId(), idempotencyKey())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{articleId}/archive")
    public ApiResponse<ArticleDetailView> archive(
            @PathVariable UUID articleId,
            @Valid @RequestBody OfflineRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.archive(articleId, body.toCommand(tenantId(), idempotencyKey())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{articleId}/rollback")
    public ApiResponse<ArticleDetailView> rollback(
            @PathVariable UUID articleId,
            @Valid @RequestBody RollbackRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.rollback(articleId, body.toCommand(tenantId(), idempotencyKey())),
                responseMetaFactory.create(request)
        );
    }

    private static UUID tenantId() {
        String tenantId = SharedRequestContextHolder.current()
                .map(context -> context.tenantId())
                .orElse(null);
        if (tenantId == null || tenantId.isBlank()) {
            throw new BizException(SharedErrorDescriptors.TENANT_REQUIRED, "X-Tenant-Id is required");
        }
        return UUID.fromString(tenantId);
    }

    private static String idempotencyKey() {
        return SharedRequestContextHolder.current()
                .map(context -> context.idempotencyKey())
                .orElse(null);
    }

    public record UpsertArticleRequest(
            @NotNull UUID operatorId,
            String articleNo,
            @NotBlank String title,
            String summary,
            String bodyFormat,
            @NotBlank String bodyText,
            UUID coverAttachmentId,
            List<ContentAttachment> attachments,
            List<String> tags,
            String contentType,
            @NotNull UUID mainCategoryId,
            UUID authorId,
            String authorName,
            String sourceType,
            String sourceUrl
    ) {

        CreateArticleCommand toCreateCommand(UUID tenantId, String idempotencyKey) {
            String resolvedArticleNo = articleNo == null || articleNo.isBlank()
                    ? "ART-" + UUID.randomUUID()
                    : articleNo;
            return new CreateArticleCommand(
                    tenantId,
                    operatorId,
                    resolvedArticleNo,
                    title,
                    summary,
                    bodyFormat,
                    bodyText,
                    coverAttachmentId,
                    attachments,
                    tags,
                    contentType,
                    mainCategoryId,
                    authorId,
                    authorName,
                    sourceType,
                    sourceUrl,
                    idempotencyKey
            );
        }

        UpdateArticleCommand toUpdateCommand(UUID tenantId, String idempotencyKey) {
            return new UpdateArticleCommand(
                    tenantId,
                    operatorId,
                    title,
                    summary,
                    bodyFormat,
                    bodyText,
                    coverAttachmentId,
                    attachments,
                    tags,
                    mainCategoryId,
                    authorId,
                    authorName,
                    sourceType,
                    sourceUrl,
                    idempotencyKey
            );
        }
    }

    public record ReviewRequest(@NotNull UUID operatorId, String opinion) {

        ReviewCommand toCommand(UUID tenantId, String idempotencyKey) {
            return new ReviewCommand(tenantId, operatorId, opinion, idempotencyKey);
        }
    }

    public record PublishRequest(
            @NotNull UUID operatorId,
            Integer versionNo,
            ReviewMode reviewMode,
            Instant startAt,
            Instant endAt,
            String reason,
            List<PublicationScopeRuleInput> scopes
    ) {

        PublishArticleCommand toCommand(UUID tenantId, String idempotencyKey) {
            return new PublishArticleCommand(
                    tenantId,
                    operatorId,
                    versionNo,
                    reviewMode,
                    startAt,
                    endAt,
                    reason,
                    scopes,
                    idempotencyKey
            );
        }
    }

    public record OfflineRequest(@NotNull UUID operatorId, String reason) {

        OfflineArticleCommand toCommand(UUID tenantId, String idempotencyKey) {
            return new OfflineArticleCommand(tenantId, operatorId, reason, idempotencyKey);
        }
    }

    public record RollbackRequest(@NotNull UUID operatorId, int targetVersionNo) {

        RollbackArticleCommand toCommand(UUID tenantId, String idempotencyKey) {
            return new RollbackArticleCommand(tenantId, operatorId, targetVersionNo, idempotencyKey);
        }
    }
}
