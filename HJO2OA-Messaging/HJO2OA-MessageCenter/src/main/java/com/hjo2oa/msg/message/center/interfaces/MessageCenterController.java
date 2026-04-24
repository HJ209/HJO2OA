package com.hjo2oa.msg.message.center.interfaces;

import com.hjo2oa.msg.message.center.application.MessageNotificationActionApplicationService;
import com.hjo2oa.msg.message.center.application.MessageNotificationQueryApplicationService;
import com.hjo2oa.msg.message.center.domain.NotificationBulkReadResult;
import com.hjo2oa.msg.message.center.domain.NotificationDetail;
import com.hjo2oa.msg.message.center.domain.NotificationSummary;
import com.hjo2oa.msg.message.center.domain.NotificationUnreadSummary;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/msg")
public class MessageCenterController {

    private final MessageNotificationQueryApplicationService queryApplicationService;
    private final MessageNotificationActionApplicationService actionApplicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public MessageCenterController(
            MessageNotificationQueryApplicationService queryApplicationService,
            MessageNotificationActionApplicationService actionApplicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.queryApplicationService = queryApplicationService;
        this.actionApplicationService = actionApplicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/messages")
    public ApiResponse<List<NotificationSummary>> messages(HttpServletRequest request) {
        return ApiResponse.success(queryApplicationService.inbox(), responseMetaFactory.create(request));
    }

    @GetMapping("/messages/{notificationId}")
    public ApiResponse<NotificationDetail> detail(
            @PathVariable String notificationId,
            HttpServletRequest request
    ) {
        NotificationDetail detail = queryApplicationService.detail(notificationId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Notification not found"));
        return ApiResponse.success(detail, responseMetaFactory.create(request));
    }

    @GetMapping("/unread-summary")
    public ApiResponse<NotificationUnreadSummary> unreadSummary(HttpServletRequest request) {
        return ApiResponse.success(queryApplicationService.unreadSummary(), responseMetaFactory.create(request));
    }

    @PostMapping("/messages/bulk-read")
    public ApiResponse<NotificationBulkReadResult> bulkMarkRead(
            @RequestBody NotificationBulkReadRequest request,
            HttpServletRequest servletRequest
    ) {
        try {
            return ApiResponse.success(
                    actionApplicationService.bulkMarkRead(request.notificationIds()),
                    responseMetaFactory.create(servletRequest)
            );
        } catch (IllegalArgumentException ex) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping("/messages/{notificationId}/read")
    public ApiResponse<NotificationSummary> markRead(
            @PathVariable String notificationId,
            HttpServletRequest request
    ) {
        NotificationSummary notification = actionApplicationService.markRead(notificationId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Notification not found"));
        return ApiResponse.success(notification, responseMetaFactory.create(request));
    }
}
