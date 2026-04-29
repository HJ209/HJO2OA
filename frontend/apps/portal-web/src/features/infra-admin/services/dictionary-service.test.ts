import { beforeEach, describe, expect, it, vi } from 'vitest'
import { dictionaryService } from '@/features/infra-admin/services/dictionary-service'
import type {
  DictionaryItem,
  DictionaryType,
} from '@/features/infra-admin/types/infra'
import { del, get, post, put } from '@/services/request'

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

const backendType = {
  id: 'type-1',
  code: 'person_status',
  name: '人员状态',
  category: 'org',
  hierarchical: false,
  cacheable: true,
  status: 'ACTIVE',
  tenantId: null,
  updatedAt: '2026-04-29T01:00:00Z',
  items: [
    {
      id: 'item-1',
      dictionaryTypeId: 'type-1',
      itemCode: 'ACTIVE',
      displayName: '启用',
      parentItemId: null,
      sortOrder: 1,
      enabled: true,
      multiLangValue: null,
    },
  ],
}

describe('dictionaryService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('lists backend dictionary types and maps to page data', async () => {
    mockedGet.mockResolvedValueOnce([backendType])

    const page = await dictionaryService.listTypes({
      page: 1,
      size: 20,
      keyword: 'person',
    })

    expect(mockedGet).toHaveBeenCalledWith('/v1/infra/dictionaries', {
      params: { includeDisabled: true },
    })
    expect(page.items).toEqual([
      {
        id: 'type-1',
        code: 'person_status',
        name: '人员状态',
        category: 'org',
        hierarchical: false,
        cacheable: true,
        status: 'enabled',
        updatedAt: '2026-04-29T01:00:00Z',
      },
    ])
  })

  it('creates and toggles dictionary types with backend ids', async () => {
    const payload: DictionaryType = {
      code: 'person_status',
      name: '人员状态',
      category: 'org',
      hierarchical: false,
      cacheable: true,
      status: 'enabled',
    }
    mockedPost.mockResolvedValueOnce(backendType)
    mockedPut.mockResolvedValue(backendType)

    await dictionaryService.createType(payload)
    await dictionaryService.disableType('type-1')
    await dictionaryService.enableType('type-1')

    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/infra/dictionaries',
      {
        code: 'person_status',
        name: '人员状态',
        category: 'org',
        hierarchical: false,
        cacheable: true,
      },
      { dedupeKey: 'dictionary-type:create:person_status' },
    )
    expect(mockedPut).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/type-1/disable',
      undefined,
      { dedupeKey: 'dictionary-type:disable:type-1' },
    )
    expect(mockedPut).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/type-1/enable',
      undefined,
      { dedupeKey: 'dictionary-type:enable:type-1' },
    )
  })

  it('lists and mutates dictionary items under the selected backend type', async () => {
    const payload: DictionaryItem = {
      code: 'ACTIVE',
      label: '启用',
      value: 'ACTIVE',
      sortOrder: 1,
      enabled: true,
    }
    mockedGet.mockResolvedValueOnce(backendType)
    mockedPost.mockResolvedValueOnce(backendType)
    mockedPut.mockResolvedValue(backendType)
    mockedDel.mockResolvedValueOnce(undefined)

    await dictionaryService.listItems('person_status')
    await dictionaryService.createItem('type-1', payload)
    await dictionaryService.updateItem('type-1', 'item-1', payload)
    await dictionaryService.disableItem('type-1', 'item-1')
    await dictionaryService.enableItem('type-1', 'item-1')
    await dictionaryService.deleteItem('type-1', 'item-1')

    expect(mockedGet).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/code/person_status',
    )
    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/type-1/items',
      {
        itemCode: 'ACTIVE',
        displayName: '启用',
        parentItemId: undefined,
        sortOrder: 1,
      },
      { dedupeKey: 'dictionary-item:create:type-1:ACTIVE' },
    )
    expect(mockedPut).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/type-1/items/item-1',
      { displayName: '启用', sortOrder: 1 },
      { dedupeKey: 'dictionary-item:update:type-1:item-1' },
    )
    expect(mockedPut).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/type-1/items/item-1/disable',
      undefined,
      { dedupeKey: 'dictionary-item:disable:type-1:item-1' },
    )
    expect(mockedPut).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/type-1/items/item-1/enable',
      undefined,
      { dedupeKey: 'dictionary-item:enable:type-1:item-1' },
    )
    expect(mockedDel).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/type-1/items/item-1',
      { dedupeKey: 'dictionary-item:delete:type-1:item-1' },
    )
  })

  it('previews and imports system enums', async () => {
    mockedGet.mockResolvedValueOnce([
      {
        code: 'system.enum.person_status.abc123',
        name: 'PersonStatus',
        className: 'com.hjo2oa.org.person.account.domain.PersonStatus',
        category: 'system-enum',
        items: [{ code: 'ACTIVE', name: 'Active', sortOrder: 0 }],
      },
    ])
    mockedPost.mockResolvedValueOnce({
      discoveredTypes: 1,
      createdTypes: 1,
      createdItems: 1,
      importedCodes: ['system.enum.person_status.abc123'],
    })

    const preview = await dictionaryService.previewSystemEnums()
    const result = await dictionaryService.importSystemEnums()

    expect(preview).toHaveLength(1)
    expect(result.createdItems).toBe(1)
    expect(mockedGet).toHaveBeenCalledWith('/v1/infra/dictionaries/system-enums')
    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/system-enums/import',
      undefined,
      { dedupeKey: 'dictionary-system-enums:import' },
    )
  })
})
