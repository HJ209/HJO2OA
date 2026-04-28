import { get, post, put } from '@/services/request'
import { buildListParams } from '@/features/infra-admin/services/service-utils'
import type {
  InfraListQuery,
  InfraPageData,
  SecurityPolicy,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/security/policies'

export const securityService = {
  list(query?: InfraListQuery): Promise<InfraPageData<SecurityPolicy>> {
    return get(BASE_URL, { params: buildListParams(query) })
  },
  create(payload: SecurityPolicy): Promise<SecurityPolicy> {
    return post(BASE_URL, payload, {
      dedupeKey: `security:create:${payload.id}`,
    })
  },
  update(id: string, payload: SecurityPolicy): Promise<SecurityPolicy> {
    return put(`${BASE_URL}/${id}`, payload, {
      dedupeKey: `security:update:${id}`,
    })
  },
}
