export type WorkspaceRole = 'OWNER' | 'MANAGER' | 'MEMBER' | 'VIEWER'

export type WorkspacePermission =
  | 'READ'
  | 'COMMENT'
  | 'CREATE_DISCUSSION'
  | 'MANAGE_DISCUSSION'
  | 'CREATE_TASK'
  | 'MANAGE_TASK'
  | 'CREATE_MEETING'
  | 'MANAGE_MEETING'
  | 'MANAGE_MEMBERS'

export type TaskStatus =
  | 'TODO'
  | 'IN_PROGRESS'
  | 'BLOCKED'
  | 'DONE'
  | 'CANCELLED'

export type MeetingStatus = 'SCHEDULED' | 'ONGOING' | 'COMPLETED' | 'CANCELLED'

export type CollaborationPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'

export interface CollaborationAttachment {
  attachmentId: string
  fileName: string
}

export interface WorkspaceMember {
  personId: string
  roleCode: WorkspaceRole
  permissions: WorkspacePermission[]
  status: string
}

export interface WorkspaceView {
  workspaceId: string
  code: string
  name: string
  description?: string | null
  status: string
  visibility: string
  ownerId: string
  members: WorkspaceMember[]
  updatedAt: string
}

export interface DiscussionReplyView {
  replyId: string
  authorId: string
  body: string
  deleted: boolean
  createdAt: string
}

export interface DiscussionView {
  discussionId: string
  workspaceId: string
  title: string
  body: string
  authorId: string
  status: 'OPEN' | 'CLOSED'
  pinned: boolean
  readByCurrentUser: boolean
  attachments: CollaborationAttachment[]
  replies: DiscussionReplyView[]
  updatedAt: string
}

export interface CommentView {
  commentId: string
  workspaceId: string
  objectType: string
  objectId: string
  authorId: string
  body: string
  deleted: boolean
  createdAt: string
  updatedAt: string
}

export interface TaskView {
  taskId: string
  workspaceId: string
  title: string
  description?: string | null
  creatorId: string
  assigneeId: string
  status: TaskStatus
  priority: CollaborationPriority
  dueAt?: string | null
  completedAt?: string | null
  participantIds: string[]
  comments: CommentView[]
  updatedAt: string
}

export interface MeetingView {
  meetingId: string
  workspaceId: string
  title: string
  agenda?: string | null
  organizerId: string
  status: MeetingStatus
  startAt: string
  endAt: string
  location?: string | null
  reminderMinutesBefore: number
  minutes?: string | null
  minutesPublishedAt?: string | null
  participantIds: string[]
  updatedAt: string
}

export interface AuditView {
  auditId: string
  actorId: string
  actionCode: string
  resourceType: string
  resourceId: string
  requestId: string
  detail?: string | null
  occurredAt: string
}

export interface CollaborationSnapshot {
  workspaces: WorkspaceView[]
  discussions: DiscussionView[]
  tasks: TaskView[]
  meetings: MeetingView[]
  auditTrail: AuditView[]
}

export interface ReminderTriggerResult {
  scannedCount: number
  sentCount: number
}

export interface MemberPayload {
  personId: string
  roleCode: WorkspaceRole
  permissions: WorkspacePermission[]
}

export interface CreateWorkspacePayload {
  code: string
  name: string
  description?: string
  visibility: 'PRIVATE' | 'TENANT'
  members: MemberPayload[]
}

export interface ReplaceMembersPayload {
  members: MemberPayload[]
}

export interface AttachmentPayload {
  attachmentId: string
  fileName: string
}

export interface CreateDiscussionPayload {
  title: string
  body: string
  mentionPersonIds: string[]
  attachments: AttachmentPayload[]
}

export interface ReplyDiscussionPayload {
  body: string
  mentionPersonIds: string[]
  attachments: AttachmentPayload[]
}

export interface CreateCommentPayload {
  workspaceId: string
  objectType: string
  objectId: string
  body: string
  mentionPersonIds: string[]
  attachments: AttachmentPayload[]
}

export interface CreateTaskPayload {
  workspaceId: string
  title: string
  description?: string
  assigneeId: string
  participantIds: string[]
  priority: CollaborationPriority
  dueAt?: string
}

export interface ChangeTaskStatusPayload {
  status: TaskStatus
  assigneeId?: string
  comment?: string
}

export interface CreateMeetingPayload {
  workspaceId: string
  title: string
  agenda?: string
  startAt: string
  endAt: string
  location?: string
  reminderMinutesBefore: number
  participantIds: string[]
}

export interface ChangeMeetingStatusPayload {
  status: MeetingStatus
  reason?: string
}

export interface PublishMeetingMinutesPayload {
  minutes: string
  mentionPersonIds: string[]
}

export interface TriggerReminderPayload {
  dueAt?: string
}
