import { get, post, put } from '@/services/request'
import { buildListParams } from '@/features/infra-admin/services/service-utils'
import type {
  I18nResource,
  InfraListQuery,
  InfraPageData,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/i18n/resources'

export const i18nService = {
  list(query?: InfraListQuery): Promise<InfraPageData<I18nResource>> {
    return get(BASE_URL, { params: buildListParams(query) })
  },
  create(payload: I18nResource): Promise<I18nResource> {
    return post(BASE_URL, payload, { dedupeKey: `i18n:create:${payload.id}` })
  },
  update(id: string, payload: I18nResource): Promise<I18nResource> {
    return put(`${BASE_URL}/${id}`, payload, { dedupeKey: `i18n:update:${id}` })
  },
}
