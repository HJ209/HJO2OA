import { get, post, put } from '@/services/request'
import { buildListParams } from '@/features/infra-admin/services/service-utils'
import type {
  DataI18nTranslation,
  InfraListQuery,
  InfraPageData,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/data-i18n/translations'

export const dataI18nService = {
  list(query?: InfraListQuery): Promise<InfraPageData<DataI18nTranslation>> {
    return get(BASE_URL, { params: buildListParams(query) })
  },
  create(payload: DataI18nTranslation): Promise<DataI18nTranslation> {
    return post(BASE_URL, payload, {
      dedupeKey: `data-i18n:create:${payload.id}`,
    })
  },
  update(
    id: string,
    payload: DataI18nTranslation,
  ): Promise<DataI18nTranslation> {
    return put(`${BASE_URL}/${id}`, payload, {
      dedupeKey: `data-i18n:update:${id}`,
    })
  },
}
