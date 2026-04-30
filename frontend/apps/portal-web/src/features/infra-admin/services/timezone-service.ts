import { get, post, put } from '@/services/request'
import {
  buildListParams,
  resolveCurrentTenantId,
  toPageData,
} from '@/features/infra-admin/services/service-utils'
import type {
  InfraListQuery,
  InfraPageData,
  ResolvedTimezone,
  TimezoneConversion,
  TimezoneSetting,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/timezone/settings'

export interface TimezoneQuery extends InfraListQuery {
  tenantId?: string
  scopeType?: TimezoneSetting['scopeType']
}

interface SetTimezoneRequest {
  timezoneId: string
}

function filterSettings(
  items: TimezoneSetting[],
  query: TimezoneQuery,
): TimezoneSetting[] {
  const keyword = query.keyword?.trim().toLowerCase()

  if (!keyword) {
    return items
  }

  return items.filter((item) =>
    [
      item.scopeType,
      item.scopeId ?? '',
      item.timezoneId,
      item.tenantId ?? '',
    ].some((value) => value.toLowerCase().includes(keyword)),
  )
}

function buildTimezoneParams(query: TimezoneQuery): URLSearchParams {
  const params = buildListParams(query)

  if (query.tenantId) {
    params.set('tenantId', query.tenantId)
  }

  if (query.scopeType) {
    params.set('scopeType', query.scopeType)
  }

  return params
}

export const timezoneService = {
  async list(
    query: TimezoneQuery = {},
  ): Promise<InfraPageData<TimezoneSetting>> {
    const items = await get<TimezoneSetting[]>(BASE_URL, {
      params: buildTimezoneParams(query),
    })

    return toPageData(filterSettings(items, query), query)
  },
  setSystemDefault(timezoneId: string): Promise<TimezoneSetting> {
    return put<TimezoneSetting, SetTimezoneRequest>(
      `${BASE_URL}/system`,
      { timezoneId },
      { dedupeKey: `timezone:system:${timezoneId}` },
    )
  },
  setTenantTimezone(
    tenantId: string,
    timezoneId: string,
  ): Promise<TimezoneSetting> {
    return put<TimezoneSetting, SetTimezoneRequest>(
      `${BASE_URL}/tenant/${tenantId}`,
      { timezoneId },
      { dedupeKey: `timezone:tenant:${tenantId}` },
    )
  },
  setPersonTimezone(
    personId: string,
    timezoneId: string,
  ): Promise<TimezoneSetting> {
    return put<TimezoneSetting, SetTimezoneRequest>(
      `${BASE_URL}/person/${personId}`,
      { timezoneId },
      { dedupeKey: `timezone:person:${personId}` },
    )
  },
  async setCurrentTenantTimezone(timezoneId: string): Promise<TimezoneSetting> {
    const tenantId = await resolveCurrentTenantId()

    return this.setTenantTimezone(tenantId, timezoneId)
  },
  resolve(
    params: {
      tenantId?: string
      personId?: string
    } = {},
  ): Promise<ResolvedTimezone> {
    return get<ResolvedTimezone>(`${BASE_URL}/resolve`, {
      params,
    })
  },
  convertToUtc(
    localDateTime: string,
    timezoneId: string,
  ): Promise<TimezoneConversion> {
    return post<
      TimezoneConversion,
      { localDateTime: string; timezoneId: string }
    >(
      `${BASE_URL}/convert/to-utc`,
      { localDateTime, timezoneId },
      { dedupeKey: `timezone:convert-to-utc:${localDateTime}:${timezoneId}` },
    )
  },
  convertFromUtc(
    utcInstant: string,
    timezoneId: string,
  ): Promise<TimezoneConversion> {
    return post<TimezoneConversion, { utcInstant: string; timezoneId: string }>(
      `${BASE_URL}/convert/from-utc`,
      { utcInstant, timezoneId },
      { dedupeKey: `timezone:convert-from-utc:${utcInstant}:${timezoneId}` },
    )
  },
}
