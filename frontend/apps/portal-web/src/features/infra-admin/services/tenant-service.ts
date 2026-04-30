import { get, post, put } from '@/services/request'
import { toPageData } from '@/features/infra-admin/services/service-utils'
import type {
  InfraListQuery,
  InfraPageData,
  TenantProfile,
  TenantQuota,
  TenantQuotaType,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/tenants'

interface BackendTenantQuota {
  id: string
  tenantProfileId: string
  quotaType: TenantQuotaType
  limitValue: number
  usedValue: number
  warningThreshold?: number | null
  warning: boolean
}

interface BackendTenantProfile {
  id: string
  tenantCode: string
  name: string
  status: 'DRAFT' | 'ACTIVE' | 'SUSPENDED' | 'ARCHIVED'
  isolationMode: 'SHARED_DB' | 'DEDICATED_DB'
  packageCode?: string | null
  defaultLocale?: string | null
  defaultTimezone?: string | null
  initialized: boolean
  adminAccountId?: string | null
  adminPersonId?: string | null
  quotas?: BackendTenantQuota[]
  updatedAt?: string
}

interface CreateTenantRequest {
  code: string
  name: string
  isolationMode: 'SHARED_DB' | 'DEDICATED_DB'
  packageCode?: string
  defaultLocale?: string
  defaultTimezone?: string
  adminAccountId?: string
  adminPersonId?: string
}

interface UpdateTenantRequest {
  name?: string
  packageCode?: string
  defaultLocale?: string
  defaultTimezone?: string
  adminAccountId?: string
  adminPersonId?: string
}

interface UpdateQuotaRequest {
  limitValue: number
  warningThreshold?: number
}

interface ConsumeQuotaRequest {
  delta: number
}

function mapStatus(
  status: BackendTenantProfile['status'],
): TenantProfile['status'] {
  if (status === 'ACTIVE') {
    return 'enabled'
  }
  if (status === 'DRAFT') {
    return 'draft'
  }
  return 'disabled'
}

function mapQuota(item: BackendTenantQuota): TenantQuota {
  return {
    id: item.id,
    tenantProfileId: item.tenantProfileId,
    quotaType: item.quotaType,
    limitValue: item.limitValue,
    usedValue: item.usedValue,
    warningThreshold: item.warningThreshold,
    warning: item.warning,
  }
}

function mapTenantProfile(item: BackendTenantProfile): TenantProfile {
  return {
    id: item.id,
    name: item.name,
    domain: item.tenantCode,
    status: mapStatus(item.status),
    timezone: item.defaultTimezone ?? '-',
    tenantCode: item.tenantCode,
    packageCode: item.packageCode ?? undefined,
    defaultLocale: item.defaultLocale ?? undefined,
    defaultTimezone: item.defaultTimezone ?? undefined,
    isolationMode: item.isolationMode,
    initialized: item.initialized,
    adminAccountId: item.adminAccountId,
    adminPersonId: item.adminPersonId,
    quotas: (item.quotas ?? []).map(mapQuota),
    updatedAt: item.updatedAt,
  }
}

function buildCreateRequest(payload: TenantProfile): CreateTenantRequest {
  return {
    code: payload.tenantCode ?? payload.domain,
    name: payload.name,
    isolationMode: payload.isolationMode ?? 'SHARED_DB',
    packageCode: payload.packageCode,
    defaultLocale: payload.defaultLocale,
    defaultTimezone: payload.defaultTimezone,
    adminAccountId: payload.adminAccountId ?? undefined,
    adminPersonId: payload.adminPersonId ?? undefined,
  }
}

function buildUpdateRequest(payload: TenantProfile): UpdateTenantRequest {
  return {
    name: payload.name,
    packageCode: payload.packageCode,
    defaultLocale: payload.defaultLocale,
    defaultTimezone: payload.defaultTimezone,
    adminAccountId: payload.adminAccountId ?? undefined,
    adminPersonId: payload.adminPersonId ?? undefined,
  }
}

export const tenantService = {
  async list(query?: InfraListQuery): Promise<InfraPageData<TenantProfile>> {
    const items = await get<BackendTenantProfile[]>(BASE_URL)

    return toPageData(items.map(mapTenantProfile), query)
  },
  async detail(id: string): Promise<TenantProfile> {
    const item = await get<BackendTenantProfile>(`${BASE_URL}/${id}`)

    return mapTenantProfile(item)
  },
  async create(payload: TenantProfile): Promise<TenantProfile> {
    const item = await post<BackendTenantProfile, CreateTenantRequest>(
      BASE_URL,
      buildCreateRequest(payload),
      { dedupeKey: `tenant:create:${payload.tenantCode ?? payload.domain}` },
    )

    return mapTenantProfile(item)
  },
  async update(id: string, payload: TenantProfile): Promise<TenantProfile> {
    const item = await put<BackendTenantProfile, UpdateTenantRequest>(
      `${BASE_URL}/${id}`,
      buildUpdateRequest(payload),
      {
        dedupeKey: `tenant:update:${id}`,
      },
    )

    return mapTenantProfile(item)
  },
  async activate(id: string): Promise<TenantProfile> {
    const item = await post<BackendTenantProfile, Record<string, never>>(
      `${BASE_URL}/${id}/activate`,
      {},
      { dedupeKey: `tenant:activate:${id}` },
    )

    return mapTenantProfile(item)
  },
  async initialize(id: string): Promise<TenantProfile> {
    const item = await post<BackendTenantProfile, Record<string, never>>(
      `${BASE_URL}/${id}/initialize`,
      {},
      { dedupeKey: `tenant:initialize:${id}` },
    )

    return mapTenantProfile(item)
  },
  async disable(id: string): Promise<TenantProfile> {
    const item = await post<BackendTenantProfile, Record<string, never>>(
      `${BASE_URL}/${id}/disable`,
      {},
      { dedupeKey: `tenant:disable:${id}` },
    )

    return mapTenantProfile(item)
  },
  async updateQuota(
    tenantId: string,
    quotaType: TenantQuotaType,
    payload: UpdateQuotaRequest,
  ): Promise<TenantQuota> {
    const item = await put<BackendTenantQuota, UpdateQuotaRequest>(
      `${BASE_URL}/${tenantId}/quotas/${quotaType}`,
      payload,
      { dedupeKey: `tenant:quota:${tenantId}:${quotaType}` },
    )

    return mapQuota(item)
  },
  async consumeQuota(
    tenantId: string,
    quotaType: TenantQuotaType,
    delta: number,
  ): Promise<TenantQuota> {
    const item = await post<BackendTenantQuota, ConsumeQuotaRequest>(
      `${BASE_URL}/${tenantId}/quota-usages/${quotaType}`,
      { delta },
      { dedupeKey: `tenant:quota-usage:${tenantId}:${quotaType}:${delta}` },
    )

    return mapQuota(item)
  },
}
