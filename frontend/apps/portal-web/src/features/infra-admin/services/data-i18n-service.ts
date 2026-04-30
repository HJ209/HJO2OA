import { get, post, put } from '@/services/request'
import {
  buildListParams,
  resolveCurrentTenantId,
  toPageData,
} from '@/features/infra-admin/services/service-utils'
import type {
  DataI18nTranslation,
  InfraListQuery,
  InfraPageData,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/data-i18n/translations'

export interface DataI18nQuery extends InfraListQuery {
  tenantId?: string
  entityType?: string
  locale?: string
}

export interface TranslationCreateRequest {
  entityType: string
  entityId: string
  fieldName: string
  locale: string
  value: string
  tenantId: string
}

export interface TranslationResolveRequest {
  entityType: string
  entityId: string
  fieldName: string
  locale: string
  tenantId: string
  fallbackLocale?: string
  originalValue?: string
}

export interface TranslationResolveResult {
  entryId?: string | null
  entityType: string
  entityId: string
  fieldName: string
  requestedLocale: string
  resolvedLocale?: string | null
  resolvedValue: string
  translationStatus?: DataI18nTranslation['translationStatus'] | null
  resolveSource: 'EXACT' | 'FALLBACK' | 'ORIGINAL_VALUE'
  fallbackApplied: boolean
  tenantId: string
}

function filterTranslations(
  items: DataI18nTranslation[],
  query: DataI18nQuery,
): DataI18nTranslation[] {
  const keyword = query.keyword?.trim().toLowerCase()

  if (!keyword) {
    return items
  }

  return items.filter((item) =>
    [
      item.entityType,
      item.entityId,
      item.fieldName,
      item.locale,
      item.translatedValue,
      item.tenantId,
    ].some((value) => value.toLowerCase().includes(keyword)),
  )
}

async function buildTranslationParams(
  query: DataI18nQuery,
): Promise<URLSearchParams> {
  const params = buildListParams(query)
  const tenantId = query.tenantId ?? (await resolveCurrentTenantId())

  params.set('tenantId', tenantId)

  if (query.entityType) {
    params.set('entityType', query.entityType)
  }

  if (query.locale) {
    params.set('locale', query.locale)
  }

  return params
}

export const dataI18nService = {
  async list(
    query: DataI18nQuery = {},
  ): Promise<InfraPageData<DataI18nTranslation>> {
    const items = await get<DataI18nTranslation[]>(BASE_URL, {
      params: await buildTranslationParams(query),
    })

    return toPageData(filterTranslations(items, query), query)
  },
  create(payload: TranslationCreateRequest): Promise<DataI18nTranslation> {
    return post<DataI18nTranslation, TranslationCreateRequest>(
      BASE_URL,
      payload,
      {
        dedupeKey: `data-i18n:create:${payload.tenantId}:${payload.entityType}:${payload.entityId}:${payload.fieldName}:${payload.locale}`,
      },
    )
  },
  update(id: string, value: string): Promise<DataI18nTranslation> {
    return put<DataI18nTranslation, { value: string }>(
      `${BASE_URL}/${id}`,
      { value },
      { dedupeKey: `data-i18n:update:${id}` },
    )
  },
  review(id: string): Promise<DataI18nTranslation> {
    return put<DataI18nTranslation, undefined>(
      `${BASE_URL}/${id}/review`,
      undefined,
      { dedupeKey: `data-i18n:review:${id}` },
    )
  },
  batchSave(
    entries: TranslationCreateRequest[],
  ): Promise<DataI18nTranslation[]> {
    return post<DataI18nTranslation[], { entries: TranslationCreateRequest[] }>(
      `${BASE_URL}/batch`,
      { entries },
      {
        dedupeKey: `data-i18n:batch:${entries
          .map(
            (entry) =>
              `${entry.tenantId}:${entry.entityType}:${entry.entityId}:${entry.fieldName}:${entry.locale}`,
          )
          .join('|')}`,
      },
    )
  },
  resolve(
    payload: TranslationResolveRequest,
  ): Promise<TranslationResolveResult> {
    return post<TranslationResolveResult, TranslationResolveRequest>(
      `${BASE_URL}/resolve`,
      payload,
      {
        dedupeKey: `data-i18n:resolve:${payload.tenantId}:${payload.entityType}:${payload.entityId}:${payload.fieldName}:${payload.locale}`,
      },
    )
  },
}
