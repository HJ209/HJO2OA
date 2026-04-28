import { beforeEach, describe, expect, it, vi } from 'vitest'
import { dictionaryService } from '@/features/infra-admin/services/dictionary-service'
import type {
  DictionaryItem,
  DictionaryType,
} from '@/features/infra-admin/types/infra'
import { get, post, put } from '@/services/request'

vi.mock('@/services/request', () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}))

const mockedGet = vi.mocked(get)
const mockedPost = vi.mocked(post)
const mockedPut = vi.mocked(put)

describe('dictionaryService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('calls dictionary type list endpoint with serialized query params', async () => {
    mockedGet.mockResolvedValueOnce({
      items: [],
      pagination: { page: 1, size: 20, total: 0, totalPages: 0 },
    })

    await dictionaryService.listTypes({
      page: 1,
      size: 20,
      keyword: '状态',
      sort: [{ field: 'code', direction: 'asc' }],
    })

    expect(mockedGet).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/types',
      expect.objectContaining({
        params: expect.any(URLSearchParams),
      }),
    )
    const params = mockedGet.mock.calls[0]?.[1]?.params as URLSearchParams
    expect(params.get('page')).toBe('1')
    expect(params.get('size')).toBe('20')
    expect(params.get('keyword')).toBe('状态')
    expect(params.get('sort')).toBe('code,asc')
  })

  it('creates and updates dictionary types with dedupe keys', async () => {
    const payload: DictionaryType = {
      code: 'status',
      name: '状态',
      status: 'enabled',
    }
    mockedPost.mockResolvedValueOnce(payload)
    mockedPut.mockResolvedValueOnce(payload)

    await dictionaryService.createType(payload)
    await dictionaryService.updateType(payload.code, payload)

    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/types',
      payload,
      { dedupeKey: 'dictionary-type:create:status' },
    )
    expect(mockedPut).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/types/status',
      payload,
      { dedupeKey: 'dictionary-type:update:status' },
    )
  })

  it('calls dictionary item endpoints under the selected type', async () => {
    const payload: DictionaryItem = {
      code: 'active',
      label: '有效',
      value: '1',
      sortOrder: 1,
      enabled: true,
    }
    mockedGet.mockResolvedValueOnce({
      items: [payload],
      pagination: { page: 1, size: 20, total: 1, totalPages: 1 },
    })
    mockedPost.mockResolvedValueOnce(payload)
    mockedPut.mockResolvedValueOnce(payload)

    await dictionaryService.listItems('status')
    await dictionaryService.createItem('status', payload)
    await dictionaryService.updateItem('status', payload.code, payload)

    expect(mockedGet).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/types/status/items',
      expect.objectContaining({ params: expect.any(URLSearchParams) }),
    )
    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/types/status/items',
      payload,
      { dedupeKey: 'dictionary-item:create:status:active' },
    )
    expect(mockedPut).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/types/status/items/active',
      payload,
      { dedupeKey: 'dictionary-item:update:status:active' },
    )
  })
})
