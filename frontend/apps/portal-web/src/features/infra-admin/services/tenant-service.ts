import { get, post, put } from '@/services/request'
import { buildListParams } from '@/features/infra-admin/services/service-utils'
import type {
  InfraListQuery,
  InfraPageData,
  TenantProfile,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/tenants/profiles'

export const tenantService = {
  list(query?: InfraListQuery): Promise<InfraPageData<TenantProfile>> {
    return get(BASE_URL, { params: buildListParams(query) })
  },
  create(payload: TenantProfile): Promise<TenantProfile> {
    return post(BASE_URL, payload, { dedupeKey: `tenant:create:${payload.id}` })
  },
  update(id: string, payload: TenantProfile): Promise<TenantProfile> {
    return put(`${BASE_URL}/${id}`, payload, {
      dedupeKey: `tenant:update:${id}`,
    })
  },
}
