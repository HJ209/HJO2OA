package com.hjo2oa.msg.message.center.interfaces;

import com.hjo2oa.biz.collaboration.hub.domain.CollaborationEvents;
import com.hjo2oa.msg.message.center.application.MessageNotificationProjectionApplicationService;
import com.hjo2oa.msg.message.center.domain.NotificationCategory;
import com.hjo2oa.msg.message.center.domain.NotificationPriority;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CollaborationNotificationEventListener {

    private static final String SOURCE_MODULE = "collaboration-hub";

    private final MessageNotificationProjectionApplicationService projectionApplicationService;

    public CollaborationNotificationEventListener(
            MessageNotificationProjectionApplicationService projectionApplicationService
    ) {
        this.projectionApplicationService = projectionApplicationService;
    }

    @EventListener
    public void onMention(CollaborationEvents.MentionCreatedEvent event) {
        projectionApplicationService.projectBusinessNotification(
                event.eventId(),
                event.eventType() + ":" + event.sourceType() + ":" + event.sourceId() + ":" + event.recipientId(),
                event.tenantId(),
                event.recipientId(),
                "有人提到了你",
                event.title(),
                event.deepLink(),
                NotificationCategory.COLLAB_MENTION,
                NotificationPriority.URGENT,
                SOURCE_MODULE,
                event.eventType(),
                event.sourceId(),
                event.occurredAt()
        );
    }

    @EventListener
    public void onTaskAssigned(CollaborationEvents.TaskAssignedEvent event) {
        projectionApplicationService.projectBusinessNotification(
                event.eventId(),
                event.eventType() + ":" + event.taskId() + ":" + event.assigneeId(),
                event.tenantId(),
                event.assigneeId(),
                "协同任务已分配",
                event.title(),
                event.deepLink(),
                NotificationCategory.COLLAB_TASK_ASSIGNED,
                NotificationPriority.fromUrgency(event.priority()),
                SOURCE_MODULE,
                event.eventType(),
                event.taskId(),
                event.occurredAt()
        );
    }

    @EventListener
    public void onTaskChanged(CollaborationEvents.TaskChangedEvent event) {
        projectionApplicationService.projectBusinessNotification(
                event.eventId(),
                event.eventType() + ":" + event.taskId() + ":" + event.recipientId() + ":" + event.newStatus(),
                event.tenantId(),
                event.recipientId(),
                "协同任务状态变更",
                event.title() + "：" + event.oldStatus() + " -> " + event.newStatus(),
                event.deepLink(),
                NotificationCategory.COLLAB_TASK_CHANGED,
                NotificationPriority.NORMAL,
                SOURCE_MODULE,
                event.eventType(),
                event.taskId(),
                event.occurredAt()
        );
    }

    @EventListener
    public void onMeetingReminder(CollaborationEvents.MeetingReminderDueEvent event) {
        projectionApplicationService.projectBusinessNotification(
                event.eventId(),
                event.eventType() + ":" + event.meetingId() + ":" + event.participantId(),
                event.tenantId(),
                event.participantId(),
                "会议即将开始",
                event.title() + " 将于 " + event.startAt() + " 开始",
                event.deepLink(),
                NotificationCategory.COLLAB_MEETING_REMINDER,
                NotificationPriority.URGENT,
                SOURCE_MODULE,
                event.eventType(),
                event.meetingId(),
                event.occurredAt()
        );
    }
}
