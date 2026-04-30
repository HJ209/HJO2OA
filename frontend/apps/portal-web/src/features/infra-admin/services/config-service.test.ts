import { beforeEach, describe, expect, it, vi } from 'vitest'
import { configService } from '@/features/infra-admin/services/config-service'
import type { ConfigEntry } from '@/features/infra-admin/types/infra'
import { get, post, put } from '@/services/request'

vi.mock('@/services/request', () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}))

const mockedGet = vi.mocked(get)
const mockedPost = vi.mocked(post)
const mockedPut = vi.mocked(put)

describe('configService', () => {
  const backendEntry = {
    id: 'entry-1',
    configKey: 'regression.browser',
    name: 'regression.browser',
    configType: 'STRING',
    defaultValue: 'created',
    validationRule: null,
    mutableAtRuntime: true,
    tenantAware: false,
    status: 'ENABLED',
    updatedAt: '2026-04-29T01:00:00Z',
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('lists backend configs and maps them to page data', async () => {
    mockedGet.mockResolvedValueOnce([backendEntry])

    const page = await configService.list({ page: 1, size: 20 })

    expect(mockedGet).toHaveBeenCalledWith(
      '/v1/infra/configs',
      expect.objectContaining({ params: expect.any(URLSearchParams) }),
    )
    expect(page.items).toEqual([
      expect.objectContaining({
        id: 'entry-1',
        key: 'regression.browser',
        value: 'created',
        group: 'STRING',
        description: undefined,
        encrypted: false,
        updatedAt: '2026-04-29T01:00:00Z',
      }),
    ])
    expect(page.pagination.total).toBe(1)
  })

  it('creates configs using the backend contract', async () => {
    const payload: ConfigEntry = {
      key: 'regression.browser',
      value: 'created',
      group: 'regression',
      encrypted: false,
    }
    mockedPost.mockResolvedValueOnce(backendEntry)

    await configService.create(payload)

    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/infra/configs',
      {
        configKey: 'regression.browser',
        name: 'regression.browser',
        configType: 'STRING',
        defaultValue: 'created',
        mutableAtRuntime: true,
        tenantAware: false,
        validationRule: undefined,
      },
      { dedupeKey: 'config:create:regression.browser' },
    )
  })

  it('updates config default value by backend entry id', async () => {
    mockedPut.mockResolvedValueOnce({
      ...backendEntry,
      defaultValue: 'updated',
    })

    await configService.update('entry-1', {
      id: 'entry-1',
      key: 'regression.browser',
      value: 'updated',
      group: 'STRING',
      encrypted: false,
    })

    expect(mockedPut).toHaveBeenCalledWith(
      '/v1/infra/configs/entry-1/default',
      { defaultValue: 'updated' },
      { dedupeKey: 'config:update:entry-1' },
    )
  })
})
