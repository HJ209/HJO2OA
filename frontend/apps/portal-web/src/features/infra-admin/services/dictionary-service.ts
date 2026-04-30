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
  defaultItem?: boolean
  extensionJson?: string | null
}

interface BackendDictionaryType {
  id: string
  code: string
  name: string
  category?: string | null
  hierarchical: boolean
  cacheable: boolean
  sortOrder?: number
  systemManaged?: boolean
  status: 'ACTIVE' | 'DISABLED'
  tenantId?: string | null
  updatedAt?: string
  items: BackendDictionaryItem[]
}

interface BackendRuntimeItem {
  id: string
  code: string
  label: string
  value: string
  parentId?: string | null
  sortOrder: number
  enabled: boolean
  defaultItem: boolean
  extensionJson?: string | null
  children?: BackendRuntimeItem[]
}

interface BackendRuntimeDictionary {
  id: string
  code: string
  name: string
  category?: string | null
  hierarchical: boolean
  tenantId?: string | null
  language: string
  items: BackendRuntimeItem[]
}

interface CreateDictionaryTypeRequest {
  code: string
  name: string
  category?: string
  hierarchical: boolean
  cacheable: boolean
  sortOrder?: number
  tenantId?: string | null
}

interface UpdateDictionaryTypeRequest {
  name?: string
  category?: string
  hierarchical?: boolean
  cacheable?: boolean
  sortOrder?: number
}

interface AddDictionaryItemRequest {
  itemCode: string
  displayName: string
  parentItemId?: string
  sortOrder: number
  defaultItem?: boolean
  multiLangValue?: string
  extensionJson?: string
}

interface UpdateDictionaryItemRequest {
  displayName: string
  parentItemId?: string
  sortOrder: number
  defaultItem?: boolean
  multiLangValue?: string
  extensionJson?: string
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
    defaultItem: item.defaultItem ?? false,
    extensionJson: item.extensionJson ?? undefined,
  }
}

function mapRuntimeItem(item: BackendRuntimeItem): DictionaryItem {
  return {
    id: item.id,
    code: item.code,
    label: item.label,
    value: item.value,
    sortOrder: item.sortOrder,
    enabled: item.enabled,
    parentId: item.parentId ?? undefined,
    defaultItem: item.defaultItem,
    extensionJson: item.extensionJson ?? undefined,
    children: item.children?.map(mapRuntimeItem),
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
    sortOrder: item.sortOrder ?? 0,
    systemManaged: item.systemManaged ?? false,
    tenantId: item.tenantId ?? null,
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
    const filteredItems = filterDictionaryTypes(
      items.map(mapDictionaryType),
      query,
    )

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
        sortOrder: payload.sortOrder ?? 0,
        tenantId: payload.tenantId,
      },
      { dedupeKey: `dictionary-type:create:${payload.code}` },
    )

    return mapDictionaryType(item)
  },
  async updateType(
    id: string,
    payload: DictionaryType,
  ): Promise<DictionaryType> {
    const item = await put<BackendDictionaryType, UpdateDictionaryTypeRequest>(
      `${BASE_URL}/${id}`,
      {
        name: payload.name,
        category: payload.category,
        hierarchical: payload.hierarchical,
        cacheable: payload.cacheable,
        sortOrder: payload.sortOrder,
      },
      { dedupeKey: `dictionary-type:update:${id}` },
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
        defaultItem: payload.defaultItem ?? false,
        multiLangValue: payload.value,
        extensionJson: payload.extensionJson,
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
        parentItemId: payload.parentId,
        sortOrder: payload.sortOrder,
        defaultItem: payload.defaultItem ?? false,
        multiLangValue: payload.value,
        extensionJson: payload.extensionJson,
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
  async getRuntime(
    code: string,
    options: { enabledOnly?: boolean; tree?: boolean; language?: string } = {},
  ): Promise<BackendRuntimeDictionary> {
    return get<BackendRuntimeDictionary>(`${BASE_URL}/${code}`, {
      params: {
        enabledOnly: options.enabledOnly ?? true,
        tree: options.tree ?? false,
        language: options.language,
      },
    })
  },
  async listRuntimeItems(
    code: string,
    options: { enabledOnly?: boolean; language?: string } = {},
  ): Promise<DictionaryItem[]> {
    const items = await get<BackendRuntimeItem[]>(`${BASE_URL}/${code}/items`, {
      params: {
        enabledOnly: options.enabledOnly ?? true,
        language: options.language,
      },
    })

    return items.map(mapRuntimeItem)
  },
  async tree(
    code: string,
    options: { enabledOnly?: boolean; language?: string } = {},
  ): Promise<DictionaryItem[]> {
    const items = await get<BackendRuntimeItem[]>(`${BASE_URL}/${code}/tree`, {
      params: {
        enabledOnly: options.enabledOnly ?? true,
        language: options.language,
      },
    })

    return items.map(mapRuntimeItem)
  },
  async batchRuntime(
    codes: string[],
    options: { enabledOnly?: boolean; tree?: boolean; language?: string } = {},
  ): Promise<Record<string, DictionaryItem[]>> {
    const data = await post<
      Record<string, BackendRuntimeDictionary>,
      { codes: string[]; enabledOnly?: boolean; tree?: boolean }
    >(
      `${BASE_URL}/batch`,
      {
        codes,
        enabledOnly: options.enabledOnly ?? true,
        tree: options.tree ?? false,
      },
      {
        params: { language: options.language },
        dedupeKey: `dictionary-runtime:batch:${codes.join(',')}`,
      },
    )

    return Object.fromEntries(
      Object.entries(data).map(([code, dictionary]) => [
        code,
        dictionary.items.map(mapRuntimeItem),
      ]),
    )
  },
  async refreshCache(
    code: string,
    options: { tree?: boolean; language?: string } = {},
  ): Promise<DictionaryItem[]> {
    const dictionary = await post<BackendRuntimeDictionary, undefined>(
      `${BASE_URL}/${code}/cache/refresh`,
      undefined,
      {
        params: {
          tree: options.tree ?? true,
          language: options.language,
        },
        dedupeKey: `dictionary-runtime:refresh:${code}`,
      },
    )

    return dictionary.items.map(mapRuntimeItem)
  },
}
