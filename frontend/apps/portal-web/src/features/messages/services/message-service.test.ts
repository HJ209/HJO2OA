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
  it('serializes list pagination, filters and sorting through the shared utility contract', async () => {
    mockedGet.mockResolvedValueOnce({
      items: [],
      pagination: { page: 1, size: 20, total: 0, totalPages: 0 },
    })

    const query: PaginationQuery = {
      page: 1,
      size: 20,
      filters: [{ field: 'readStatus', value: 'UNREAD' }],
      sort: [{ field: 'createdAt', direction: 'desc' }],
    }

    await getMessageNotifications(query)

    expect(mockedGet).toHaveBeenCalledWith(
      '/v1/messages/notifications?page=1&size=20&sort=createdAt%2Cdesc&filter%5BreadStatus%5D=UNREAD',
    )
  })

  it('calls the detail endpoint', async () => {
    mockedGet.mockResolvedValueOnce({
      id: 'msg-001',
      type: 'SYSTEM',
      title: '系统通知',
      body: '详情',
      readStatus: 'UNREAD',
      createdAt: '2026-04-28T00:00:00.000Z',
    })

    await getMessageNotificationDetail('msg-001')

    expect(mockedGet).toHaveBeenCalledWith('/v1/messages/notifications/msg-001')
  })

  it('marks a single message as read with idempotency and dedupe keys', async () => {
    mockedPost.mockResolvedValueOnce({
      id: 'msg-001',
      type: 'SYSTEM',
      title: '系统通知',
      summary: '摘要',
      readStatus: 'READ',
      createdAt: '2026-04-28T00:00:00.000Z',
    })

    await markMessageAsRead('msg-001')

    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/messages/notifications/msg-001/read',
      {},
      {
        dedupeKey: 'message-read:msg-001',
        idempotencyKey: 'message-read:msg-001',
      },
    )
  })

  it('marks all messages as read with idempotency and dedupe keys', async () => {
    mockedPost.mockResolvedValueOnce(undefined)

    await markAllMessagesAsRead()

    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/messages/notifications/read-all',
      {},
      {
        dedupeKey: 'message-read-all',
        idempotencyKey: 'message-read-all',
      },
    )
  })

  it('calls the unread count endpoint', async () => {
    mockedGet.mockResolvedValueOnce({ count: 3 })

    await getUnreadMessageCount()

    expect(mockedGet).toHaveBeenCalledWith(
      '/v1/messages/notifications/unread-count',
    )
  })
})
