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

  it('lists backend jobs using job UUID as the action id', async () => {
    mockedGet.mockResolvedValueOnce([
      {
        id: 'job-uuid',
        jobCode: 'manual-reconcile',
        handlerName: 'manual-handler',
        name: 'Manual reconcile',
        triggerType: 'MANUAL',
        cronExpr: null,
        timezoneId: 'UTC',
        concurrencyPolicy: 'FORBID',
        retryPolicy: null,
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
        id: 'job-uuid',
        jobCode: 'manual-reconcile',
        handlerName: 'manual-handler',
        name: 'Manual reconcile',
        cron: 'MANUAL',
        triggerType: 'MANUAL',
        timezoneId: 'UTC',
        concurrencyPolicy: 'FORBID',
        retryPolicy: null,
        status: 'enabled',
        tenantId: undefined,
        nextRunAt: '2026-04-29T01:00:00Z',
        createdAt: undefined,
        updatedAt: '2026-04-29T01:00:00Z',
      },
    ])
  })

  it('triggers backend jobs by job UUID and maps the execution record', async () => {
    mockedPost.mockResolvedValueOnce({
      id: 'exec-1',
      scheduledJobId: 'job-uuid',
      triggerSource: 'MANUAL',
      executionStatus: 'SUCCESS',
      startedAt: '2026-04-29T01:00:00Z',
      finishedAt: '2026-04-29T01:00:01Z',
      durationMs: 1000,
      attemptNo: 1,
      maxAttempts: 1,
    })

    const execution = await schedulerService.trigger('job-uuid')

    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/infra/scheduler/jobs/job-uuid/trigger',
      undefined,
      { dedupeKey: 'scheduler:trigger:job-uuid' },
    )
    expect(execution).toEqual(
      expect.objectContaining({
        id: 'exec-1',
        scheduledJobId: 'job-uuid',
        executionStatus: 'SUCCESS',
        durationMs: 1000,
      }),
    )
  })

  it('queries execution records with backend filters', async () => {
    mockedGet.mockResolvedValueOnce([])

    await schedulerService.listExecutions({
      page: 1,
      size: 20,
      jobId: 'job-uuid',
      executionStatus: 'FAILED',
    })

    const params = mockedGet.mock.calls[0]?.[1]?.params as URLSearchParams
    expect(mockedGet).toHaveBeenCalledWith(
      '/v1/infra/scheduler/executions',
      expect.objectContaining({ params: expect.any(URLSearchParams) }),
    )
    expect(params.get('jobId')).toBe('job-uuid')
    expect(params.get('executionStatus')).toBe('FAILED')
  })
})
