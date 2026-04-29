import { del, get, post, put } from '@/services/request'
import { toPageData } from '@/features/infra-admin/services/service-utils'
import type {
  DictionaryItem,
  DictionaryType,
  InfraListQuery,
  InfraPageData,
  SystemEnumDictionary,
  SystemEnumImportResult,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/dictionaries'

interface BackendDictionaryItem {
  id: string
  dictionaryTypeId: string
  itemCode: string
  displayName: string
  parentItemId?: string | null
  sortOrder: number
  enabled: boolean
  multiLangValue?: string | null
}

interface BackendDictionaryType {
  id: string
  code: string
  name: string
  category?: string | null
  hierarchical: boolean
  cacheable: boolean
  status: 'ACTIVE' | 'DISABLED'
  tenantId?: string | null
  updatedAt?: string
  items: BackendDictionaryItem[]
}

interface CreateDictionaryTypeRequest {
  code: string
  name: string
  category?: string
  hierarchical: boolean
  cacheable: boolean
}

interface AddDictionaryItemRequest {
  itemCode: string
  displayName: string
  parentItemId?: string
  sortOrder: number
}

interface UpdateDictionaryItemRequest {
  displayName: string
  sortOrder: number
}

function mapDictionaryItem(item: BackendDictionaryItem): DictionaryItem {
  return {
    id: item.id,
    code: item.itemCode,
    label: item.displayName,
    value: item.multiLangValue ?? item.itemCode,
    sortOrder: item.sortOrder,
    enabled: item.enabled,
    parentId: item.parentItemId ?? undefined,
  }
}

function mapDictionaryType(item: BackendDictionaryType): DictionaryType {
  return {
    id: item.id,
    code: item.code,
    name: item.name,
    category: item.category ?? undefined,
    hierarchical: item.hierarchical,
    cacheable: item.cacheable,
    status: item.status === 'ACTIVE' ? 'enabled' : 'disabled',
    updatedAt: item.updatedAt,
  }
}

function filterDictionaryTypes(
  items: DictionaryType[],
  query: InfraListQuery,
): DictionaryType[] {
  const keyword = query.keyword?.trim().toLowerCase()

  if (!keyword) {
    return items
  }

  return items.filter((item) =>
    [item.code, item.name, item.category]
      .filter(Boolean)
      .some((value) => value?.toLowerCase().includes(keyword)),
  )
}

export const dictionaryService = {
  async listTypes(
    query: InfraListQuery = {},
  ): Promise<InfraPageData<DictionaryType>> {
    const items = await get<BackendDictionaryType[]>(BASE_URL, {
      params: { includeDisabled: true },
    })
    const filteredItems = filterDictionaryTypes(items.map(mapDictionaryType), query)

    return toPageData(filteredItems, query)
  },
  async createType(payload: DictionaryType): Promise<DictionaryType> {
    const item = await post<BackendDictionaryType, CreateDictionaryTypeRequest>(
      BASE_URL,
      {
        code: payload.code,
        name: payload.name,
        category: payload.category,
        hierarchical: payload.hierarchical ?? false,
        cacheable: payload.cacheable ?? true,
      },
      { dedupeKey: `dictionary-type:create:${payload.code}` },
    )

    return mapDictionaryType(item)
  },
  async enableType(id: string): Promise<DictionaryType> {
    const item = await put<BackendDictionaryType, undefined>(
      `${BASE_URL}/${id}/enable`,
      undefined,
      { dedupeKey: `dictionary-type:enable:${id}` },
    )

    return mapDictionaryType(item)
  },
  async disableType(id: string): Promise<DictionaryType> {
    const item = await put<BackendDictionaryType, undefined>(
      `${BASE_URL}/${id}/disable`,
      undefined,
      { dedupeKey: `dictionary-type:disable:${id}` },
    )

    return mapDictionaryType(item)
  },
  async listItems(
    typeCode: string,
    query: InfraListQuery = {},
  ): Promise<InfraPageData<DictionaryItem>> {
    if (!typeCode) {
      return toPageData([], query)
    }

    const dictionaryType = await get<BackendDictionaryType>(
      `${BASE_URL}/code/${typeCode}`,
    )

    return toPageData(dictionaryType.items.map(mapDictionaryItem), query)
  },
  async createItem(
    typeId: string,
    payload: DictionaryItem,
  ): Promise<DictionaryItem> {
    const item = await post<BackendDictionaryType, AddDictionaryItemRequest>(
      `${BASE_URL}/${typeId}/items`,
      {
        itemCode: payload.code,
        displayName: payload.label,
        parentItemId: payload.parentId,
        sortOrder: payload.sortOrder,
      },
      { dedupeKey: `dictionary-item:create:${typeId}:${payload.code}` },
    )
    const createdItem =
      item.items.find((entry) => entry.itemCode === payload.code) ??
      item.items[item.items.length - 1]

    if (!createdItem) {
      throw new Error('Dictionary item was not returned by backend')
    }

    return mapDictionaryItem(createdItem)
  },
  async updateItem(
    typeId: string,
    itemId: string,
    payload: DictionaryItem,
  ): Promise<DictionaryItem> {
    const item = await put<BackendDictionaryType, UpdateDictionaryItemRequest>(
      `${BASE_URL}/${typeId}/items/${itemId}`,
      {
        displayName: payload.label,
        sortOrder: payload.sortOrder,
      },
      { dedupeKey: `dictionary-item:update:${typeId}:${itemId}` },
    )
    const updatedItem = item.items.find((entry) => entry.id === itemId)

    if (!updatedItem) {
      throw new Error('Dictionary item was not returned by backend')
    }

    return mapDictionaryItem(updatedItem)
  },
  async deleteItem(typeId: string, itemId: string): Promise<void> {
    await del<void>(`${BASE_URL}/${typeId}/items/${itemId}`, {
      dedupeKey: `dictionary-item:delete:${typeId}:${itemId}`,
    })
  },
  async enableItem(typeId: string, itemId: string): Promise<DictionaryItem> {
    const item = await put<BackendDictionaryType, undefined>(
      `${BASE_URL}/${typeId}/items/${itemId}/enable`,
      undefined,
      { dedupeKey: `dictionary-item:enable:${typeId}:${itemId}` },
    )
    const updatedItem = item.items.find((entry) => entry.id === itemId)

    if (!updatedItem) {
      throw new Error('Dictionary item was not returned by backend')
    }

    return mapDictionaryItem(updatedItem)
  },
  async disableItem(typeId: string, itemId: string): Promise<DictionaryItem> {
    const item = await put<BackendDictionaryType, undefined>(
      `${BASE_URL}/${typeId}/items/${itemId}/disable`,
      undefined,
      { dedupeKey: `dictionary-item:disable:${typeId}:${itemId}` },
    )
    const updatedItem = item.items.find((entry) => entry.id === itemId)

    if (!updatedItem) {
      throw new Error('Dictionary item was not returned by backend')
    }

    return mapDictionaryItem(updatedItem)
  },
  async previewSystemEnums(): Promise<SystemEnumDictionary[]> {
    return get<SystemEnumDictionary[]>(`${BASE_URL}/system-enums`)
  },
  async importSystemEnums(): Promise<SystemEnumImportResult> {
    return post<SystemEnumImportResult, undefined>(
      `${BASE_URL}/system-enums/import`,
      undefined,
      { dedupeKey: 'dictionary-system-enums:import' },
    )
  },
}
