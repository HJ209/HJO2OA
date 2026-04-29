import { get, post, put } from '@/services/request'
import { toPageData } from '@/features/infra-admin/services/service-utils'
import type {
  InfraListQuery,
  InfraPageData,
  TenantProfile,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/tenants'

interface BackendTenantProfile {
  id: string
  tenantCode: string
  name: string
  status: 'DRAFT' | 'ACTIVE' | 'SUSPENDED' | 'ARCHIVED'
  defaultTimezone?: string
}

function mapTenantProfile(item: BackendTenantProfile): TenantProfile {
  return {
    id: item.id,
    name: item.name,
    domain: item.tenantCode,
    status: item.status === 'ACTIVE' ? 'enabled' : 'disabled',
    timezone: item.defaultTimezone ?? '-',
  }
}

export const tenantService = {
  async list(query?: InfraListQuery): Promise<InfraPageData<TenantProfile>> {
    const items = await get<BackendTenantProfile[]>(BASE_URL)

    return toPageData(items.map(mapTenantProfile), query)
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
