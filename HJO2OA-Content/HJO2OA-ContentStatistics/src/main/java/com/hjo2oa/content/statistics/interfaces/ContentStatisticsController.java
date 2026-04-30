package com.hjo2oa.content.statistics.interfaces;

import com.hjo2oa.content.statistics.application.ContentStatisticsApplicationService;
import com.hjo2oa.content.statistics.application.ContentStatisticsApplicationService.ContentActionType;
import com.hjo2oa.content.statistics.application.ContentStatisticsApplicationService.ContentEngagementSnapshotView;
import com.hjo2oa.content.statistics.application.ContentStatisticsApplicationService.RecordContentActionCommand;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedRequestContextHolder;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/v1/content")
public class ContentStatisticsController {

    private final ContentStatisticsApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public ContentStatisticsController(
            ContentStatisticsApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/articles/{articleId}/actions")
    public ApiResponse<ContentEngagementSnapshotView> recordAction(
            @PathVariable UUID articleId,
            @RequestBody RecordActionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.recordAction(new RecordContentActionCommand(
                        tenantId(),
                        articleId,
                        body == null ? null : body.personId(),
                        body == null ? null : body.assignmentId(),
                        body == null ? ContentActionType.READ : body.actionType(),
                        idempotencyKey()
                )),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/articles/{articleId}/statistics")
    public ApiResponse<ContentEngagementSnapshotView> snapshot(
            @PathVariable UUID articleId,
            @RequestParam(required = false) String bucket,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.snapshot(tenantId(), articleId, bucket),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/statistics/ranking")
    public ApiResponse<List<ContentEngagementSnapshotView>> ranking(
            @RequestParam(required = false) String bucket,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.ranking(tenantId(), bucket, limit),
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

    public record RecordActionRequest(UUID personId, UUID assignmentId, ContentActionType actionType) {
    }
}
