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
  name: 'Person Status',
  category: 'org',
  hierarchical: true,
  cacheable: true,
  sortOrder: 3,
  systemManaged: false,
  status: 'ACTIVE',
  tenantId: null,
  updatedAt: '2026-04-29T01:00:00Z',
  items: [
    {
      id: 'item-1',
      dictionaryTypeId: 'type-1',
      itemCode: 'ACTIVE',
      displayName: 'Active',
      parentItemId: null,
      sortOrder: 1,
      enabled: true,
      multiLangValue: 'active',
      defaultItem: true,
      extensionJson: '{"color":"green"}',
    },
  ],
}

describe('dictionaryService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('lists backend dictionary types with system and sort metadata', async () => {
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
      expect.objectContaining({
        id: 'type-1',
        code: 'person_status',
        name: 'Person Status',
        hierarchical: true,
        cacheable: true,
        sortOrder: 3,
        systemManaged: false,
        status: 'enabled',
      }),
    ])
  })

  it('creates, updates and toggles dictionary types through backend ids', async () => {
    const payload: DictionaryType = {
      code: 'person_status',
      name: 'Person Status',
      category: 'org',
      hierarchical: true,
      cacheable: true,
      sortOrder: 3,
      status: 'enabled',
    }
    mockedPost.mockResolvedValueOnce(backendType)
    mockedPut.mockResolvedValue(backendType)

    await dictionaryService.createType(payload)
    await dictionaryService.updateType('type-1', payload)
    await dictionaryService.disableType('type-1')
    await dictionaryService.enableType('type-1')

    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/infra/dictionaries',
      {
        code: 'person_status',
        name: 'Person Status',
        category: 'org',
        hierarchical: true,
        cacheable: true,
        sortOrder: 3,
        tenantId: undefined,
      },
      { dedupeKey: 'dictionary-type:create:person_status' },
    )
    expect(mockedPut).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/type-1',
      {
        name: 'Person Status',
        category: 'org',
        hierarchical: true,
        cacheable: true,
        sortOrder: 3,
      },
      { dedupeKey: 'dictionary-type:update:type-1' },
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

  it('lists and mutates tree dictionary items with default and extension data', async () => {
    const payload: DictionaryItem = {
      code: 'ACTIVE',
      label: 'Active',
      value: 'active',
      parentId: 'parent-1',
      sortOrder: 1,
      enabled: true,
      defaultItem: true,
      extensionJson: '{"color":"green"}',
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
        displayName: 'Active',
        parentItemId: 'parent-1',
        sortOrder: 1,
        defaultItem: true,
        multiLangValue: 'active',
        extensionJson: '{"color":"green"}',
      },
      { dedupeKey: 'dictionary-item:create:type-1:ACTIVE' },
    )
    expect(mockedPut).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/type-1/items/item-1',
      {
        displayName: 'Active',
        parentItemId: 'parent-1',
        sortOrder: 1,
        defaultItem: true,
        multiLangValue: 'active',
        extensionJson: '{"color":"green"}',
      },
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

  it('uses runtime dictionary APIs for items, tree, batch and refresh', async () => {
    const runtimeItem = {
      id: 'item-1',
      code: 'ACTIVE',
      label: 'Active',
      value: 'active',
      parentId: null,
      sortOrder: 1,
      enabled: true,
      defaultItem: true,
      extensionJson: null,
      children: [],
    }
    mockedGet
      .mockResolvedValueOnce([runtimeItem])
      .mockResolvedValueOnce([runtimeItem])
    mockedPost
      .mockResolvedValueOnce({
        person_status: {
          id: 'type-1',
          code: 'person_status',
          name: 'Person Status',
          hierarchical: false,
          language: 'en-US',
          items: [runtimeItem],
        },
      })
      .mockResolvedValueOnce({
        id: 'type-1',
        code: 'person_status',
        name: 'Person Status',
        hierarchical: false,
        language: 'en-US',
        items: [runtimeItem],
      })

    const runtimeItems =
      await dictionaryService.listRuntimeItems('person_status')
    const tree = await dictionaryService.tree('person_status', {
      enabledOnly: false,
    })
    const batch = await dictionaryService.batchRuntime(['person_status'], {
      language: 'en-US',
    })
    const refreshed = await dictionaryService.refreshCache('person_status')

    expect(runtimeItems[0]).toEqual(
      expect.objectContaining({ value: 'active' }),
    )
    expect(tree[0]).toEqual(expect.objectContaining({ code: 'ACTIVE' }))
    expect(batch.person_status).toHaveLength(1)
    expect(refreshed).toHaveLength(1)
    expect(mockedGet).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/person_status/items',
      {
        params: { enabledOnly: true, language: undefined },
      },
    )
    expect(mockedGet).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/person_status/tree',
      {
        params: { enabledOnly: false, language: undefined },
      },
    )
    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/batch',
      { codes: ['person_status'], enabledOnly: true, tree: false },
      {
        params: { language: 'en-US' },
        dedupeKey: 'dictionary-runtime:batch:person_status',
      },
    )
    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/person_status/cache/refresh',
      undefined,
      {
        params: { tree: true, language: undefined },
        dedupeKey: 'dictionary-runtime:refresh:person_status',
      },
    )
  })

  it('previews and imports system enum diffs', async () => {
    mockedGet.mockResolvedValueOnce([
      {
        code: 'system.enum.person_status.abc123',
        name: 'PersonStatus',
        className: 'com.hjo2oa.org.person.account.domain.PersonStatus',
        category: 'system-enum',
        imported: true,
        newItemCodes: ['ACTIVE'],
        changedItemCodes: [],
        disabledItemCodes: [],
        items: [{ code: 'ACTIVE', name: 'Active', sortOrder: 0 }],
      },
    ])
    mockedPost.mockResolvedValueOnce({
      discoveredTypes: 1,
      createdTypes: 1,
      createdItems: 1,
      updatedItems: 0,
      disabledItems: 0,
      importedCodes: ['system.enum.person_status.abc123'],
    })

    const preview = await dictionaryService.previewSystemEnums()
    const result = await dictionaryService.importSystemEnums()

    expect(preview[0].newItemCodes).toEqual(['ACTIVE'])
    expect(result.createdItems).toBe(1)
    expect(mockedGet).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/system-enums',
    )
    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/infra/dictionaries/system-enums/import',
      undefined,
      { dedupeKey: 'dictionary-system-enums:import' },
    )
  })
})
