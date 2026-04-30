import { del, get, post, put } from '@/services/request'
import type {
  AuditView,
  ChangeMeetingStatusPayload,
  ChangeTaskStatusPayload,
  CollaborationSnapshot,
  CommentView,
  CreateCommentPayload,
  CreateDiscussionPayload,
  CreateMeetingPayload,
  CreateTaskPayload,
  CreateWorkspacePayload,
  DiscussionView,
  MeetingView,
  PublishMeetingMinutesPayload,
  ReminderTriggerResult,
  ReplaceMembersPayload,
  ReplyDiscussionPayload,
  TaskView,
  TriggerReminderPayload,
  WorkspaceView,
} from '@/features/collaboration/types/collaboration'

const COLLABORATION_API_PREFIX = '/v1/biz/collaboration'

function uniqueMutationKey(prefix: string): string {
  if (
    typeof crypto !== 'undefined' &&
    typeof crypto.randomUUID === 'function'
  ) {
    return `${prefix}:${crypto.randomUUID()}`
  }

  return `${prefix}:${Date.now()}:${Math.random().toString(16).slice(2)}`
}

export function getCollaborationSnapshot(): Promise<CollaborationSnapshot> {
  return get<CollaborationSnapshot>(`${COLLABORATION_API_PREFIX}/snapshot`)
}

export function listWorkspaces(): Promise<WorkspaceView[]> {
  return get<WorkspaceView[]>(`${COLLABORATION_API_PREFIX}/workspaces`)
}

export function createWorkspace(
  payload: CreateWorkspacePayload,
): Promise<WorkspaceView> {
  return post<WorkspaceView, CreateWorkspacePayload>(
    `${COLLABORATION_API_PREFIX}/workspaces`,
    payload,
    {
      dedupeKey: `collaboration:workspace:create:${payload.code}`,
      idempotencyKey: uniqueMutationKey('collaboration-workspace-create'),
    },
  )
}

export function replaceWorkspaceMembers(
  workspaceId: string,
  payload: ReplaceMembersPayload,
): Promise<WorkspaceView> {
  return put<WorkspaceView, ReplaceMembersPayload>(
    `${COLLABORATION_API_PREFIX}/workspaces/${workspaceId}/members`,
    payload,
    {
      dedupeKey: `collaboration:workspace:members:${workspaceId}`,
      idempotencyKey: uniqueMutationKey('collaboration-members-replace'),
    },
  )
}

export function listDiscussions(
  workspaceId: string,
): Promise<DiscussionView[]> {
  return get<DiscussionView[]>(
    `${COLLABORATION_API_PREFIX}/workspaces/${workspaceId}/discussions`,
  )
}

export function createDiscussion(
  workspaceId: string,
  payload: CreateDiscussionPayload,
): Promise<DiscussionView> {
  return post<DiscussionView, CreateDiscussionPayload>(
    `${COLLABORATION_API_PREFIX}/workspaces/${workspaceId}/discussions`,
    payload,
    {
      dedupeKey: `collaboration:discussion:create:${workspaceId}:${payload.title}`,
      idempotencyKey: uniqueMutationKey('collaboration-discussion-create'),
    },
  )
}

export function replyDiscussion(
  discussionId: string,
  payload: ReplyDiscussionPayload,
): Promise<DiscussionView> {
  return post<DiscussionView, ReplyDiscussionPayload>(
    `${COLLABORATION_API_PREFIX}/discussions/${discussionId}/replies`,
    payload,
    {
      dedupeKey: `collaboration:discussion:reply:${discussionId}:${payload.body}`,
      idempotencyKey: uniqueMutationKey('collaboration-discussion-reply'),
    },
  )
}

export function markDiscussionRead(
  discussionId: string,
): Promise<DiscussionView> {
  return post<DiscussionView, Record<string, never>>(
    `${COLLABORATION_API_PREFIX}/discussions/${discussionId}/read`,
    {},
    {
      dedupeKey: `collaboration:discussion:read:${discussionId}`,
      idempotencyKey: uniqueMutationKey('collaboration-discussion-read'),
    },
  )
}

export function pinDiscussion(
  discussionId: string,
  pinned: boolean,
): Promise<DiscussionView> {
  return post<DiscussionView, Record<string, never>>(
    `${COLLABORATION_API_PREFIX}/discussions/${discussionId}/pin`,
    {},
    {
      params: new URLSearchParams({ pinned: String(pinned) }),
      dedupeKey: `collaboration:discussion:pin:${discussionId}:${pinned}`,
      idempotencyKey: uniqueMutationKey('collaboration-discussion-pin'),
    },
  )
}

export function closeDiscussion(discussionId: string): Promise<DiscussionView> {
  return post<DiscussionView, Record<string, never>>(
    `${COLLABORATION_API_PREFIX}/discussions/${discussionId}/close`,
    {},
    {
      dedupeKey: `collaboration:discussion:close:${discussionId}`,
      idempotencyKey: uniqueMutationKey('collaboration-discussion-close'),
    },
  )
}

export function createComment(
  payload: CreateCommentPayload,
): Promise<CommentView> {
  return post<CommentView, CreateCommentPayload>(
    `${COLLABORATION_API_PREFIX}/comments`,
    payload,
    {
      dedupeKey: `collaboration:comment:create:${payload.objectType}:${payload.objectId}:${payload.body}`,
      idempotencyKey: uniqueMutationKey('collaboration-comment-create'),
    },
  )
}

export function listComments(
  objectType: string,
  objectId: string,
): Promise<CommentView[]> {
  return get<CommentView[]>(`${COLLABORATION_API_PREFIX}/comments`, {
    params: new URLSearchParams({ objectType, objectId }),
  })
}

export function deleteComment(commentId: string): Promise<CommentView> {
  return del<CommentView>(`${COLLABORATION_API_PREFIX}/comments/${commentId}`, {
    dedupeKey: `collaboration:comment:delete:${commentId}`,
    idempotencyKey: uniqueMutationKey('collaboration-comment-delete'),
  })
}

export function createTask(payload: CreateTaskPayload): Promise<TaskView> {
  return post<TaskView, CreateTaskPayload>(
    `${COLLABORATION_API_PREFIX}/tasks`,
    payload,
    {
      dedupeKey: `collaboration:task:create:${payload.workspaceId}:${payload.title}`,
      idempotencyKey: uniqueMutationKey('collaboration-task-create'),
    },
  )
}

export function changeTaskStatus(
  taskId: string,
  payload: ChangeTaskStatusPayload,
): Promise<TaskView> {
  return post<TaskView, ChangeTaskStatusPayload>(
    `${COLLABORATION_API_PREFIX}/tasks/${taskId}/status`,
    payload,
    {
      dedupeKey: `collaboration:task:status:${taskId}:${payload.status}`,
      idempotencyKey: uniqueMutationKey('collaboration-task-status'),
    },
  )
}

export function addTaskComment(
  taskId: string,
  payload: CreateCommentPayload,
): Promise<CommentView> {
  return post<CommentView, CreateCommentPayload>(
    `${COLLABORATION_API_PREFIX}/tasks/${taskId}/comments`,
    payload,
    {
      dedupeKey: `collaboration:task:comment:${taskId}:${payload.body}`,
      idempotencyKey: uniqueMutationKey('collaboration-task-comment'),
    },
  )
}

export function createMeeting(
  payload: CreateMeetingPayload,
): Promise<MeetingView> {
  return post<MeetingView, CreateMeetingPayload>(
    `${COLLABORATION_API_PREFIX}/meetings`,
    payload,
    {
      dedupeKey: `collaboration:meeting:create:${payload.workspaceId}:${payload.title}:${payload.startAt}`,
      idempotencyKey: uniqueMutationKey('collaboration-meeting-create'),
    },
  )
}

export function changeMeetingStatus(
  meetingId: string,
  payload: ChangeMeetingStatusPayload,
): Promise<MeetingView> {
  return post<MeetingView, ChangeMeetingStatusPayload>(
    `${COLLABORATION_API_PREFIX}/meetings/${meetingId}/status`,
    payload,
    {
      dedupeKey: `collaboration:meeting:status:${meetingId}:${payload.status}`,
      idempotencyKey: uniqueMutationKey('collaboration-meeting-status'),
    },
  )
}

export function publishMeetingMinutes(
  meetingId: string,
  payload: PublishMeetingMinutesPayload,
): Promise<MeetingView> {
  return post<MeetingView, PublishMeetingMinutesPayload>(
    `${COLLABORATION_API_PREFIX}/meetings/${meetingId}/minutes`,
    payload,
    {
      dedupeKey: `collaboration:meeting:minutes:${meetingId}`,
      idempotencyKey: uniqueMutationKey('collaboration-meeting-minutes'),
    },
  )
}

export function triggerDueMeetingReminders(
  payload: TriggerReminderPayload = {},
): Promise<ReminderTriggerResult> {
  return post<ReminderTriggerResult, TriggerReminderPayload>(
    `${COLLABORATION_API_PREFIX}/meeting-reminders/trigger-due`,
    payload,
    {
      dedupeKey: 'collaboration:meeting-reminders:trigger-due',
      idempotencyKey: uniqueMutationKey('collaboration-meeting-reminders'),
    },
  )
}

export function listAuditTrail(): Promise<AuditView[]> {
  return get<AuditView[]>(`${COLLABORATION_API_PREFIX}/audit`)
}
