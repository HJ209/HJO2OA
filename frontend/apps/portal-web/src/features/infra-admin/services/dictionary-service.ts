import { get, post, put } from '@/services/request'
import { buildListParams } from '@/features/infra-admin/services/service-utils'
import type {
  DictionaryItem,
  DictionaryType,
  InfraListQuery,
  InfraPageData,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/dictionaries/types'

export const dictionaryService = {
  listTypes(query?: InfraListQuery): Promise<InfraPageData<DictionaryType>> {
    return get(BASE_URL, { params: buildListParams(query) })
  },
  createType(payload: DictionaryType): Promise<DictionaryType> {
    return post(BASE_URL, payload, {
      dedupeKey: `dictionary-type:create:${payload.code}`,
    })
  },
  updateType(code: string, payload: DictionaryType): Promise<DictionaryType> {
    return put(`${BASE_URL}/${code}`, payload, {
      dedupeKey: `dictionary-type:update:${code}`,
    })
  },
  listItems(
    typeCode: string,
    query?: InfraListQuery,
  ): Promise<InfraPageData<DictionaryItem>> {
    return get(`${BASE_URL}/${typeCode}/items`, {
      params: buildListParams(query),
    })
  },
  createItem(
    typeCode: string,
    payload: DictionaryItem,
  ): Promise<DictionaryItem> {
    return post(`${BASE_URL}/${typeCode}/items`, payload, {
      dedupeKey: `dictionary-item:create:${typeCode}:${payload.code}`,
    })
  },
  updateItem(
    typeCode: string,
    code: string,
    payload: DictionaryItem,
  ): Promise<DictionaryItem> {
    return put(`${BASE_URL}/${typeCode}/items/${code}`, payload, {
      dedupeKey: `dictionary-item:update:${typeCode}:${code}`,
    })
  },
}
