import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchPortalHome } from '@/features/portal-home/services/portal-home-service'
import { fetchPortalSnapshot } from '@/features/portal-home/services/portal-snapshot-service'
import type {
  PortalHomePageAssembly,
  PortalSnapshot,
} from '@/features/portal-home/types/portal-home'
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
    const snapshot: PortalSnapshot = {
      banners: [],
      todoSummary: {
        pendingCount: 0,
        overdueCount: 0,
        entryHref: '/todo',
      },
      announcementSummary: {
        totalCount: 0,
        latest: [],
        entryHref: '/announcements',
      },
      messageSummary: {
        unreadCount: 0,
        latest: [],
        entryHref: '/messages',
      },
      shortcuts: [],
      statsCards: [],
    }

    mockedGet.mockResolvedValueOnce(snapshot)

    await expect(fetchPortalSnapshot()).resolves.toEqual(snapshot)
    expect(mockedGet).toHaveBeenCalledWith(
      '/api/v1/portal/aggregation/dashboard',
    )
  })
})
