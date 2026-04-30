package com.hjo2oa.content.category.management.interfaces;

import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.CategoryTreeNode;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.CategoryView;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.CreateCategoryCommand;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.MoveCategoryCommand;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.PermissionPreviewResult;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.PermissionRuleInput;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.PermissionRuleView;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.PermissionScope;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.PermissionSubjectContext;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.UpdateCategoryCommand;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
@RequestMapping("/api/v1/content/categories")
public class ContentCategoryController {

    private final ContentCategoryApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public ContentCategoryController(
            ContentCategoryApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/tree")
    public ApiResponse<List<CategoryTreeNode>> tree(
            @RequestParam(defaultValue = "false") boolean enabledOnly,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.tree(tenantId(), enabledOnly),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{categoryId}")
    public ApiResponse<CategoryView> get(@PathVariable UUID categoryId, HttpServletRequest request) {
        return ApiResponse.success(applicationService.get(tenantId(), categoryId), responseMetaFactory.create(request));
    }

    @PostMapping
    public ApiResponse<CategoryView> create(
            @Valid @RequestBody UpsertCategoryRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.create(body.toCreateCommand(tenantId(), idempotencyKey())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{categoryId}")
    public ApiResponse<CategoryView> update(
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpsertCategoryRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.update(categoryId, body.toUpdateCommand(tenantId(), idempotencyKey())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{categoryId}/reorder")
    public ApiResponse<CategoryView> reorder(
            @PathVariable UUID categoryId,
            @Valid @RequestBody MoveCategoryRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.move(categoryId, body.toCommand(tenantId(), idempotencyKey())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{categoryId}/enable")
    public ApiResponse<CategoryView> enable(
            @PathVariable UUID categoryId,
            @Valid @RequestBody OperatorRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.enable(tenantId(), categoryId, body.operatorId()),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{categoryId}/disable")
    public ApiResponse<CategoryView> disable(
            @PathVariable UUID categoryId,
            @Valid @RequestBody OperatorRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.disable(tenantId(), categoryId, body.operatorId()),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{categoryId}/permissions")
    public ApiResponse<List<PermissionRuleView>> permissions(
            @PathVariable UUID categoryId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.permissions(tenantId(), categoryId),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{categoryId}/permissions")
    public ApiResponse<List<PermissionRuleView>> replacePermissions(
            @PathVariable UUID categoryId,
            @Valid @RequestBody ReplacePermissionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.replacePermissions(
                        tenantId(),
                        categoryId,
                        body.operatorId(),
                        body.permissions()
                ),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{categoryId}/permissions/preview")
    public ApiResponse<PermissionPreviewResult> previewPermissions(
            @PathVariable UUID categoryId,
            @Valid @RequestBody PermissionPreviewRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.previewPermission(
                        tenantId(),
                        categoryId,
                        body.subject(),
                        body.scope() == null ? PermissionScope.READ : body.scope()
                ),
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

    public record UpsertCategoryRequest(
            @NotNull UUID operatorId,
            @NotBlank String code,
            @NotBlank String name,
            String categoryType,
            UUID parentId,
            String routePath,
            int sortOrder,
            String visibleMode,
            List<PermissionRuleInput> permissions
    ) {

        CreateCategoryCommand toCreateCommand(UUID tenantId, String idempotencyKey) {
            return new CreateCategoryCommand(
                    tenantId,
                    operatorId,
                    code,
                    name,
                    categoryType,
                    parentId,
                    routePath,
                    sortOrder,
                    visibleMode,
                    permissions,
                    idempotencyKey
            );
        }

        UpdateCategoryCommand toUpdateCommand(UUID tenantId, String idempotencyKey) {
            return new UpdateCategoryCommand(
                    tenantId,
                    operatorId,
                    code,
                    name,
                    categoryType,
                    parentId,
                    routePath,
                    sortOrder,
                    visibleMode,
                    permissions,
                    idempotencyKey
            );
        }
    }

    public record MoveCategoryRequest(
            @NotNull UUID operatorId,
            UUID parentId,
            int sortOrder,
            Integer expectedVersionNo
    ) {

        MoveCategoryCommand toCommand(UUID tenantId, String idempotencyKey) {
            return new MoveCategoryCommand(tenantId, operatorId, parentId, sortOrder, expectedVersionNo, idempotencyKey);
        }
    }

    public record OperatorRequest(@NotNull UUID operatorId) {
    }

    public record ReplacePermissionRequest(
            @NotNull UUID operatorId,
            List<PermissionRuleInput> permissions
    ) {
    }

    public record PermissionPreviewRequest(
            PermissionSubjectContext subject,
            PermissionScope scope
    ) {
    }
}
