import {
  useEffect,
  useMemo,
  useState,
  type FormEvent,
  type ReactElement,
} from 'react'
import {
  BellRing,
  CheckCircle2,
  ClipboardList,
  FileText,
  MessageSquare,
  Pin,
  Plus,
  RefreshCw,
  Send,
  ShieldCheck,
  UsersRound,
  XCircle,
} from 'lucide-react'
import {
  QueryClient,
  QueryClientProvider,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { MemberSelector } from '@/features/collaboration/components/member-selector'
import {
  addTaskComment,
  changeMeetingStatus,
  changeTaskStatus,
  closeDiscussion,
  createComment,
  createDiscussion,
  createMeeting,
  createTask,
  createWorkspace,
  deleteComment,
  getCollaborationSnapshot,
  listComments,
  markDiscussionRead,
  pinDiscussion,
  publishMeetingMinutes,
  replaceWorkspaceMembers,
  replyDiscussion,
  triggerDueMeetingReminders,
} from '@/features/collaboration/services/collaboration-service'
import type {
  AttachmentPayload,
  AuditView,
  CollaborationAttachment,
  CollaborationPriority,
  CommentView,
  CreateCommentPayload,
  DiscussionView,
  MeetingStatus,
  MeetingView,
  MemberPayload,
  TaskStatus,
  TaskView,
  WorkspaceMember,
  WorkspacePermission,
  WorkspaceRole,
  WorkspaceView,
} from '@/features/collaboration/types/collaboration'
import { formatUtcToUserTimezone } from '@/utils/format-time'
import { cn } from '@/utils/cn'

const QUERY_KEY = ['collaboration']

const ROLE_OPTIONS: Array<{ label: string; value: WorkspaceRole }> = [
  { label: '负责人', value: 'OWNER' },
  { label: '管理员', value: 'MANAGER' },
  { label: '成员', value: 'MEMBER' },
  { label: '只读', value: 'VIEWER' },
]

const PERMISSION_OPTIONS: Array<{ label: string; value: WorkspacePermission }> =
  [
    { label: '读', value: 'READ' },
    { label: '评论', value: 'COMMENT' },
    { label: '发起讨论', value: 'CREATE_DISCUSSION' },
    { label: '管理讨论', value: 'MANAGE_DISCUSSION' },
    { label: '创建任务', value: 'CREATE_TASK' },
    { label: '管理任务', value: 'MANAGE_TASK' },
    { label: '创建会议', value: 'CREATE_MEETING' },
    { label: '管理会议', value: 'MANAGE_MEETING' },
    { label: '管理成员', value: 'MANAGE_MEMBERS' },
  ]

const PRIORITY_OPTIONS: Array<{
  label: string
  value: CollaborationPriority
}> = [
  { label: '低', value: 'LOW' },
  { label: '普通', value: 'NORMAL' },
  { label: '高', value: 'HIGH' },
  { label: '紧急', value: 'URGENT' },
]

const TASK_NEXT_STATUS: Record<TaskStatus, TaskStatus[]> = {
  TODO: ['IN_PROGRESS', 'BLOCKED', 'CANCELLED'],
  IN_PROGRESS: ['BLOCKED', 'DONE', 'CANCELLED'],
  BLOCKED: ['IN_PROGRESS', 'CANCELLED'],
  DONE: [],
  CANCELLED: [],
}

const MEETING_NEXT_STATUS: Record<MeetingStatus, MeetingStatus[]> = {
  SCHEDULED: ['ONGOING', 'COMPLETED', 'CANCELLED'],
  ONGOING: ['COMPLETED', 'CANCELLED'],
  COMPLETED: [],
  CANCELLED: [],
}

const STATUS_LABELS: Record<
  TaskStatus | MeetingStatus | 'OPEN' | 'CLOSED',
  string
> = {
  TODO: '待处理',
  IN_PROGRESS: '进行中',
  BLOCKED: '阻塞',
  DONE: '完成',
  CANCELLED: '取消',
  SCHEDULED: '已安排',
  ONGOING: '进行中',
  COMPLETED: '已完成',
  OPEN: '开放',
  CLOSED: '关闭',
}

type CollaborationTab =
  | 'discussions'
  | 'tasks'
  | 'meetings'
  | 'comments'
  | 'audit'

interface WorkspaceFormState {
  code: string
  name: string
  description: string
  visibility: 'PRIVATE' | 'TENANT'
  members: MemberPayload[]
}

interface DiscussionFormState {
  title: string
  body: string
  mentionPersonIds: string[]
  attachmentsText: string
}

interface TaskFormState {
  title: string
  description: string
  assigneeIds: string[]
  participantIds: string[]
  priority: CollaborationPriority
  dueAtLocal: string
}

interface MeetingFormState {
  title: string
  agenda: string
  startAtLocal: string
  endAtLocal: string
  location: string
  reminderMinutesBefore: number
  participantIds: string[]
}

interface GenericCommentFormState {
  objectType: string
  objectId: string
  body: string
  mentionPersonIds: string[]
}

const DEFAULT_WORKSPACE_FORM: WorkspaceFormState = {
  code: '',
  name: '',
  description: '',
  visibility: 'PRIVATE',
  members: [],
}

const DEFAULT_DISCUSSION_FORM: DiscussionFormState = {
  title: '',
  body: '',
  mentionPersonIds: [],
  attachmentsText: '',
}

const DEFAULT_TASK_FORM: TaskFormState = {
  title: '',
  description: '',
  assigneeIds: [],
  participantIds: [],
  priority: 'NORMAL',
  dueAtLocal: '',
}

const DEFAULT_MEETING_FORM: MeetingFormState = {
  title: '',
  agenda: '',
  startAtLocal: '',
  endAtLocal: '',
  location: '',
  reminderMinutesBefore: 15,
  participantIds: [],
}

const DEFAULT_COMMENT_FORM: GenericCommentFormState = {
  objectType: 'TASK',
  objectId: '',
  body: '',
  mentionPersonIds: [],
}

function invalidationKey(): { queryKey: string[] } {
  return { queryKey: QUERY_KEY }
}

function formatTime(value?: string | null): string {
  return value ? formatUtcToUserTimezone(value) : '未设置'
}

function toIsoString(localDateTime: string): string | undefined {
  if (!localDateTime) {
    return undefined
  }

  return new Date(localDateTime).toISOString()
}

function parseAttachments(rawText: string): AttachmentPayload[] {
  return rawText
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [attachmentId, fileName] = line
        .split('|')
        .map((part) => part.trim())

      return {
        attachmentId,
        fileName: fileName || attachmentId,
      }
    })
}

function normalizeMembers(
  ids: string[],
  currentMembers: MemberPayload[],
): MemberPayload[] {
  const byId = new Map(
    currentMembers.map((member) => [member.personId, member]),
  )

  return ids.map((personId) => ({
    personId,
    roleCode: byId.get(personId)?.roleCode ?? 'MEMBER',
    permissions: byId.get(personId)?.permissions ?? [],
  }))
}

function hasText(value: string): boolean {
  return value.trim().length > 0
}

function TextArea({
  className,
  ...props
}: React.TextareaHTMLAttributes<HTMLTextAreaElement>): ReactElement {
  return (
    <textarea
      className={cn(
        'min-h-24 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm outline-none transition placeholder:text-slate-400 focus:border-sky-400 focus:ring-2 focus:ring-sky-100 disabled:cursor-not-allowed disabled:bg-slate-50',
        className,
      )}
      {...props}
    />
  )
}

function FieldLabel({ children }: { children: React.ReactNode }): ReactElement {
  return (
    <label className="text-sm font-medium text-slate-700">{children}</label>
  )
}

function NativeSelect<TValue extends string>({
  value,
  onChange,
  children,
  disabled,
}: {
  value: TValue
  onChange: (value: TValue) => void
  children: React.ReactNode
  disabled?: boolean
}): ReactElement {
  return (
    <select
      className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 shadow-sm outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100 disabled:bg-slate-50"
      disabled={disabled}
      onChange={(event) => onChange(event.target.value as TValue)}
      value={value}
    >
      {children}
    </select>
  )
}

function EmptyState({ text }: { text: string }): ReactElement {
  return (
    <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
      {text}
    </div>
  )
}

function StatusBadge({
  status,
}: {
  status: TaskStatus | MeetingStatus | 'OPEN' | 'CLOSED'
}): ReactElement {
  const done = status === 'DONE' || status === 'COMPLETED'
  const cancelled = status === 'CANCELLED' || status === 'CLOSED'

  return (
    <Badge
      className={cn(
        done ? 'bg-emerald-100 text-emerald-700' : undefined,
        cancelled ? 'bg-slate-100 text-slate-600' : undefined,
      )}
      variant={done ? 'success' : 'secondary'}
    >
      {STATUS_LABELS[status]}
    </Badge>
  )
}

function AttachmentList({
  attachments,
}: {
  attachments: CollaborationAttachment[]
}): ReactElement | null {
  if (!attachments.length) {
    return null
  }

  return (
    <div className="flex flex-wrap gap-2">
      {attachments.map((attachment) => (
        <Badge key={attachment.attachmentId} variant="secondary">
          {attachment.fileName}
        </Badge>
      ))}
    </div>
  )
}

function MemberRoleEditor({
  members,
  workspaceMembers,
  onChange,
}: {
  members: MemberPayload[]
  workspaceMembers: WorkspaceMember[]
  onChange: (members: MemberPayload[]) => void
}): ReactElement {
  function updateMember(personId: string, patch: Partial<MemberPayload>): void {
    onChange(
      members.map((member) =>
        member.personId === personId ? { ...member, ...patch } : member,
      ),
    )
  }

  function togglePermission(
    member: MemberPayload,
    permission: WorkspacePermission,
  ): void {
    const nextPermissions = member.permissions.includes(permission)
      ? member.permissions.filter((item) => item !== permission)
      : [...member.permissions, permission]

    updateMember(member.personId, { permissions: nextPermissions })
  }

  return (
    <div className="space-y-3">
      <MemberSelector
        label="成员选择"
        onChange={(ids) => onChange(normalizeMembers(ids, members))}
        selectedIds={members.map((member) => member.personId)}
        workspaceMembers={workspaceMembers}
      />

      {members.length ? (
        <div className="space-y-3">
          {members.map((member) => (
            <div
              className="rounded-xl border border-slate-200 bg-white p-3"
              key={member.personId}
            >
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="min-w-0">
                  <div className="truncate text-sm font-semibold text-slate-900">
                    {member.personId}
                  </div>
                  <div className="text-xs text-slate-500">角色与细粒度权限</div>
                </div>
                <NativeSelect<WorkspaceRole>
                  onChange={(roleCode) =>
                    updateMember(member.personId, { roleCode })
                  }
                  value={member.roleCode}
                >
                  {ROLE_OPTIONS.map((role) => (
                    <option key={role.value} value={role.value}>
                      {role.label}
                    </option>
                  ))}
                </NativeSelect>
              </div>
              <div className="mt-3 flex flex-wrap gap-2">
                {PERMISSION_OPTIONS.map((permission) => (
                  <label
                    className={cn(
                      'flex cursor-pointer items-center gap-1 rounded-full border px-2.5 py-1 text-xs transition',
                      member.permissions.includes(permission.value)
                        ? 'border-sky-200 bg-sky-50 text-sky-700'
                        : 'border-slate-200 bg-slate-50 text-slate-600',
                    )}
                    key={permission.value}
                  >
                    <input
                      checked={member.permissions.includes(permission.value)}
                      className="h-3 w-3"
                      onChange={() =>
                        togglePermission(member, permission.value)
                      }
                      type="checkbox"
                    />
                    {permission.label}
                  </label>
                ))}
              </div>
            </div>
          ))}
        </div>
      ) : null}
    </div>
  )
}

function WorkspacePanel({
  workspaces,
  selectedWorkspaceId,
  selectedWorkspace,
  workspaceForm,
  memberDrafts,
  isCreating,
  isReplacingMembers,
  onSelectWorkspace,
  onWorkspaceFormChange,
  onWorkspaceMembersChange,
  onCreateWorkspace,
  onMemberDraftsChange,
  onReplaceMembers,
}: {
  workspaces: WorkspaceView[]
  selectedWorkspaceId: string | null
  selectedWorkspace?: WorkspaceView
  workspaceForm: WorkspaceFormState
  memberDrafts: MemberPayload[]
  isCreating: boolean
  isReplacingMembers: boolean
  onSelectWorkspace: (workspaceId: string) => void
  onWorkspaceFormChange: (patch: Partial<WorkspaceFormState>) => void
  onWorkspaceMembersChange: (members: MemberPayload[]) => void
  onCreateWorkspace: (event: FormEvent<HTMLFormElement>) => void
  onMemberDraftsChange: (members: MemberPayload[]) => void
  onReplaceMembers: () => void
}): ReactElement {
  return (
    <div className="grid gap-5 xl:grid-cols-[minmax(260px,360px)_minmax(0,1fr)]">
      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="mb-3 flex items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-slate-950">协同空间</h2>
            <p className="text-sm text-slate-500">按租户隔离的空间和成员权限</p>
          </div>
          <Badge variant="secondary">{workspaces.length}</Badge>
        </div>

        <div className="space-y-2">
          {workspaces.length ? (
            workspaces.map((workspace) => (
              <button
                className={cn(
                  'w-full rounded-xl border px-3 py-3 text-left transition',
                  workspace.workspaceId === selectedWorkspaceId
                    ? 'border-sky-300 bg-sky-50'
                    : 'border-slate-200 bg-white hover:border-slate-300',
                )}
                key={workspace.workspaceId}
                onClick={() => onSelectWorkspace(workspace.workspaceId)}
                type="button"
              >
                <div className="flex items-center justify-between gap-3">
                  <span className="truncate text-sm font-semibold text-slate-900">
                    {workspace.name}
                  </span>
                  <Badge variant="secondary">{workspace.visibility}</Badge>
                </div>
                <div className="mt-1 truncate text-xs text-slate-500">
                  {workspace.code}
                </div>
              </button>
            ))
          ) : (
            <EmptyState text="当前租户暂无协同空间。" />
          )}
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="grid gap-5 lg:grid-cols-2">
          <form className="space-y-3" onSubmit={onCreateWorkspace}>
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
              <Plus className="h-4 w-4 text-sky-600" />
              新建空间
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="space-y-1">
                <FieldLabel>空间编码</FieldLabel>
                <Input
                  onChange={(event) =>
                    onWorkspaceFormChange({ code: event.target.value })
                  }
                  required
                  value={workspaceForm.code}
                />
              </div>
              <div className="space-y-1">
                <FieldLabel>空间名称</FieldLabel>
                <Input
                  onChange={(event) =>
                    onWorkspaceFormChange({ name: event.target.value })
                  }
                  required
                  value={workspaceForm.name}
                />
              </div>
            </div>
            <div className="space-y-1">
              <FieldLabel>描述</FieldLabel>
              <TextArea
                onChange={(event) =>
                  onWorkspaceFormChange({ description: event.target.value })
                }
                value={workspaceForm.description}
              />
            </div>
            <div className="space-y-1">
              <FieldLabel>可见性</FieldLabel>
              <NativeSelect<'PRIVATE' | 'TENANT'>
                onChange={(visibility) => onWorkspaceFormChange({ visibility })}
                value={workspaceForm.visibility}
              >
                <option value="PRIVATE">私有</option>
                <option value="TENANT">租户可见</option>
              </NativeSelect>
            </div>
            <MemberRoleEditor
              members={workspaceForm.members}
              onChange={onWorkspaceMembersChange}
              workspaceMembers={selectedWorkspace?.members ?? []}
            />
            <Button disabled={isCreating} type="submit">
              <Plus className="h-4 w-4" />
              创建空间
            </Button>
          </form>

          <div className="space-y-3">
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
              <ShieldCheck className="h-4 w-4 text-sky-600" />
              当前空间成员
            </div>
            {selectedWorkspace ? (
              <>
                <div className="grid gap-2 sm:grid-cols-2">
                  <div className="rounded-xl bg-slate-50 p-3">
                    <div className="text-xs text-slate-500">负责人</div>
                    <div className="mt-1 truncate text-sm font-semibold text-slate-900">
                      {selectedWorkspace.ownerId}
                    </div>
                  </div>
                  <div className="rounded-xl bg-slate-50 p-3">
                    <div className="text-xs text-slate-500">成员数</div>
                    <div className="mt-1 text-sm font-semibold text-slate-900">
                      {selectedWorkspace.members.length}
                    </div>
                  </div>
                </div>
                <MemberRoleEditor
                  members={memberDrafts}
                  onChange={onMemberDraftsChange}
                  workspaceMembers={selectedWorkspace.members}
                />
                <Button
                  disabled={isReplacingMembers}
                  onClick={onReplaceMembers}
                  type="button"
                >
                  <UsersRound className="h-4 w-4" />
                  保存成员和权限
                </Button>
              </>
            ) : (
              <EmptyState text="请选择或创建一个协同空间。" />
            )}
          </div>
        </div>
      </section>
    </div>
  )
}

function DiscussionPanel({
  workspace,
  discussions,
  selectedDiscussion,
  discussionForm,
  replyText,
  replyMentionPersonIds,
  isCreating,
  isReplying,
  onDiscussionFormChange,
  onCreateDiscussion,
  onSelectDiscussion,
  onReplyTextChange,
  onReplyMentionPersonIdsChange,
  onReplyDiscussion,
  onMarkRead,
  onPin,
  onClose,
}: {
  workspace?: WorkspaceView
  discussions: DiscussionView[]
  selectedDiscussion?: DiscussionView
  discussionForm: DiscussionFormState
  replyText: string
  replyMentionPersonIds: string[]
  isCreating: boolean
  isReplying: boolean
  onDiscussionFormChange: (patch: Partial<DiscussionFormState>) => void
  onCreateDiscussion: (event: FormEvent<HTMLFormElement>) => void
  onSelectDiscussion: (discussionId: string) => void
  onReplyTextChange: (value: string) => void
  onReplyMentionPersonIdsChange: (personIds: string[]) => void
  onReplyDiscussion: () => void
  onMarkRead: () => void
  onPin: (pinned: boolean) => void
  onClose: () => void
}): ReactElement {
  return (
    <div className="grid gap-5 xl:grid-cols-[minmax(280px,380px)_minmax(0,1fr)]">
      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="mb-3 flex items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-slate-950">讨论主题</h2>
          <Badge variant="secondary">{discussions.length}</Badge>
        </div>
        <div className="space-y-2">
          {discussions.length ? (
            discussions.map((discussion) => (
              <button
                className={cn(
                  'w-full rounded-xl border px-3 py-3 text-left transition',
                  discussion.discussionId === selectedDiscussion?.discussionId
                    ? 'border-sky-300 bg-sky-50'
                    : 'border-slate-200 hover:border-slate-300',
                )}
                key={discussion.discussionId}
                onClick={() => onSelectDiscussion(discussion.discussionId)}
                type="button"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="truncate text-sm font-semibold text-slate-900">
                    {discussion.title}
                  </span>
                  <div className="flex shrink-0 items-center gap-1">
                    {discussion.pinned ? <Pin className="h-3.5 w-3.5" /> : null}
                    <StatusBadge status={discussion.status} />
                  </div>
                </div>
                <div className="mt-1 line-clamp-2 text-xs text-slate-500">
                  {discussion.body}
                </div>
              </button>
            ))
          ) : (
            <EmptyState text="当前空间暂无讨论。" />
          )}
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(260px,360px)]">
          <div className="space-y-4">
            {selectedDiscussion ? (
              <>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <MessageSquare className="h-4 w-4 text-sky-600" />
                      <h3 className="text-lg font-semibold text-slate-950">
                        {selectedDiscussion.title}
                      </h3>
                    </div>
                    <p className="mt-1 text-sm text-slate-500">
                      {selectedDiscussion.authorId} ·{' '}
                      {formatTime(selectedDiscussion.updatedAt)}
                    </p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Button
                      onClick={onMarkRead}
                      size="sm"
                      type="button"
                      variant="outline"
                    >
                      <CheckCircle2 className="h-4 w-4" />
                      已读
                    </Button>
                    <Button
                      onClick={() => onPin(!selectedDiscussion.pinned)}
                      size="sm"
                      type="button"
                      variant="outline"
                    >
                      <Pin className="h-4 w-4" />
                      {selectedDiscussion.pinned ? '取消置顶' : '置顶'}
                    </Button>
                    <Button
                      disabled={selectedDiscussion.status === 'CLOSED'}
                      onClick={onClose}
                      size="sm"
                      type="button"
                      variant="outline"
                    >
                      <XCircle className="h-4 w-4" />
                      关闭
                    </Button>
                  </div>
                </div>
                <p className="whitespace-pre-wrap rounded-xl bg-slate-50 p-3 text-sm text-slate-700">
                  {selectedDiscussion.body}
                </p>
                <AttachmentList attachments={selectedDiscussion.attachments} />
                <div className="space-y-2">
                  <div className="text-sm font-semibold text-slate-900">
                    回复
                  </div>
                  {selectedDiscussion.replies.length ? (
                    selectedDiscussion.replies.map((reply) => (
                      <div
                        className="rounded-xl border border-slate-200 px-3 py-2"
                        key={reply.replyId}
                      >
                        <div className="text-xs text-slate-500">
                          {reply.authorId} · {formatTime(reply.createdAt)}
                        </div>
                        <div className="mt-1 whitespace-pre-wrap text-sm text-slate-700">
                          {reply.deleted ? '该回复已删除' : reply.body}
                        </div>
                      </div>
                    ))
                  ) : (
                    <EmptyState text="暂无回复。" />
                  )}
                </div>
                <div className="space-y-2">
                  <FieldLabel>回复内容</FieldLabel>
                  <TextArea
                    onChange={(event) => onReplyTextChange(event.target.value)}
                    value={replyText}
                  />
                  <MemberSelector
                    label="回复提及"
                    onChange={onReplyMentionPersonIdsChange}
                    selectedIds={replyMentionPersonIds}
                    workspaceMembers={workspace?.members}
                  />
                  <Button
                    disabled={isReplying || !hasText(replyText)}
                    onClick={onReplyDiscussion}
                    type="button"
                  >
                    <Send className="h-4 w-4" />
                    发送回复
                  </Button>
                </div>
              </>
            ) : (
              <EmptyState text="请选择一个讨论查看详情。" />
            )}
          </div>

          <form className="space-y-3" onSubmit={onCreateDiscussion}>
            <div className="text-sm font-semibold text-slate-900">新讨论</div>
            <Input
              disabled={!workspace}
              onChange={(event) =>
                onDiscussionFormChange({ title: event.target.value })
              }
              placeholder="主题标题"
              required
              value={discussionForm.title}
            />
            <TextArea
              disabled={!workspace}
              onChange={(event) =>
                onDiscussionFormChange({ body: event.target.value })
              }
              placeholder="讨论内容"
              required
              value={discussionForm.body}
            />
            <MemberSelector
              disabled={!workspace}
              label="提及成员"
              onChange={(mentionPersonIds) =>
                onDiscussionFormChange({ mentionPersonIds })
              }
              selectedIds={discussionForm.mentionPersonIds}
              workspaceMembers={workspace?.members}
            />
            <TextArea
              disabled={!workspace}
              onChange={(event) =>
                onDiscussionFormChange({ attachmentsText: event.target.value })
              }
              placeholder="附件链接，每行 attachmentId|文件名"
              value={discussionForm.attachmentsText}
            />
            <Button disabled={isCreating || !workspace} type="submit">
              <Plus className="h-4 w-4" />
              发布讨论
            </Button>
          </form>
        </div>
      </section>
    </div>
  )
}

function TaskPanel({
  workspace,
  tasks,
  selectedTask,
  taskForm,
  taskStatusComment,
  taskCommentText,
  isCreating,
  isChanging,
  isCommenting,
  onTaskFormChange,
  onCreateTask,
  onSelectTask,
  onTaskStatusCommentChange,
  onChangeStatus,
  onTaskCommentTextChange,
  onAddTaskComment,
  onDeleteComment,
}: {
  workspace?: WorkspaceView
  tasks: TaskView[]
  selectedTask?: TaskView
  taskForm: TaskFormState
  taskStatusComment: string
  taskCommentText: string
  isCreating: boolean
  isChanging: boolean
  isCommenting: boolean
  onTaskFormChange: (patch: Partial<TaskFormState>) => void
  onCreateTask: (event: FormEvent<HTMLFormElement>) => void
  onSelectTask: (taskId: string) => void
  onTaskStatusCommentChange: (value: string) => void
  onChangeStatus: (status: TaskStatus) => void
  onTaskCommentTextChange: (value: string) => void
  onAddTaskComment: () => void
  onDeleteComment: (commentId: string) => void
}): ReactElement {
  const nextStatuses = selectedTask ? TASK_NEXT_STATUS[selectedTask.status] : []

  return (
    <div className="grid gap-5 xl:grid-cols-[minmax(280px,380px)_minmax(0,1fr)]">
      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="mb-3 flex items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-slate-950">协同任务</h2>
          <Badge variant="secondary">{tasks.length}</Badge>
        </div>
        <div className="space-y-2">
          {tasks.length ? (
            tasks.map((task) => (
              <button
                className={cn(
                  'w-full rounded-xl border px-3 py-3 text-left transition',
                  task.taskId === selectedTask?.taskId
                    ? 'border-sky-300 bg-sky-50'
                    : 'border-slate-200 hover:border-slate-300',
                )}
                key={task.taskId}
                onClick={() => onSelectTask(task.taskId)}
                type="button"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="truncate text-sm font-semibold text-slate-900">
                    {task.title}
                  </span>
                  <StatusBadge status={task.status} />
                </div>
                <div className="mt-1 text-xs text-slate-500">
                  {task.assigneeId} · {formatTime(task.dueAt)}
                </div>
              </button>
            ))
          ) : (
            <EmptyState text="当前空间暂无协同任务。" />
          )}
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(260px,360px)]">
          <div className="space-y-4">
            {selectedTask ? (
              <>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <ClipboardList className="h-4 w-4 text-sky-600" />
                      <h3 className="text-lg font-semibold text-slate-950">
                        {selectedTask.title}
                      </h3>
                    </div>
                    <p className="mt-1 text-sm text-slate-500">
                      负责人 {selectedTask.assigneeId} · 截止{' '}
                      {formatTime(selectedTask.dueAt)}
                    </p>
                  </div>
                  <StatusBadge status={selectedTask.status} />
                </div>
                <div className="grid gap-3 sm:grid-cols-3">
                  <div className="rounded-xl bg-slate-50 p-3">
                    <div className="text-xs text-slate-500">优先级</div>
                    <div className="mt-1 text-sm font-semibold">
                      {selectedTask.priority}
                    </div>
                  </div>
                  <div className="rounded-xl bg-slate-50 p-3">
                    <div className="text-xs text-slate-500">参与人</div>
                    <div className="mt-1 text-sm font-semibold">
                      {selectedTask.participantIds.length}
                    </div>
                  </div>
                  <div className="rounded-xl bg-slate-50 p-3">
                    <div className="text-xs text-slate-500">更新</div>
                    <div className="mt-1 text-sm font-semibold">
                      {formatTime(selectedTask.updatedAt)}
                    </div>
                  </div>
                </div>
                <p className="whitespace-pre-wrap rounded-xl bg-slate-50 p-3 text-sm text-slate-700">
                  {selectedTask.description || '无任务描述'}
                </p>
                <div className="space-y-2">
                  <FieldLabel>状态备注</FieldLabel>
                  <Input
                    onChange={(event) =>
                      onTaskStatusCommentChange(event.target.value)
                    }
                    value={taskStatusComment}
                  />
                  <div className="flex flex-wrap gap-2">
                    {nextStatuses.length ? (
                      nextStatuses.map((status) => (
                        <Button
                          disabled={isChanging}
                          key={status}
                          onClick={() => onChangeStatus(status)}
                          size="sm"
                          type="button"
                          variant="outline"
                        >
                          {STATUS_LABELS[status]}
                        </Button>
                      ))
                    ) : (
                      <Badge variant="secondary">无后续状态</Badge>
                    )}
                  </div>
                </div>
                <div className="space-y-2">
                  <div className="text-sm font-semibold text-slate-900">
                    任务评论
                  </div>
                  {selectedTask.comments.length ? (
                    selectedTask.comments.map((comment) => (
                      <div
                        className="rounded-xl border border-slate-200 px-3 py-2"
                        key={comment.commentId}
                      >
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-xs text-slate-500">
                            {comment.authorId} · {formatTime(comment.createdAt)}
                          </span>
                          <Button
                            onClick={() => onDeleteComment(comment.commentId)}
                            size="sm"
                            type="button"
                            variant="ghost"
                          >
                            删除
                          </Button>
                        </div>
                        <div className="mt-1 whitespace-pre-wrap text-sm text-slate-700">
                          {comment.deleted ? '该评论已删除' : comment.body}
                        </div>
                      </div>
                    ))
                  ) : (
                    <EmptyState text="暂无任务评论。" />
                  )}
                  <TextArea
                    onChange={(event) =>
                      onTaskCommentTextChange(event.target.value)
                    }
                    value={taskCommentText}
                  />
                  <Button
                    disabled={isCommenting || !hasText(taskCommentText)}
                    onClick={onAddTaskComment}
                    type="button"
                  >
                    <Send className="h-4 w-4" />
                    添加评论
                  </Button>
                </div>
              </>
            ) : (
              <EmptyState text="请选择一个任务查看详情。" />
            )}
          </div>

          <form className="space-y-3" onSubmit={onCreateTask}>
            <div className="text-sm font-semibold text-slate-900">新任务</div>
            <Input
              disabled={!workspace}
              onChange={(event) =>
                onTaskFormChange({ title: event.target.value })
              }
              placeholder="任务标题"
              required
              value={taskForm.title}
            />
            <TextArea
              disabled={!workspace}
              onChange={(event) =>
                onTaskFormChange({ description: event.target.value })
              }
              placeholder="任务描述"
              value={taskForm.description}
            />
            <MemberSelector
              disabled={!workspace}
              label="负责人"
              onChange={(ids) =>
                onTaskFormChange({ assigneeIds: ids.slice(-1) })
              }
              selectedIds={taskForm.assigneeIds}
              workspaceMembers={workspace?.members}
            />
            <MemberSelector
              disabled={!workspace}
              label="参与人"
              onChange={(participantIds) =>
                onTaskFormChange({ participantIds })
              }
              selectedIds={taskForm.participantIds}
              workspaceMembers={workspace?.members}
            />
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="space-y-1">
                <FieldLabel>优先级</FieldLabel>
                <NativeSelect<CollaborationPriority>
                  disabled={!workspace}
                  onChange={(priority) => onTaskFormChange({ priority })}
                  value={taskForm.priority}
                >
                  {PRIORITY_OPTIONS.map((priority) => (
                    <option key={priority.value} value={priority.value}>
                      {priority.label}
                    </option>
                  ))}
                </NativeSelect>
              </div>
              <div className="space-y-1">
                <FieldLabel>截止时间</FieldLabel>
                <Input
                  disabled={!workspace}
                  onChange={(event) =>
                    onTaskFormChange({ dueAtLocal: event.target.value })
                  }
                  type="datetime-local"
                  value={taskForm.dueAtLocal}
                />
              </div>
            </div>
            <Button disabled={isCreating || !workspace} type="submit">
              <Plus className="h-4 w-4" />
              创建任务
            </Button>
          </form>
        </div>
      </section>
    </div>
  )
}

function MeetingPanel({
  workspace,
  meetings,
  selectedMeeting,
  meetingForm,
  minutesText,
  isCreating,
  isChanging,
  isPublishingMinutes,
  isTriggeringReminder,
  onMeetingFormChange,
  onCreateMeeting,
  onSelectMeeting,
  onChangeStatus,
  onMinutesTextChange,
  onPublishMinutes,
  onTriggerReminders,
}: {
  workspace?: WorkspaceView
  meetings: MeetingView[]
  selectedMeeting?: MeetingView
  meetingForm: MeetingFormState
  minutesText: string
  isCreating: boolean
  isChanging: boolean
  isPublishingMinutes: boolean
  isTriggeringReminder: boolean
  onMeetingFormChange: (patch: Partial<MeetingFormState>) => void
  onCreateMeeting: (event: FormEvent<HTMLFormElement>) => void
  onSelectMeeting: (meetingId: string) => void
  onChangeStatus: (status: MeetingStatus) => void
  onMinutesTextChange: (value: string) => void
  onPublishMinutes: () => void
  onTriggerReminders: () => void
}): ReactElement {
  const nextStatuses = selectedMeeting
    ? MEETING_NEXT_STATUS[selectedMeeting.status]
    : []

  return (
    <div className="grid gap-5 xl:grid-cols-[minmax(280px,380px)_minmax(0,1fr)]">
      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="mb-3 flex items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-slate-950">会议日程</h2>
          <Button
            disabled={isTriggeringReminder}
            onClick={onTriggerReminders}
            size="sm"
            type="button"
            variant="outline"
          >
            <BellRing className="h-4 w-4" />
            触发提醒
          </Button>
        </div>
        <div className="space-y-2">
          {meetings.length ? (
            meetings.map((meeting) => (
              <button
                className={cn(
                  'w-full rounded-xl border px-3 py-3 text-left transition',
                  meeting.meetingId === selectedMeeting?.meetingId
                    ? 'border-sky-300 bg-sky-50'
                    : 'border-slate-200 hover:border-slate-300',
                )}
                key={meeting.meetingId}
                onClick={() => onSelectMeeting(meeting.meetingId)}
                type="button"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="truncate text-sm font-semibold text-slate-900">
                    {meeting.title}
                  </span>
                  <StatusBadge status={meeting.status} />
                </div>
                <div className="mt-1 text-xs text-slate-500">
                  {formatTime(meeting.startAt)} · {meeting.location || '线上'}
                </div>
              </button>
            ))
          ) : (
            <EmptyState text="当前空间暂无会议。" />
          )}
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(260px,360px)]">
          <div className="space-y-4">
            {selectedMeeting ? (
              <>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <BellRing className="h-4 w-4 text-sky-600" />
                      <h3 className="text-lg font-semibold text-slate-950">
                        {selectedMeeting.title}
                      </h3>
                    </div>
                    <p className="mt-1 text-sm text-slate-500">
                      {formatTime(selectedMeeting.startAt)} -{' '}
                      {formatTime(selectedMeeting.endAt)}
                    </p>
                  </div>
                  <StatusBadge status={selectedMeeting.status} />
                </div>
                <div className="grid gap-3 sm:grid-cols-3">
                  <div className="rounded-xl bg-slate-50 p-3">
                    <div className="text-xs text-slate-500">组织人</div>
                    <div className="mt-1 truncate text-sm font-semibold">
                      {selectedMeeting.organizerId}
                    </div>
                  </div>
                  <div className="rounded-xl bg-slate-50 p-3">
                    <div className="text-xs text-slate-500">参与人</div>
                    <div className="mt-1 text-sm font-semibold">
                      {selectedMeeting.participantIds.length}
                    </div>
                  </div>
                  <div className="rounded-xl bg-slate-50 p-3">
                    <div className="text-xs text-slate-500">提前提醒</div>
                    <div className="mt-1 text-sm font-semibold">
                      {selectedMeeting.reminderMinutesBefore} 分钟
                    </div>
                  </div>
                </div>
                <p className="whitespace-pre-wrap rounded-xl bg-slate-50 p-3 text-sm text-slate-700">
                  {selectedMeeting.agenda || '无议程'}
                </p>
                <div className="flex flex-wrap gap-2">
                  {nextStatuses.length ? (
                    nextStatuses.map((status) => (
                      <Button
                        disabled={isChanging}
                        key={status}
                        onClick={() => onChangeStatus(status)}
                        size="sm"
                        type="button"
                        variant="outline"
                      >
                        {STATUS_LABELS[status]}
                      </Button>
                    ))
                  ) : (
                    <Badge variant="secondary">无后续状态</Badge>
                  )}
                </div>
                <div className="space-y-2">
                  <div className="text-sm font-semibold text-slate-900">
                    会议纪要
                  </div>
                  <p className="whitespace-pre-wrap rounded-xl border border-slate-200 p-3 text-sm text-slate-700">
                    {selectedMeeting.minutes || '未发布纪要'}
                  </p>
                  <TextArea
                    onChange={(event) =>
                      onMinutesTextChange(event.target.value)
                    }
                    value={minutesText}
                  />
                  <Button
                    disabled={isPublishingMinutes || !hasText(minutesText)}
                    onClick={onPublishMinutes}
                    type="button"
                  >
                    <FileText className="h-4 w-4" />
                    发布纪要
                  </Button>
                </div>
              </>
            ) : (
              <EmptyState text="请选择一个会议查看详情。" />
            )}
          </div>

          <form className="space-y-3" onSubmit={onCreateMeeting}>
            <div className="text-sm font-semibold text-slate-900">新会议</div>
            <Input
              disabled={!workspace}
              onChange={(event) =>
                onMeetingFormChange({ title: event.target.value })
              }
              placeholder="会议标题"
              required
              value={meetingForm.title}
            />
            <TextArea
              disabled={!workspace}
              onChange={(event) =>
                onMeetingFormChange({ agenda: event.target.value })
              }
              placeholder="会议议程"
              value={meetingForm.agenda}
            />
            <div className="grid gap-3 sm:grid-cols-2">
              <Input
                disabled={!workspace}
                onChange={(event) =>
                  onMeetingFormChange({ startAtLocal: event.target.value })
                }
                required
                type="datetime-local"
                value={meetingForm.startAtLocal}
              />
              <Input
                disabled={!workspace}
                onChange={(event) =>
                  onMeetingFormChange({ endAtLocal: event.target.value })
                }
                required
                type="datetime-local"
                value={meetingForm.endAtLocal}
              />
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <Input
                disabled={!workspace}
                onChange={(event) =>
                  onMeetingFormChange({ location: event.target.value })
                }
                placeholder="地点"
                value={meetingForm.location}
              />
              <Input
                disabled={!workspace}
                min={0}
                onChange={(event) =>
                  onMeetingFormChange({
                    reminderMinutesBefore: Number(event.target.value),
                  })
                }
                type="number"
                value={meetingForm.reminderMinutesBefore}
              />
            </div>
            <MemberSelector
              disabled={!workspace}
              label="参与人"
              onChange={(participantIds) =>
                onMeetingFormChange({ participantIds })
              }
              selectedIds={meetingForm.participantIds}
              workspaceMembers={workspace?.members}
            />
            <Button disabled={isCreating || !workspace} type="submit">
              <Plus className="h-4 w-4" />
              创建会议
            </Button>
          </form>
        </div>
      </section>
    </div>
  )
}

function CommentPanel({
  workspace,
  comments,
  form,
  isLoading,
  isCreating,
  isDeleting,
  onFormChange,
  onCreateComment,
  onDeleteComment,
}: {
  workspace?: WorkspaceView
  comments: CommentView[]
  form: GenericCommentFormState
  isLoading: boolean
  isCreating: boolean
  isDeleting: boolean
  onFormChange: (patch: Partial<GenericCommentFormState>) => void
  onCreateComment: (event: FormEvent<HTMLFormElement>) => void
  onDeleteComment: (commentId: string) => void
}): ReactElement {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="grid gap-5 lg:grid-cols-[minmax(260px,360px)_minmax(0,1fr)]">
        <form className="space-y-3" onSubmit={onCreateComment}>
          <div>
            <h2 className="text-lg font-semibold text-slate-950">业务评论</h2>
            <p className="text-sm text-slate-500">
              评论可挂到任务、会议、讨论或其他业务对象
            </p>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <Input
              disabled={!workspace}
              onChange={(event) =>
                onFormChange({ objectType: event.target.value.toUpperCase() })
              }
              placeholder="对象类型"
              required
              value={form.objectType}
            />
            <Input
              disabled={!workspace}
              onChange={(event) =>
                onFormChange({ objectId: event.target.value })
              }
              placeholder="对象 ID"
              required
              value={form.objectId}
            />
          </div>
          <TextArea
            disabled={!workspace}
            onChange={(event) => onFormChange({ body: event.target.value })}
            placeholder="评论内容"
            required
            value={form.body}
          />
          <MemberSelector
            disabled={!workspace}
            label="提及成员"
            onChange={(mentionPersonIds) => onFormChange({ mentionPersonIds })}
            selectedIds={form.mentionPersonIds}
            workspaceMembers={workspace?.members}
          />
          <Button disabled={isCreating || !workspace} type="submit">
            <Send className="h-4 w-4" />
            提交评论
          </Button>
        </form>

        <div className="space-y-3">
          <div className="flex items-center justify-between gap-3">
            <div className="text-sm font-semibold text-slate-900">评论列表</div>
            {isLoading ? <Badge variant="secondary">加载中</Badge> : null}
          </div>
          {comments.length ? (
            comments.map((comment) => (
              <div
                className="rounded-xl border border-slate-200 px-3 py-2"
                key={comment.commentId}
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="text-xs text-slate-500">
                    {comment.authorId} · {formatTime(comment.createdAt)}
                  </span>
                  <Button
                    disabled={isDeleting}
                    onClick={() => onDeleteComment(comment.commentId)}
                    size="sm"
                    type="button"
                    variant="ghost"
                  >
                    删除
                  </Button>
                </div>
                <div className="mt-1 whitespace-pre-wrap text-sm text-slate-700">
                  {comment.deleted ? '该评论已删除' : comment.body}
                </div>
              </div>
            ))
          ) : (
            <EmptyState text="输入对象类型和对象 ID 后查询评论。" />
          )}
        </div>
      </div>
    </section>
  )
}

function AuditPanel({ auditTrail }: { auditTrail: AuditView[] }): ReactElement {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-center gap-2">
        <ShieldCheck className="h-4 w-4 text-sky-600" />
        <h2 className="text-lg font-semibold text-slate-950">协同审计</h2>
      </div>
      {auditTrail.length ? (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[760px] text-left text-sm">
            <thead className="border-b border-slate-200 text-xs text-slate-500">
              <tr>
                <th className="py-2 pr-3 font-medium">时间</th>
                <th className="py-2 pr-3 font-medium">动作</th>
                <th className="py-2 pr-3 font-medium">对象</th>
                <th className="py-2 pr-3 font-medium">人员</th>
                <th className="py-2 pr-3 font-medium">requestId</th>
                <th className="py-2 pr-3 font-medium">详情</th>
              </tr>
            </thead>
            <tbody>
              {auditTrail.map((audit) => (
                <tr
                  className="border-b border-slate-100 last:border-b-0"
                  key={audit.auditId}
                >
                  <td className="py-2 pr-3 text-slate-600">
                    {formatTime(audit.occurredAt)}
                  </td>
                  <td className="py-2 pr-3 font-medium text-slate-900">
                    {audit.actionCode}
                  </td>
                  <td className="py-2 pr-3 text-slate-600">
                    {audit.resourceType}:{audit.resourceId}
                  </td>
                  <td className="py-2 pr-3 text-slate-600">{audit.actorId}</td>
                  <td className="py-2 pr-3 text-slate-600">
                    {audit.requestId}
                  </td>
                  <td className="py-2 pr-3 text-slate-600">
                    {audit.detail ?? '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <EmptyState text="暂无协同审计记录。" />
      )}
    </section>
  )
}

function CollaborationContent(): ReactElement {
  const queryClient = useQueryClient()
  const [activeTab, setActiveTab] = useState<CollaborationTab>('discussions')
  const [selectedWorkspaceId, setSelectedWorkspaceId] = useState<string | null>(
    null,
  )
  const [selectedDiscussionId, setSelectedDiscussionId] = useState<
    string | null
  >(null)
  const [selectedTaskId, setSelectedTaskId] = useState<string | null>(null)
  const [selectedMeetingId, setSelectedMeetingId] = useState<string | null>(
    null,
  )
  const [workspaceForm, setWorkspaceForm] = useState(DEFAULT_WORKSPACE_FORM)
  const [memberDrafts, setMemberDrafts] = useState<MemberPayload[]>([])
  const [discussionForm, setDiscussionForm] = useState(DEFAULT_DISCUSSION_FORM)
  const [replyText, setReplyText] = useState('')
  const [replyMentionPersonIds, setReplyMentionPersonIds] = useState<string[]>(
    [],
  )
  const [taskForm, setTaskForm] = useState(DEFAULT_TASK_FORM)
  const [taskStatusComment, setTaskStatusComment] = useState('')
  const [taskCommentText, setTaskCommentText] = useState('')
  const [meetingForm, setMeetingForm] = useState(DEFAULT_MEETING_FORM)
  const [minutesText, setMinutesText] = useState('')
  const [genericCommentForm, setGenericCommentForm] =
    useState(DEFAULT_COMMENT_FORM)

  const snapshotQuery = useQuery({
    queryKey: QUERY_KEY,
    queryFn: getCollaborationSnapshot,
  })

  const snapshot = snapshotQuery.data
  const workspaces = useMemo(
    () => snapshot?.workspaces ?? [],
    [snapshot?.workspaces],
  )
  const selectedWorkspace = workspaces.find(
    (workspace) => workspace.workspaceId === selectedWorkspaceId,
  )

  const discussions = useMemo(
    () =>
      (snapshot?.discussions ?? []).filter(
        (discussion) =>
          discussion.workspaceId === selectedWorkspace?.workspaceId,
      ),
    [selectedWorkspace?.workspaceId, snapshot?.discussions],
  )
  const tasks = useMemo(
    () =>
      (snapshot?.tasks ?? []).filter(
        (task) => task.workspaceId === selectedWorkspace?.workspaceId,
      ),
    [selectedWorkspace?.workspaceId, snapshot?.tasks],
  )
  const meetings = useMemo(
    () =>
      (snapshot?.meetings ?? []).filter(
        (meeting) => meeting.workspaceId === selectedWorkspace?.workspaceId,
      ),
    [selectedWorkspace?.workspaceId, snapshot?.meetings],
  )
  const selectedDiscussion = discussions.find(
    (discussion) => discussion.discussionId === selectedDiscussionId,
  )
  const selectedTask = tasks.find((task) => task.taskId === selectedTaskId)
  const selectedMeeting = meetings.find(
    (meeting) => meeting.meetingId === selectedMeetingId,
  )

  const commentsQuery = useQuery({
    queryKey: [
      ...QUERY_KEY,
      'comments',
      genericCommentForm.objectType,
      genericCommentForm.objectId,
    ],
    queryFn: () =>
      listComments(genericCommentForm.objectType, genericCommentForm.objectId),
    enabled:
      hasText(genericCommentForm.objectType) &&
      hasText(genericCommentForm.objectId),
  })

  const invalidateCollaboration = (): Promise<void> =>
    queryClient.invalidateQueries(invalidationKey())

  const createWorkspaceMutation = useMutation({
    mutationFn: createWorkspace,
    onSuccess: async (workspace) => {
      setSelectedWorkspaceId(workspace.workspaceId)
      setWorkspaceForm(DEFAULT_WORKSPACE_FORM)
      await invalidateCollaboration()
    },
  })
  const replaceMembersMutation = useMutation({
    mutationFn: () =>
      replaceWorkspaceMembers(selectedWorkspace?.workspaceId ?? '', {
        members: memberDrafts,
      }),
    onSuccess: async () => {
      await invalidateCollaboration()
    },
  })
  const createDiscussionMutation = useMutation({
    mutationFn: () =>
      createDiscussion(selectedWorkspace?.workspaceId ?? '', {
        title: discussionForm.title.trim(),
        body: discussionForm.body.trim(),
        mentionPersonIds: discussionForm.mentionPersonIds,
        attachments: parseAttachments(discussionForm.attachmentsText),
      }),
    onSuccess: async (discussion) => {
      setSelectedDiscussionId(discussion.discussionId)
      setDiscussionForm(DEFAULT_DISCUSSION_FORM)
      await invalidateCollaboration()
    },
  })
  const replyDiscussionMutation = useMutation({
    mutationFn: () =>
      replyDiscussion(selectedDiscussion?.discussionId ?? '', {
        body: replyText.trim(),
        mentionPersonIds: replyMentionPersonIds,
        attachments: [],
      }),
    onSuccess: async () => {
      setReplyText('')
      setReplyMentionPersonIds([])
      await invalidateCollaboration()
    },
  })
  const markReadMutation = useMutation({
    mutationFn: () =>
      markDiscussionRead(selectedDiscussion?.discussionId ?? ''),
    onSuccess: invalidateCollaboration,
  })
  const pinDiscussionMutation = useMutation({
    mutationFn: (pinned: boolean) =>
      pinDiscussion(selectedDiscussion?.discussionId ?? '', pinned),
    onSuccess: invalidateCollaboration,
  })
  const closeDiscussionMutation = useMutation({
    mutationFn: () => closeDiscussion(selectedDiscussion?.discussionId ?? ''),
    onSuccess: invalidateCollaboration,
  })
  const createTaskMutation = useMutation({
    mutationFn: () =>
      createTask({
        workspaceId: selectedWorkspace?.workspaceId ?? '',
        title: taskForm.title.trim(),
        description: taskForm.description.trim(),
        assigneeId: taskForm.assigneeIds[0],
        participantIds: taskForm.participantIds,
        priority: taskForm.priority,
        dueAt: toIsoString(taskForm.dueAtLocal),
      }),
    onSuccess: async (task) => {
      setSelectedTaskId(task.taskId)
      setTaskForm(DEFAULT_TASK_FORM)
      await invalidateCollaboration()
    },
  })
  const changeTaskStatusMutation = useMutation({
    mutationFn: (status: TaskStatus) =>
      changeTaskStatus(selectedTask?.taskId ?? '', {
        status,
        comment: taskStatusComment.trim() || undefined,
      }),
    onSuccess: async () => {
      setTaskStatusComment('')
      await invalidateCollaboration()
    },
  })
  const addTaskCommentMutation = useMutation({
    mutationFn: () =>
      addTaskComment(selectedTask?.taskId ?? '', {
        workspaceId: selectedWorkspace?.workspaceId ?? '',
        objectType: 'TASK',
        objectId: selectedTask?.taskId ?? '',
        body: taskCommentText.trim(),
        mentionPersonIds: [],
        attachments: [],
      }),
    onSuccess: async () => {
      setTaskCommentText('')
      await invalidateCollaboration()
    },
  })
  const deleteCommentMutation = useMutation({
    mutationFn: deleteComment,
    onSuccess: async () => {
      await invalidateCollaboration()
      await queryClient.invalidateQueries({
        queryKey: [...QUERY_KEY, 'comments'],
      })
    },
  })
  const createMeetingMutation = useMutation({
    mutationFn: () =>
      createMeeting({
        workspaceId: selectedWorkspace?.workspaceId ?? '',
        title: meetingForm.title.trim(),
        agenda: meetingForm.agenda.trim(),
        startAt: toIsoString(meetingForm.startAtLocal) ?? '',
        endAt: toIsoString(meetingForm.endAtLocal) ?? '',
        location: meetingForm.location.trim(),
        reminderMinutesBefore: meetingForm.reminderMinutesBefore,
        participantIds: meetingForm.participantIds,
      }),
    onSuccess: async (meeting) => {
      setSelectedMeetingId(meeting.meetingId)
      setMeetingForm(DEFAULT_MEETING_FORM)
      await invalidateCollaboration()
    },
  })
  const changeMeetingStatusMutation = useMutation({
    mutationFn: (status: MeetingStatus) =>
      changeMeetingStatus(selectedMeeting?.meetingId ?? '', { status }),
    onSuccess: invalidateCollaboration,
  })
  const publishMinutesMutation = useMutation({
    mutationFn: () =>
      publishMeetingMinutes(selectedMeeting?.meetingId ?? '', {
        minutes: minutesText.trim(),
        mentionPersonIds: selectedMeeting?.participantIds ?? [],
      }),
    onSuccess: async () => {
      setMinutesText('')
      await invalidateCollaboration()
    },
  })
  const triggerRemindersMutation = useMutation({
    mutationFn: () =>
      triggerDueMeetingReminders({ dueAt: new Date().toISOString() }),
    onSuccess: invalidateCollaboration,
  })
  const createGenericCommentMutation = useMutation({
    mutationFn: () => {
      const payload: CreateCommentPayload = {
        workspaceId: selectedWorkspace?.workspaceId ?? '',
        objectType: genericCommentForm.objectType.trim(),
        objectId: genericCommentForm.objectId.trim(),
        body: genericCommentForm.body.trim(),
        mentionPersonIds: genericCommentForm.mentionPersonIds,
        attachments: [],
      }

      return createComment(payload)
    },
    onSuccess: async () => {
      setGenericCommentForm((current) => ({ ...current, body: '' }))
      await queryClient.invalidateQueries({
        queryKey: [...QUERY_KEY, 'comments'],
      })
      await invalidateCollaboration()
    },
  })

  useEffect(() => {
    if (!workspaces.length) {
      setSelectedWorkspaceId(null)
      return
    }

    if (
      !selectedWorkspaceId ||
      !workspaces.some(
        (workspace) => workspace.workspaceId === selectedWorkspaceId,
      )
    ) {
      setSelectedWorkspaceId(workspaces[0].workspaceId)
    }
  }, [selectedWorkspaceId, workspaces])

  useEffect(() => {
    if (!selectedWorkspace) {
      setMemberDrafts([])
      return
    }

    setMemberDrafts(
      selectedWorkspace.members.map((member) => ({
        personId: member.personId,
        roleCode: member.roleCode,
        permissions: member.permissions,
      })),
    )
  }, [selectedWorkspace])

  useEffect(() => {
    if (!discussions.length) {
      setSelectedDiscussionId(null)
      return
    }

    if (
      !selectedDiscussionId ||
      !discussions.some(
        (discussion) => discussion.discussionId === selectedDiscussionId,
      )
    ) {
      setSelectedDiscussionId(discussions[0].discussionId)
    }
  }, [discussions, selectedDiscussionId])

  useEffect(() => {
    if (!tasks.length) {
      setSelectedTaskId(null)
      return
    }

    if (
      !selectedTaskId ||
      !tasks.some((task) => task.taskId === selectedTaskId)
    ) {
      setSelectedTaskId(tasks[0].taskId)
    }
  }, [selectedTaskId, tasks])

  useEffect(() => {
    if (!meetings.length) {
      setSelectedMeetingId(null)
      return
    }

    if (
      !selectedMeetingId ||
      !meetings.some((meeting) => meeting.meetingId === selectedMeetingId)
    ) {
      setSelectedMeetingId(meetings[0].meetingId)
    }
  }, [meetings, selectedMeetingId])

  function handleCreateWorkspace(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    createWorkspaceMutation.mutate({
      code: workspaceForm.code.trim(),
      name: workspaceForm.name.trim(),
      description: workspaceForm.description.trim(),
      visibility: workspaceForm.visibility,
      members: workspaceForm.members,
    })
  }

  function handleReplaceMembers(): void {
    if (selectedWorkspace) {
      replaceMembersMutation.mutate()
    }
  }

  function handleCreateDiscussion(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    if (selectedWorkspace) {
      createDiscussionMutation.mutate()
    }
  }

  function handleReplyDiscussion(): void {
    if (selectedDiscussion && hasText(replyText)) {
      replyDiscussionMutation.mutate()
    }
  }

  function handleCreateTask(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    if (selectedWorkspace && taskForm.assigneeIds[0]) {
      createTaskMutation.mutate()
    }
  }

  function handleCreateMeeting(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    if (selectedWorkspace) {
      createMeetingMutation.mutate()
    }
  }

  function handleCreateComment(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    if (selectedWorkspace && hasText(genericCommentForm.body)) {
      createGenericCommentMutation.mutate()
    }
  }

  function handleWorkspaceFormChange(patch: Partial<WorkspaceFormState>): void {
    setWorkspaceForm((current) => ({ ...current, ...patch }))
  }

  function handleDiscussionFormChange(
    patch: Partial<DiscussionFormState>,
  ): void {
    setDiscussionForm((current) => ({ ...current, ...patch }))
  }

  function handleTaskFormChange(patch: Partial<TaskFormState>): void {
    setTaskForm((current) => ({ ...current, ...patch }))
  }

  function handleMeetingFormChange(patch: Partial<MeetingFormState>): void {
    setMeetingForm((current) => ({ ...current, ...patch }))
  }

  function handleGenericCommentFormChange(
    patch: Partial<GenericCommentFormState>,
  ): void {
    setGenericCommentForm((current) => ({ ...current, ...patch }))
  }

  const tabButtons: Array<{
    key: CollaborationTab
    label: string
    count?: number
  }> = [
    { key: 'discussions', label: '讨论', count: discussions.length },
    { key: 'tasks', label: '任务', count: tasks.length },
    { key: 'meetings', label: '会议', count: meetings.length },
    { key: 'comments', label: '评论' },
    { key: 'audit', label: '审计', count: snapshot?.auditTrail.length ?? 0 },
  ]

  return (
    <div className="space-y-5">
      <Card>
        <CardHeader className="flex-row items-start justify-between gap-4">
          <div>
            <Badge>协同工作台</Badge>
            <CardTitle className="mt-3 flex items-center gap-2 text-2xl">
              <UsersRound className="h-6 w-6 text-sky-600" />
              团队协同
            </CardTitle>
            <p className="mt-2 max-w-3xl text-sm text-slate-500">
              空间成员、讨论、业务评论、任务、会议、已读、提醒、通知和审计均通过后端协同域
              API 落库处理。
            </p>
          </div>
          <Button
            disabled={snapshotQuery.isFetching}
            onClick={() => void snapshotQuery.refetch()}
            type="button"
            variant="outline"
          >
            <RefreshCw className="h-4 w-4" />
            刷新
          </Button>
        </CardHeader>
        <CardContent>
          <div className="grid gap-3 md:grid-cols-4">
            <div className="rounded-xl bg-slate-50 p-3">
              <div className="text-xs text-slate-500">空间</div>
              <div className="mt-1 text-xl font-semibold text-slate-950">
                {workspaces.length}
              </div>
            </div>
            <div className="rounded-xl bg-slate-50 p-3">
              <div className="text-xs text-slate-500">讨论</div>
              <div className="mt-1 text-xl font-semibold text-slate-950">
                {snapshot?.discussions.length ?? 0}
              </div>
            </div>
            <div className="rounded-xl bg-slate-50 p-3">
              <div className="text-xs text-slate-500">任务</div>
              <div className="mt-1 text-xl font-semibold text-slate-950">
                {snapshot?.tasks.length ?? 0}
              </div>
            </div>
            <div className="rounded-xl bg-slate-50 p-3">
              <div className="text-xs text-slate-500">会议</div>
              <div className="mt-1 text-xl font-semibold text-slate-950">
                {snapshot?.meetings.length ?? 0}
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <WorkspacePanel
        isCreating={createWorkspaceMutation.isPending}
        isReplacingMembers={replaceMembersMutation.isPending}
        memberDrafts={memberDrafts}
        onCreateWorkspace={handleCreateWorkspace}
        onMemberDraftsChange={setMemberDrafts}
        onReplaceMembers={handleReplaceMembers}
        onSelectWorkspace={setSelectedWorkspaceId}
        onWorkspaceFormChange={handleWorkspaceFormChange}
        onWorkspaceMembersChange={(members) =>
          setWorkspaceForm((current) => ({ ...current, members }))
        }
        selectedWorkspace={selectedWorkspace}
        selectedWorkspaceId={selectedWorkspaceId}
        workspaceForm={workspaceForm}
        workspaces={workspaces}
      />

      <div className="flex flex-wrap gap-2">
        {tabButtons.map((tab) => (
          <button
            className={cn(
              'inline-flex h-10 items-center gap-2 rounded-xl border px-4 text-sm font-medium transition',
              activeTab === tab.key
                ? 'border-sky-500 bg-sky-500 text-white'
                : 'border-slate-200 bg-white text-slate-600 hover:text-slate-950',
            )}
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            type="button"
          >
            {tab.label}
            {typeof tab.count === 'number' ? (
              <span
                className={cn(
                  'rounded-full px-2 py-0.5 text-xs',
                  activeTab === tab.key
                    ? 'bg-white/20 text-white'
                    : 'bg-slate-100 text-slate-500',
                )}
              >
                {tab.count}
              </span>
            ) : null}
          </button>
        ))}
      </div>

      {snapshotQuery.isError ? (
        <EmptyState text="协同数据加载失败，请检查后端服务和当前租户身份。" />
      ) : null}

      {activeTab === 'discussions' ? (
        <DiscussionPanel
          discussionForm={discussionForm}
          discussions={discussions}
          isCreating={createDiscussionMutation.isPending}
          isReplying={replyDiscussionMutation.isPending}
          onClose={() => closeDiscussionMutation.mutate()}
          onCreateDiscussion={handleCreateDiscussion}
          onDiscussionFormChange={handleDiscussionFormChange}
          onMarkRead={() => markReadMutation.mutate()}
          onPin={(pinned) => pinDiscussionMutation.mutate(pinned)}
          onReplyDiscussion={handleReplyDiscussion}
          onReplyMentionPersonIdsChange={setReplyMentionPersonIds}
          onReplyTextChange={setReplyText}
          onSelectDiscussion={setSelectedDiscussionId}
          replyMentionPersonIds={replyMentionPersonIds}
          replyText={replyText}
          selectedDiscussion={selectedDiscussion}
          workspace={selectedWorkspace}
        />
      ) : null}

      {activeTab === 'tasks' ? (
        <TaskPanel
          isChanging={changeTaskStatusMutation.isPending}
          isCommenting={addTaskCommentMutation.isPending}
          isCreating={createTaskMutation.isPending}
          onAddTaskComment={() => addTaskCommentMutation.mutate()}
          onChangeStatus={(status) => changeTaskStatusMutation.mutate(status)}
          onCreateTask={handleCreateTask}
          onDeleteComment={(commentId) =>
            deleteCommentMutation.mutate(commentId)
          }
          onSelectTask={setSelectedTaskId}
          onTaskCommentTextChange={setTaskCommentText}
          onTaskFormChange={handleTaskFormChange}
          onTaskStatusCommentChange={setTaskStatusComment}
          selectedTask={selectedTask}
          taskCommentText={taskCommentText}
          taskForm={taskForm}
          taskStatusComment={taskStatusComment}
          tasks={tasks}
          workspace={selectedWorkspace}
        />
      ) : null}

      {activeTab === 'meetings' ? (
        <MeetingPanel
          isChanging={changeMeetingStatusMutation.isPending}
          isCreating={createMeetingMutation.isPending}
          isPublishingMinutes={publishMinutesMutation.isPending}
          isTriggeringReminder={triggerRemindersMutation.isPending}
          meetingForm={meetingForm}
          meetings={meetings}
          minutesText={minutesText}
          onChangeStatus={(status) =>
            changeMeetingStatusMutation.mutate(status)
          }
          onCreateMeeting={handleCreateMeeting}
          onMeetingFormChange={handleMeetingFormChange}
          onMinutesTextChange={setMinutesText}
          onPublishMinutes={() => publishMinutesMutation.mutate()}
          onSelectMeeting={setSelectedMeetingId}
          onTriggerReminders={() => triggerRemindersMutation.mutate()}
          selectedMeeting={selectedMeeting}
          workspace={selectedWorkspace}
        />
      ) : null}

      {activeTab === 'comments' ? (
        <CommentPanel
          comments={commentsQuery.data ?? []}
          form={genericCommentForm}
          isCreating={createGenericCommentMutation.isPending}
          isDeleting={deleteCommentMutation.isPending}
          isLoading={commentsQuery.isFetching}
          onCreateComment={handleCreateComment}
          onDeleteComment={(commentId) =>
            deleteCommentMutation.mutate(commentId)
          }
          onFormChange={handleGenericCommentFormChange}
          workspace={selectedWorkspace}
        />
      ) : null}

      {activeTab === 'audit' ? (
        <AuditPanel auditTrail={snapshot?.auditTrail ?? []} />
      ) : null}
    </div>
  )
}

export default function CollaborationPage(): ReactElement {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            retry: 1,
            refetchOnWindowFocus: false,
            staleTime: 15000,
          },
        },
      }),
  )

  return (
    <QueryClientProvider client={queryClient}>
      <CollaborationContent />
    </QueryClientProvider>
  )
}
