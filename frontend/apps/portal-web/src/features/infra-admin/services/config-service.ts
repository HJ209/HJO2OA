import { get, post, put } from '@/services/request'
import { buildListParams } from '@/features/infra-admin/services/service-utils'
import type {
  ConfigEntry,
  InfraListQuery,
  InfraPageData,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/config/entries'

export const configService = {
  list(query?: InfraListQuery): Promise<InfraPageData<ConfigEntry>> {
    return get(BASE_URL, { params: buildListParams(query) })
  },
  create(payload: ConfigEntry): Promise<ConfigEntry> {
    return post(BASE_URL, payload, {
      dedupeKey: `config:create:${payload.key}`,
    })
  },
  update(key: string, payload: ConfigEntry): Promise<ConfigEntry> {
    return put(`${BASE_URL}/${key}`, payload, {
      dedupeKey: `config:update:${key}`,
    })
  },
}
