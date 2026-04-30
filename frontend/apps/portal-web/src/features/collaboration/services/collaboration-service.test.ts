import { afterEach, describe, expect, it, vi } from 'vitest'
import { del, get, post, put } from '@/services/request'
import {
  changeTaskStatus,
  closeDiscussion,
  createDiscussion,
  createTask,
  createWorkspace,
  deleteComment,
  getCollaborationSnapshot,
  markDiscussionRead,
  pinDiscussion,
  replaceWorkspaceMembers,
  triggerDueMeetingReminders,
} from '@/features/collaboration/services/collaboration-service'

vi.mock('@/services/request', () => ({
  del: vi.fn(),
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}))

const mockedDel = vi.mocked(del)
const mockedGet = vi.mocked(get)
const mockedPost = vi.mocked(post)
const mockedPut = vi.mocked(put)

afterEach(() => {
  vi.clearAllMocks()
})

describe('collaboration-service', () => {
  it('loads collaboration snapshot from the unified business API', async () => {
    mockedGet.mockResolvedValueOnce({
      workspaces: [],
      discussions: [],
      tasks: [],
      meetings: [],
      auditTrail: [],
    })

    const snapshot = await getCollaborationSnapshot()

    expect(mockedGet).toHaveBeenCalledWith('/v1/biz/collaboration/snapshot')
    expect(snapshot.workspaces).toEqual([])
  })

  it('creates workspace with idempotency and member payload', async () => {
    mockedPost.mockResolvedValueOnce({ workspaceId: 'space-1' })

    await createWorkspace({
      code: 'project-alpha',
      name: 'Project Alpha',
      description: 'Cross team work',
      visibility: 'PRIVATE',
      members: [
        {
          personId: 'person-1',
          roleCode: 'MANAGER',
          permissions: ['READ', 'COMMENT'],
        },
      ],
    })

    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/biz/collaboration/workspaces',
      expect.objectContaining({
        code: 'project-alpha',
        members: [
          {
            personId: 'person-1',
            roleCode: 'MANAGER',
            permissions: ['READ', 'COMMENT'],
          },
        ],
      }),
      expect.objectContaining({
        dedupeKey: 'collaboration:workspace:create:project-alpha',
        idempotencyKey: expect.stringContaining(
          'collaboration-workspace-create:',
        ),
      }),
    )
  })

  it('replaces workspace members through the member permission endpoint', async () => {
    mockedPut.mockResolvedValueOnce({ workspaceId: 'space-1' })

    await replaceWorkspaceMembers('space-1', {
      members: [{ personId: 'person-2', roleCode: 'MEMBER', permissions: [] }],
    })

    expect(mockedPut).toHaveBeenCalledWith(
      '/v1/biz/collaboration/workspaces/space-1/members',
      {
        members: [
          { personId: 'person-2', roleCode: 'MEMBER', permissions: [] },
        ],
      },
      expect.objectContaining({
        dedupeKey: 'collaboration:workspace:members:space-1',
      }),
    )
  })

  it('supports discussion actions including mention, read, pin and close', async () => {
    mockedPost
      .mockResolvedValueOnce({ discussionId: 'discussion-1' })
      .mockResolvedValueOnce({ discussionId: 'discussion-1' })
      .mockResolvedValueOnce({ discussionId: 'discussion-1' })
      .mockResolvedValueOnce({ discussionId: 'discussion-1' })

    await createDiscussion('space-1', {
      title: 'Launch plan',
      body: 'Please review',
      mentionPersonIds: ['person-1'],
      attachments: [{ attachmentId: 'file-1', fileName: 'plan.pdf' }],
    })
    await markDiscussionRead('discussion-1')
    await pinDiscussion('discussion-1', true)
    await closeDiscussion('discussion-1')

    expect(mockedPost).toHaveBeenNthCalledWith(
      1,
      '/v1/biz/collaboration/workspaces/space-1/discussions',
      expect.objectContaining({ mentionPersonIds: ['person-1'] }),
      expect.objectContaining({
        dedupeKey: 'collaboration:discussion:create:space-1:Launch plan',
      }),
    )
    expect(mockedPost).toHaveBeenNthCalledWith(
      3,
      '/v1/biz/collaboration/discussions/discussion-1/pin',
      {},
      expect.objectContaining({
        params: new URLSearchParams({ pinned: 'true' }),
      }),
    )
  })

  it('supports task status flow calls and comment cleanup endpoint', async () => {
    mockedPost
      .mockResolvedValueOnce({ taskId: 'task-1' })
      .mockResolvedValueOnce({ taskId: 'task-1', status: 'IN_PROGRESS' })
    mockedDel.mockResolvedValueOnce({ commentId: 'comment-1', deleted: true })

    await createTask({
      workspaceId: 'space-1',
      title: 'Prepare deck',
      description: 'Draft rollout deck',
      assigneeId: 'person-1',
      participantIds: ['person-2'],
      priority: 'HIGH',
      dueAt: '2026-04-30T01:00:00.000Z',
    })
    await changeTaskStatus('task-1', {
      status: 'IN_PROGRESS',
      comment: 'Started',
    })
    await deleteComment('comment-1')

    expect(mockedPost).toHaveBeenNthCalledWith(
      2,
      '/v1/biz/collaboration/tasks/task-1/status',
      { status: 'IN_PROGRESS', comment: 'Started' },
      expect.objectContaining({
        dedupeKey: 'collaboration:task:status:task-1:IN_PROGRESS',
      }),
    )
    expect(mockedDel).toHaveBeenCalledWith(
      '/v1/biz/collaboration/comments/comment-1',
      expect.objectContaining({
        dedupeKey: 'collaboration:comment:delete:comment-1',
      }),
    )
  })

  it('triggers due meeting reminders with idempotency', async () => {
    mockedPost.mockResolvedValueOnce({ scannedCount: 2, sentCount: 2 })

    const result = await triggerDueMeetingReminders({
      dueAt: '2026-04-29T01:00:00.000Z',
    })

    expect(result.sentCount).toBe(2)
    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/biz/collaboration/meeting-reminders/trigger-due',
      { dueAt: '2026-04-29T01:00:00.000Z' },
      expect.objectContaining({
        dedupeKey: 'collaboration:meeting-reminders:trigger-due',
      }),
    )
  })
})
