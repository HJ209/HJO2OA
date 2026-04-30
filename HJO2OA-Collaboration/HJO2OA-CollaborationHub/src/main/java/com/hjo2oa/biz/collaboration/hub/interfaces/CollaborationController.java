package com.hjo2oa.biz.collaboration.hub.interfaces;

import com.hjo2oa.biz.collaboration.hub.application.CollaborationApplicationService;
import com.hjo2oa.biz.collaboration.hub.application.CollaborationRequestContext;
import com.hjo2oa.biz.collaboration.hub.domain.CollaborationViews;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/biz/collaboration")
public class CollaborationController {

    private final CollaborationApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public CollaborationController(
            CollaborationApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/snapshot")
    public ApiResponse<CollaborationViews.CollaborationSnapshot> snapshot(HttpServletRequest request) {
        return ApiResponse.success(
                applicationService.snapshot(CollaborationRequestContext.from(request)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/workspaces")
    public ApiResponse<List<CollaborationViews.WorkspaceView>> workspaces(HttpServletRequest request) {
        return ApiResponse.success(
                applicationService.listWorkspaces(CollaborationRequestContext.from(request)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/workspaces")
    public ApiResponse<CollaborationViews.WorkspaceView> createWorkspace(
            @RequestBody CollaborationDtos.CreateWorkspaceRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.createWorkspace(CollaborationRequestContext.from(request), body.toCommand()),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/workspaces/{workspaceId}")
    public ApiResponse<CollaborationViews.WorkspaceView> workspace(
            @PathVariable String workspaceId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.workspace(CollaborationRequestContext.from(request), workspaceId),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/workspaces/{workspaceId}/members")
    public ApiResponse<CollaborationViews.WorkspaceView> replaceMembers(
            @PathVariable String workspaceId,
            @RequestBody CollaborationDtos.ReplaceMembersRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.replaceMembers(
                        CollaborationRequestContext.from(request),
                        workspaceId,
                        body.toCommand()
                ),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/workspaces/{workspaceId}/discussions")
    public ApiResponse<List<CollaborationViews.DiscussionView>> discussions(
            @PathVariable String workspaceId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listDiscussions(CollaborationRequestContext.from(request), workspaceId),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/workspaces/{workspaceId}/discussions")
    public ApiResponse<CollaborationViews.DiscussionView> createDiscussion(
            @PathVariable String workspaceId,
            @RequestBody CollaborationDtos.CreateDiscussionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.createDiscussion(
                        CollaborationRequestContext.from(request),
                        workspaceId,
                        body.toCommand()
                ),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/discussions/{discussionId}")
    public ApiResponse<CollaborationViews.DiscussionView> discussion(
            @PathVariable String discussionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.discussion(CollaborationRequestContext.from(request), discussionId),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/discussions/{discussionId}/replies")
    public ApiResponse<CollaborationViews.DiscussionView> replyDiscussion(
            @PathVariable String discussionId,
            @RequestBody CollaborationDtos.ReplyDiscussionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.replyDiscussion(
                        CollaborationRequestContext.from(request),
                        discussionId,
                        body.toCommand()
                ),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/discussions/{discussionId}/read")
    public ApiResponse<CollaborationViews.DiscussionView> markDiscussionRead(
            @PathVariable String discussionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.markDiscussionRead(CollaborationRequestContext.from(request), discussionId),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/discussions/{discussionId}/pin")
    public ApiResponse<CollaborationViews.DiscussionView> pinDiscussion(
            @PathVariable String discussionId,
            @RequestParam(defaultValue = "true") boolean pinned,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.pinDiscussion(CollaborationRequestContext.from(request), discussionId, pinned),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/discussions/{discussionId}/close")
    public ApiResponse<CollaborationViews.DiscussionView> closeDiscussion(
            @PathVariable String discussionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.closeDiscussion(CollaborationRequestContext.from(request), discussionId),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/comments")
    public ApiResponse<List<CollaborationViews.CommentView>> comments(
            @RequestParam String objectType,
            @RequestParam String objectId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listComments(CollaborationRequestContext.from(request), objectType, objectId),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/comments")
    public ApiResponse<CollaborationViews.CommentView> createComment(
            @RequestBody CollaborationDtos.CreateCommentRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.createComment(CollaborationRequestContext.from(request), body.toCommand()),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<CollaborationViews.CommentView> deleteComment(
            @PathVariable String commentId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.deleteComment(CollaborationRequestContext.from(request), commentId),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/tasks")
    public ApiResponse<List<CollaborationViews.TaskView>> tasks(
            @RequestParam(required = false) String workspaceId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listTasks(CollaborationRequestContext.from(request), workspaceId),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/tasks")
    public ApiResponse<CollaborationViews.TaskView> createTask(
            @RequestBody CollaborationDtos.CreateTaskRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.createTask(CollaborationRequestContext.from(request), body.toCommand()),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/tasks/{taskId}/status")
    public ApiResponse<CollaborationViews.TaskView> changeTaskStatus(
            @PathVariable String taskId,
            @RequestBody CollaborationDtos.ChangeTaskStatusRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.changeTaskStatus(
                        CollaborationRequestContext.from(request),
                        taskId,
                        body.toCommand()
                ),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/tasks/{taskId}/comments")
    public ApiResponse<CollaborationViews.CommentView> commentOnTask(
            @PathVariable String taskId,
            @RequestBody CollaborationDtos.CreateCommentRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.commentOnTask(CollaborationRequestContext.from(request), taskId, body.toCommand()),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/meetings")
    public ApiResponse<List<CollaborationViews.MeetingView>> meetings(
            @RequestParam(required = false) String workspaceId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listMeetings(CollaborationRequestContext.from(request), workspaceId),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/meetings")
    public ApiResponse<CollaborationViews.MeetingView> createMeeting(
            @RequestBody CollaborationDtos.CreateMeetingRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.createMeeting(CollaborationRequestContext.from(request), body.toCommand()),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/meetings/{meetingId}/status")
    public ApiResponse<CollaborationViews.MeetingView> changeMeetingStatus(
            @PathVariable String meetingId,
            @RequestBody CollaborationDtos.ChangeMeetingStatusRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.changeMeetingStatus(
                        CollaborationRequestContext.from(request),
                        meetingId,
                        body.toCommand()
                ),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/meetings/{meetingId}/minutes")
    public ApiResponse<CollaborationViews.MeetingView> publishMeetingMinutes(
            @PathVariable String meetingId,
            @RequestBody CollaborationDtos.PublishMeetingMinutesRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.publishMeetingMinutes(
                        CollaborationRequestContext.from(request),
                        meetingId,
                        body.toCommand()
                ),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/meeting-reminders/trigger-due")
    public ApiResponse<CollaborationViews.ReminderTriggerResult> triggerDueReminders(
            @RequestBody(required = false) CollaborationDtos.TriggerReminderRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.triggerDueMeetingReminders(
                        CollaborationRequestContext.from(request),
                        body == null ? null : body.dueAt()
                ),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/audit")
    public ApiResponse<List<CollaborationViews.AuditView>> auditTrail(HttpServletRequest request) {
        return ApiResponse.success(
                applicationService.listAuditTrail(CollaborationRequestContext.from(request)),
                responseMetaFactory.create(request)
        );
    }
}
