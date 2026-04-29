import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchPortalHome } from '@/features/portal-home/services/portal-home-service'
import { fetchPortalSnapshot } from '@/features/portal-home/services/portal-snapshot-service'
import type { PortalHomePageAssembly } from '@/features/portal-home/types/portal-home'
import { get } from '@/services/request'

vi.mock('@/services/request', () => ({
  get: vi.fn(),
}))

const mockedGet = vi.mocked(get)

describe('portal home services', () => {
  beforeEach(() => {
    mockedGet.mockReset()
  })

  it('fetches portal home assembly from the backend contract path', async () => {
    const assembly: PortalHomePageAssembly = {
      sections: [],
    }

    mockedGet.mockResolvedValueOnce(assembly)

    await expect(fetchPortalHome()).resolves.toEqual(assembly)
    expect(mockedGet).toHaveBeenCalledWith('/v1/portal/home/page')
  })

  it('fetches portal snapshot from the backend contract path', async () => {
    const dashboard = {
      identity: {
        state: 'READY',
        data: {
          positionName: 'Portal Admin',
          organizationName: 'Headquarters',
        },
      },
      todo: {
        state: 'READY',
        data: {
          totalCount: 3,
          urgentCount: 1,
        },
      },
      message: {
        state: 'READY',
        data: {
          unreadCount: 2,
          topItems: [
            {
              notificationId: 'notif-1',
              title: 'Approve travel request',
              createdAt: '2026-04-19T10:00:00Z',
              priority: 'HIGH',
            },
          ],
        },
      },
    }

    mockedGet.mockResolvedValueOnce(dashboard)

    await expect(fetchPortalSnapshot()).resolves.toMatchObject({
      todoSummary: {
        pendingCount: 3,
        todayDueCount: 1,
        entryHref: '/todo',
      },
      messageSummary: {
        unreadCount: 2,
        latest: [
          {
            id: 'notif-1',
            title: 'Approve travel request',
            sentAtUtc: '2026-04-19T10:00:00Z',
          },
        ],
        entryHref: '/messages',
      },
      statsCards: [
        {
          id: 'identity',
          title: '当前岗位',
          value: 'Portal Admin',
          trendText: 'Headquarters',
        },
        {
          id: 'todo',
          value: 3,
        },
        {
          id: 'message',
          value: 2,
        },
      ],
    })
    expect(mockedGet).toHaveBeenCalledWith('/v1/portal/aggregation/dashboard')
  })
})
