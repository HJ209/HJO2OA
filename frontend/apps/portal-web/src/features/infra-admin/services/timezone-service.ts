import { get, post, put } from '@/services/request'
import { buildListParams } from '@/features/infra-admin/services/service-utils'
import type {
  InfraListQuery,
  InfraPageData,
  TimezoneSetting,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/timezone/settings'

export const timezoneService = {
  list(query?: InfraListQuery): Promise<InfraPageData<TimezoneSetting>> {
    return get(BASE_URL, { params: buildListParams(query) })
  },
  create(payload: TimezoneSetting): Promise<TimezoneSetting> {
    return post(BASE_URL, payload, {
      dedupeKey: `timezone:create:${payload.id}`,
    })
  },
  update(id: string, payload: TimezoneSetting): Promise<TimezoneSetting> {
    return put(`${BASE_URL}/${id}`, payload, {
      dedupeKey: `timezone:update:${id}`,
    })
  },
}
