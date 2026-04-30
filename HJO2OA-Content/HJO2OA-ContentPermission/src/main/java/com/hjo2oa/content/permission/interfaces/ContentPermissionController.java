package com.hjo2oa.content.permission.interfaces;

import com.hjo2oa.content.permission.application.ContentPermissionApplicationService;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.ContentSubjectContext;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.PermissionDecision;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.PublicationScopeRuleInput;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.PublicationScopeRuleView;
import com.hjo2oa.content.permission.application.ContentPermissionApplicationService.ReplacePublicationScopeCommand;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedRequestContextHolder;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/content")
public class ContentPermissionController {

    private final ContentPermissionApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public ContentPermissionController(
            ContentPermissionApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/publications/{publicationId}/scopes")
    public ApiResponse<List<PublicationScopeRuleView>> scopes(
            @PathVariable UUID publicationId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.scopes(tenantId(), publicationId), responseMetaFactory.create(request));
    }

    @PutMapping("/publications/{publicationId}/scopes")
    public ApiResponse<List<PublicationScopeRuleView>> replaceScopes(
            @PathVariable UUID publicationId,
            @Valid @RequestBody ReplaceScopeRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.replaceScopes(new ReplacePublicationScopeCommand(
                        tenantId(),
                        publicationId,
                        body.articleId(),
                        body.operatorId(),
                        body.rules(),
                        idempotencyKey()
                )),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/articles/{articleId}/permissions/preview")
    public ApiResponse<PermissionDecision> preview(
            @PathVariable UUID articleId,
            @Valid @RequestBody PermissionPreviewRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.evaluateArticle(tenantId(), articleId, body.subject()),
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

    public record ReplaceScopeRequest(
            @NotNull UUID articleId,
            @NotNull UUID operatorId,
            List<PublicationScopeRuleInput> rules
    ) {
    }

    public record PermissionPreviewRequest(@NotNull ContentSubjectContext subject) {
    }
}
