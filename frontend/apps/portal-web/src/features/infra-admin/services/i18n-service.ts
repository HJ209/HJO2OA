import { del, get, post, put } from '@/services/request'
import {
  buildListParams,
  toPageData,
} from '@/features/infra-admin/services/service-utils'
import type {
  I18nBundle,
  I18nResourceEntry,
  InfraListQuery,
  InfraPageData,
  ResolvedI18nMessage,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/i18n/bundles'

export interface I18nBundleQuery extends InfraListQuery {
  moduleCode?: string
  locale?: string
  tenantId?: string
}

type BackendI18nEntry = I18nResourceEntry

interface BackendI18nBundle {
  id: string
  bundleCode: string
  moduleCode: string
  locale: string
  fallbackLocale?: string | null
  status: I18nBundle['status']
  tenantId?: string | null
  createdAt?: string
  updatedAt?: string
  entries: BackendI18nEntry[]
}

interface CreateBundleRequest {
  bundleCode: string
  moduleCode: string
  locale: string
  fallbackLocale?: string | null
  tenantId?: string | null
}

interface UpdateBundleRequest {
  moduleCode: string
  fallbackLocale?: string | null
}

interface EntryRequest {
  resourceKey: string
  resourceValue: string
}

interface ResolveRequest {
  bundleCode: string
  resourceKey: string
  locale: string
  tenantId?: string | null
}

function mapBundle(bundle: BackendI18nBundle): I18nBundle {
  return {
    id: bundle.id,
    bundleCode: bundle.bundleCode,
    moduleCode: bundle.moduleCode,
    locale: bundle.locale,
    fallbackLocale: bundle.fallbackLocale ?? null,
    status: bundle.status,
    tenantId: bundle.tenantId ?? null,
    createdAt: bundle.createdAt,
    updatedAt: bundle.updatedAt,
    entries: bundle.entries ?? [],
  }
}

function filterBundles(
  items: I18nBundle[],
  query: I18nBundleQuery,
): I18nBundle[] {
  const keyword = query.keyword?.trim().toLowerCase()

  if (!keyword) {
    return items
  }

  return items.filter((item) =>
    [
      item.bundleCode,
      item.moduleCode,
      item.locale,
      item.fallbackLocale ?? '',
      ...item.entries.flatMap((entry) => [
        entry.resourceKey,
        entry.resourceValue,
      ]),
    ].some((value) => value.toLowerCase().includes(keyword)),
  )
}

function buildBundleParams(query: I18nBundleQuery): URLSearchParams {
  const params = buildListParams(query)

  if (query.moduleCode) {
    params.set('moduleCode', query.moduleCode)
  }

  if (query.locale) {
    params.set('locale', query.locale)
  }

  if (query.tenantId) {
    params.set('tenantId', query.tenantId)
  }

  return params
}

export const i18nService = {
  async list(query: I18nBundleQuery = {}): Promise<InfraPageData<I18nBundle>> {
    const items = await get<BackendI18nBundle[]>(BASE_URL, {
      params: buildBundleParams(query),
    })

    return toPageData(filterBundles(items.map(mapBundle), query), query)
  },
  async create(payload: CreateBundleRequest): Promise<I18nBundle> {
    const bundle = await post<BackendI18nBundle, CreateBundleRequest>(
      BASE_URL,
      payload,
      { dedupeKey: `i18n:create:${payload.bundleCode}:${payload.locale}` },
    )

    return mapBundle(bundle)
  },
  async update(id: string, payload: UpdateBundleRequest): Promise<I18nBundle> {
    const bundle = await put<BackendI18nBundle, UpdateBundleRequest>(
      `${BASE_URL}/${id}`,
      payload,
      { dedupeKey: `i18n:update:${id}` },
    )

    return mapBundle(bundle)
  },
  async activate(id: string): Promise<I18nBundle> {
    const bundle = await put<BackendI18nBundle, undefined>(
      `${BASE_URL}/${id}/activate`,
      undefined,
      { dedupeKey: `i18n:activate:${id}` },
    )

    return mapBundle(bundle)
  },
  async deprecate(id: string): Promise<I18nBundle> {
    const bundle = await put<BackendI18nBundle, undefined>(
      `${BASE_URL}/${id}/deprecate`,
      undefined,
      { dedupeKey: `i18n:deprecate:${id}` },
    )

    return mapBundle(bundle)
  },
  async addEntry(id: string, payload: EntryRequest): Promise<I18nBundle> {
    const bundle = await post<BackendI18nBundle, EntryRequest>(
      `${BASE_URL}/${id}/entries`,
      payload,
      { dedupeKey: `i18n:add-entry:${id}:${payload.resourceKey}` },
    )

    return mapBundle(bundle)
  },
  async updateEntry(id: string, payload: EntryRequest): Promise<I18nBundle> {
    const bundle = await put<
      BackendI18nBundle,
      Pick<EntryRequest, 'resourceValue'>
    >(
      `${BASE_URL}/${id}/entries/${encodeURIComponent(payload.resourceKey)}`,
      { resourceValue: payload.resourceValue },
      { dedupeKey: `i18n:update-entry:${id}:${payload.resourceKey}` },
    )

    return mapBundle(bundle)
  },
  async removeEntry(id: string, resourceKey: string): Promise<I18nBundle> {
    const bundle = await del<BackendI18nBundle>(
      `${BASE_URL}/${id}/entries/${encodeURIComponent(resourceKey)}`,
      { dedupeKey: `i18n:remove-entry:${id}:${resourceKey}` },
    )

    return mapBundle(bundle)
  },
  resolve(payload: ResolveRequest): Promise<ResolvedI18nMessage> {
    return post<ResolvedI18nMessage, ResolveRequest>(
      `${BASE_URL}/resolve`,
      payload,
      {
        dedupeKey: `i18n:resolve:${payload.bundleCode}:${payload.resourceKey}:${payload.locale}`,
      },
    )
  },
}
