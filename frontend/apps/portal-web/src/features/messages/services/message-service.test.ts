import { afterEach, describe, expect, it, vi } from 'vitest'
import type { PaginationQuery } from '@/types/api'
import { get, post } from '@/services/request'
import {
  getMessageNotificationDetail,
  getMessageNotifications,
  getUnreadMessageCount,
  markAllMessagesAsRead,
  markMessageAsRead,
} from '@/features/messages/services/message-service'

vi.mock('@/services/request', () => ({
  get: vi.fn(),
  post: vi.fn(),
}))

const mockedGet = vi.mocked(get)
const mockedPost = vi.mocked(post)

afterEach(() => {
  vi.clearAllMocks()
})

describe('message-service', () => {
  it('calls the messages list endpoint with pagination query', async () => {
    mockedGet.mockResolvedValueOnce([])

    const query: PaginationQuery = {
      page: 1,
      size: 20,
      filters: [{ field: 'readStatus', value: 'UNREAD' }],
      sort: [{ field: 'createdAt', direction: 'desc' }],
    }

    await getMessageNotifications(query)

    expect(mockedGet).toHaveBeenCalledWith(
      expect.stringContaining('/v1/msg/messages?'),
    )
  })

  it('transforms backend NotificationSummary to frontend MessageNotificationSummary', async () => {
    mockedGet.mockResolvedValueOnce([
      {
        notificationId: 'notif-001',
        title: '待办创建',
        bodySummary: '您有一条新待办',
        category: 'TODO_CREATED',
        priority: 'NORMAL',
        inboxStatus: 'UNREAD',
        deliveryStatus: 'DELIVERED',
        sourceModule: 'todo-center',
        deepLink: '/todo/1',
        targetAssignmentId: null,
        targetPositionId: null,
        createdAt: '2026-04-28T00:00:00.000Z',
      },
    ])

    const result = await getMessageNotifications()

    expect(result).toEqual([
      {
        id: 'notif-001',
        type: 'TASK',
        title: '待办创建',
        summary: '您有一条新待办',
        readStatus: 'UNREAD',
        createdAt: '2026-04-28T00:00:00.000Z',
      },
    ])
  })

  it('calls the detail endpoint and transforms response', async () => {
    mockedGet.mockResolvedValueOnce({
      notificationId: 'notif-001',
      title: '系统通知',
      bodySummary: '详情内容',
      category: 'SYSTEM_SECURITY',
      priority: 'URGENT',
      inboxStatus: 'READ',
      deliveryStatus: 'DELIVERED',
      sourceModule: 'system',
      sourceEventType: 'SECURITY_ALERT',
      sourceBusinessId: 'evt-001',
      deepLink: '/detail',
      targetAssignmentId: null,
      targetPositionId: null,
      createdAt: '2026-04-28T00:00:00.000Z',
      readAt: '2026-04-28T01:00:00.000Z',
      archivedAt: null,
      revokedAt: null,
      expiredAt: null,
      statusReason: null,
    })

    const result = await getMessageNotificationDetail('notif-001')

    expect(mockedGet).toHaveBeenCalledWith('/v1/msg/messages/notif-001')
    expect(result).toEqual({
      id: 'notif-001',
      type: 'SYSTEM',
      title: '系统通知',
      body: '详情内容',
      readStatus: 'READ',
      createdAt: '2026-04-28T00:00:00.000Z',
      readAt: '2026-04-28T01:00:00.000Z',
    })
  })

  it('marks a single message as read with idempotency and dedupe keys', async () => {
    mockedPost.mockResolvedValueOnce({
      notificationId: 'notif-001',
      title: '系统通知',
      bodySummary: '摘要',
      category: 'TODO_CREATED',
      priority: 'NORMAL',
      inboxStatus: 'READ',
      deliveryStatus: 'DELIVERED',
      sourceModule: 'todo-center',
      deepLink: '/todo/1',
      targetAssignmentId: null,
      targetPositionId: null,
      createdAt: '2026-04-28T00:00:00.000Z',
    })

    const result = await markMessageAsRead('notif-001')

    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/msg/messages/notif-001/read',
      {},
      {
        dedupeKey: 'message-read:notif-001',
        idempotencyKey: 'message-read:notif-001',
      },
    )
    expect(result.readStatus).toBe('READ')
  })

  it('marks all messages as read with bulk-read endpoint', async () => {
    mockedPost.mockResolvedValueOnce({
      readCount: 3,
      notFoundIds: [],
    })

    await markAllMessagesAsRead(['notif-001', 'notif-002', 'notif-003'])

    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/msg/messages/bulk-read',
      { notificationIds: ['notif-001', 'notif-002', 'notif-003'] },
      {
        dedupeKey: 'message-read-all',
        idempotencyKey: 'message-read-all',
      },
    )
  })

  it('calls the unread summary endpoint and extracts total count', async () => {
    mockedGet.mockResolvedValueOnce({
      totalUnreadCount: 5,
      categoryUnreadCounts: { TODO_CREATED: 3, TODO_OVERDUE: 2 },
      latestNotificationIds: ['notif-001', 'notif-002'],
    })

    const result = await getUnreadMessageCount()

    expect(mockedGet).toHaveBeenCalledWith('/v1/msg/unread-summary')
    expect(result).toEqual({ count: 5 })
  })
})
