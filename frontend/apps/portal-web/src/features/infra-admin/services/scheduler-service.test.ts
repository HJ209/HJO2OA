import { beforeEach, describe, expect, it, vi } from 'vitest'
import { schedulerService } from '@/features/infra-admin/services/scheduler-service'
import { get, post } from '@/services/request'

vi.mock('@/services/request', () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}))

const mockedGet = vi.mocked(get)
const mockedPost = vi.mocked(post)

describe('schedulerService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('lists backend jobs and exposes jobCode as the trigger id', async () => {
    mockedGet.mockResolvedValueOnce([
      {
        id: 'job-uuid',
        jobCode: 'manual-reconcile',
        name: 'Manual reconcile',
        triggerType: 'MANUAL',
        cronExpr: null,
        status: 'ACTIVE',
        updatedAt: '2026-04-29T01:00:00Z',
      },
    ])

    const page = await schedulerService.list({ page: 1, size: 20 })

    expect(mockedGet).toHaveBeenCalledWith(
      '/v1/infra/scheduler/jobs',
      expect.objectContaining({ params: expect.any(URLSearchParams) }),
    )
    expect(page.items).toEqual([
      {
        id: 'manual-reconcile',
        name: 'Manual reconcile',
        cron: 'MANUAL',
        status: 'enabled',
        nextRunAt: '2026-04-29T01:00:00Z',
      },
    ])
  })

  it('triggers backend jobs by jobCode', async () => {
    mockedPost.mockResolvedValueOnce({ id: 'exec-1' })

    await schedulerService.trigger('manual-reconcile')

    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/infra/scheduler/jobs/trigger/manual-reconcile',
      undefined,
      { dedupeKey: 'scheduler:trigger:manual-reconcile' },
    )
  })
})
