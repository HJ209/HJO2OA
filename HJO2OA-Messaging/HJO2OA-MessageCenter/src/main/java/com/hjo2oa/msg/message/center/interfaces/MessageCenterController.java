package com.hjo2oa.msg.message.center.interfaces;

import com.hjo2oa.msg.message.center.application.MessageNotificationActionApplicationService;
import com.hjo2oa.msg.message.center.application.MessageNotificationQueryApplicationService;
import com.hjo2oa.msg.message.center.domain.NotificationBulkReadResult;
import com.hjo2oa.msg.message.center.domain.NotificationDetail;
import com.hjo2oa.msg.message.center.domain.NotificationSummary;
import com.hjo2oa.msg.message.center.domain.NotificationUnreadSummary;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/msg")
public class MessageCenterController {

    private final MessageNotificationQueryApplicationService queryApplicationService;
    private final MessageNotificationActionApplicationService actionApplicationService;

    public MessageCenterController(
            MessageNotificationQueryApplicationService queryApplicationService,
            MessageNotificationActionApplicationService actionApplicationService
    ) {
        this.queryApplicationService = queryApplicationService;
        this.actionApplicationService = actionApplicationService;
    }

    @GetMapping("/messages")
    public List<NotificationSummary> messages() {
        return queryApplicationService.inbox();
    }

    @GetMapping("/messages/{notificationId}")
    public NotificationDetail detail(@PathVariable String notificationId) {
        return queryApplicationService.detail(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }

    @GetMapping("/unread-summary")
    public NotificationUnreadSummary unreadSummary() {
        return queryApplicationService.unreadSummary();
    }

    @PostMapping("/messages/bulk-read")
    public NotificationBulkReadResult bulkMarkRead(@RequestBody NotificationBulkReadRequest request) {
        try {
            return actionApplicationService.bulkMarkRead(request.notificationIds());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/messages/{notificationId}/read")
    public NotificationSummary markRead(@PathVariable String notificationId) {
        return actionApplicationService.markRead(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }
}
