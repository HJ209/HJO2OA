package com.hjo2oa.content.search.interfaces;

import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.ContentSubjectContext;
import com.hjo2oa.content.search.application.ContentSearchApplicationService;
import com.hjo2oa.content.search.application.ContentSearchApplicationService.ContentSearchCriteria;
import com.hjo2oa.content.search.application.ContentSearchApplicationService.ContentSearchPage;
import com.hjo2oa.content.search.application.ContentSearchApplicationService.PortalContentFeed;
import com.hjo2oa.content.search.application.ContentSearchApplicationService.PortalContentQuery;
import com.hjo2oa.content.search.application.ContentSearchApplicationService.SearchDocumentStatus;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedRequestContextHolder;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/content")
public class ContentSearchController {

    private final ContentSearchApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public ContentSearchController(
            ContentSearchApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/search")
    public ApiResponse<ContentSearchPage> search(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) SearchDocumentStatus status,
            @RequestParam(required = false) UUID authorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID personId,
            @RequestParam(required = false) UUID assignmentId,
            @RequestParam(required = false) UUID positionId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) Set<UUID> roleIds,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.search(new ContentSearchCriteria(
                        tenantId(),
                        categoryId,
                        status == null ? SearchDocumentStatus.PUBLISHED : status,
                        authorId,
                        from,
                        to,
                        keyword,
                        page,
                        size,
                        new ContentSubjectContext(personId, assignmentId, positionId, departmentId, roleIds),
                        "PUBLISHED_AT_DESC"
                )),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/search/advanced")
    public ApiResponse<ContentSearchPage> advanced(
            @RequestBody AdvancedSearchRequest body,
            HttpServletRequest request
    ) {
        AdvancedSearchRequest safeBody = body == null ? AdvancedSearchRequest.empty() : body;
        return ApiResponse.success(
                applicationService.search(safeBody.toCriteria(tenantId())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/portal/feed")
    public ApiResponse<PortalContentFeed> portalFeed(
            @RequestBody PortalFeedRequest body,
            HttpServletRequest request
    ) {
        PortalFeedRequest safeBody = body == null ? PortalFeedRequest.empty() : body;
        return ApiResponse.success(
                applicationService.latestForPortal(safeBody.toQuery(tenantId())),
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

    public record AdvancedSearchRequest(
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

        static AdvancedSearchRequest empty() {
            return new AdvancedSearchRequest(null, null, null, null, null, null, 1, 20, null, null);
        }

        ContentSearchCriteria toCriteria(UUID tenantId) {
            return new ContentSearchCriteria(
                    tenantId,
                    categoryId,
                    status == null ? SearchDocumentStatus.PUBLISHED : status,
                    authorId,
                    publishedFrom,
                    publishedTo,
                    keyword,
                    page <= 0 ? 1 : page,
                    size <= 0 ? 20 : size,
                    subject,
                    sort == null ? "PUBLISHED_AT_DESC" : sort
            );
        }
    }

    public record PortalFeedRequest(
            UUID categoryId,
            String keyword,
            int limit,
            ContentSubjectContext subject
    ) {

        static PortalFeedRequest empty() {
            return new PortalFeedRequest(null, null, 10, null);
        }

        PortalContentQuery toQuery(UUID tenantId) {
            return new PortalContentQuery(tenantId, categoryId, keyword, limit <= 0 ? 10 : limit, subject);
        }
    }
}
