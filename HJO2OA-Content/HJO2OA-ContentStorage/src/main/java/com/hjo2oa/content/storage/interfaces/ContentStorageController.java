package com.hjo2oa.content.storage.interfaces;

import com.hjo2oa.content.storage.application.ContentStorageApplicationService;
import com.hjo2oa.content.storage.application.ContentStorageApplicationService.ContentVersionCompareView;
import com.hjo2oa.content.storage.application.ContentStorageApplicationService.ContentVersionView;
import com.hjo2oa.content.storage.application.ContentStorageApplicationService.RollbackVersionCommand;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/content/articles/{articleId}/versions")
public class ContentStorageController {

    private final ContentStorageApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public ContentStorageController(
            ContentStorageApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping
    public ApiResponse<List<ContentVersionView>> versions(
            @PathVariable UUID articleId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.versions(tenantId(), articleId),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{versionNo}")
    public ApiResponse<ContentVersionView> version(
            @PathVariable UUID articleId,
            @PathVariable int versionNo,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.get(tenantId(), articleId, versionNo),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/compare")
    public ApiResponse<ContentVersionCompareView> compare(
            @PathVariable UUID articleId,
            @RequestParam int leftVersionNo,
            @RequestParam int rightVersionNo,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.compare(tenantId(), articleId, leftVersionNo, rightVersionNo),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/rollback")
    public ApiResponse<ContentVersionView> rollback(
            @PathVariable UUID articleId,
            @Valid @RequestBody RollbackRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.rollback(new RollbackVersionCommand(
                        tenantId(),
                        articleId,
                        body.targetVersionNo(),
                        body.operatorId(),
                        idempotencyKey()
                )),
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

    public record RollbackRequest(@NotNull UUID operatorId, int targetVersionNo) {
    }
}
