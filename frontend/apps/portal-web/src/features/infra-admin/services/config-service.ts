import { del, get, post, put } from '@/services/request'
import {
  buildListParams,
  toPageData,
} from '@/features/infra-admin/services/service-utils'
import type {
  ConfigEntry,
  ConfigOverrideRule,
  ConfigOverrideScope,
  ConfigResolutionContext,
  FeatureRule,
  FeatureRuleType,
  InfraListQuery,
  InfraPageData,
  ResolvedConfigValue,
} from '@/features/infra-admin/types/infra'

const BASE_URL = '/v1/infra/configs'
const DEFAULT_CONFIG_TYPE = 'STRING'
const CONFIG_TYPES = new Set([
  'STRING',
  'NUMBER',
  'BOOLEAN',
  'JSON',
  'FEATURE_FLAG',
])

interface BackendConfigOverride {
  id: string
  configEntryId: string
  scopeType: ConfigOverrideScope
  scopeId: string
  overrideValue: string
  active: boolean
}

interface BackendFeatureRule {
  id: string
  configEntryId: string
  ruleType: FeatureRuleType
  ruleValue: string
  sortOrder: number
  active: boolean
}

interface BackendConfigEntry {
  id: string
  configKey: string
  name: string
  configType: string
  defaultValue: string
  validationRule?: string | null
  mutableAtRuntime: boolean
  tenantAware: boolean
  status: 'ACTIVE' | 'DISABLED' | 'DEPRECATED'
  updatedAt?: string
  overrides?: BackendConfigOverride[]
  featureRules?: BackendFeatureRule[]
}

interface CreateConfigRequest {
  configKey: string
  name: string
  configType: string
  defaultValue: string
  mutableAtRuntime: boolean
  tenantAware: boolean
  validationRule?: string
}

interface UpdateDefaultValueRequest {
  defaultValue: string
}

interface AddOverrideRequest {
  scopeType: ConfigOverrideScope
  scopeId: string
  overrideValue: string
}

interface AddFeatureRuleRequest {
  ruleType: FeatureRuleType
  ruleValue: string
  sortOrder?: number
}

interface UpdateFeatureRuleRequest {
  ruleType?: FeatureRuleType
  ruleValue?: string
  sortOrder?: number
  active?: boolean
}

function mapOverride(item: BackendConfigOverride): ConfigOverrideRule {
  return {
    id: item.id,
    configEntryId: item.configEntryId,
    scopeType: item.scopeType,
    scopeId: item.scopeId,
    overrideValue: item.overrideValue,
    active: item.active,
  }
}

function mapFeatureRule(item: BackendFeatureRule): FeatureRule {
  return {
    id: item.id,
    configEntryId: item.configEntryId,
    ruleType: item.ruleType,
    ruleValue: item.ruleValue,
    sortOrder: item.sortOrder,
    active: item.active,
  }
}

function mapConfigEntry(item: BackendConfigEntry): ConfigEntry {
  return {
    id: item.id,
    key: item.configKey,
    value: item.defaultValue,
    group: item.configType,
    description: item.validationRule ?? undefined,
    encrypted: item.tenantAware,
    name: item.name,
    configType: item.configType as ConfigEntry['configType'],
    defaultValue: item.defaultValue,
    validationRule: item.validationRule ?? null,
    mutableAtRuntime: item.mutableAtRuntime,
    tenantAware: item.tenantAware,
    status: item.status,
    overrides: (item.overrides ?? []).map(mapOverride),
    featureRules: (item.featureRules ?? []).map(mapFeatureRule),
    updatedAt: item.updatedAt,
  }
}

function buildCreateRequest(payload: ConfigEntry): CreateConfigRequest {
  const requestedType = payload.group?.trim().toUpperCase()

  return {
    configKey: payload.key,
    name: payload.name?.trim() || payload.description?.trim() || payload.key,
    configType:
      requestedType && CONFIG_TYPES.has(requestedType)
        ? requestedType
        : DEFAULT_CONFIG_TYPE,
    defaultValue: payload.value,
    mutableAtRuntime: payload.mutableAtRuntime ?? true,
    tenantAware: payload.tenantAware ?? payload.encrypted,
    validationRule: payload.validationRule ?? payload.description,
  }
}

export const configService = {
  async list(query: InfraListQuery = {}): Promise<InfraPageData<ConfigEntry>> {
    const items = await get<BackendConfigEntry[]>(BASE_URL, {
      params: buildListParams(query),
    })

    return toPageData(items.map(mapConfigEntry), query)
  },
  async listFeatureFlags(
    query: InfraListQuery = {},
  ): Promise<InfraPageData<ConfigEntry>> {
    const items = await get<BackendConfigEntry[]>('/v1/infra/feature-flags', {
      params: buildListParams(query),
    })

    return toPageData(items.map(mapConfigEntry), query)
  },
  async create(payload: ConfigEntry): Promise<ConfigEntry> {
    const item = await post<BackendConfigEntry, CreateConfigRequest>(
      BASE_URL,
      buildCreateRequest(payload),
      {
        dedupeKey: `config:create:${payload.key}`,
      },
    )

    return mapConfigEntry(item)
  },
  async update(id: string, payload: ConfigEntry): Promise<ConfigEntry> {
    const item = await put<BackendConfigEntry, UpdateDefaultValueRequest>(
      `${BASE_URL}/${id}/default`,
      { defaultValue: payload.value },
      {
        dedupeKey: `config:update:${id}`,
      },
    )

    return mapConfigEntry(item)
  },
  async addOverride(
    entryId: string,
    payload: AddOverrideRequest,
  ): Promise<ConfigEntry> {
    const item = await post<BackendConfigEntry, AddOverrideRequest>(
      `${BASE_URL}/${entryId}/overrides`,
      payload,
      {
        dedupeKey: `config:override:${entryId}:${payload.scopeType}:${payload.scopeId}`,
      },
    )

    return mapConfigEntry(item)
  },
  async removeOverride(
    entryId: string,
    overrideId: string,
  ): Promise<ConfigEntry> {
    const item = await del<BackendConfigEntry>(
      `${BASE_URL}/${entryId}/overrides/${overrideId}`,
      { dedupeKey: `config:override:remove:${overrideId}` },
    )

    return mapConfigEntry(item)
  },
  async disableOverride(overrideId: string): Promise<ConfigEntry> {
    const item = await post<BackendConfigEntry, Record<string, never>>(
      `/v1/infra/config-overrides/${overrideId}/disable`,
      {},
      { dedupeKey: `config:override:disable:${overrideId}` },
    )

    return mapConfigEntry(item)
  },
  async addFeatureRule(
    entryId: string,
    payload: AddFeatureRuleRequest,
  ): Promise<ConfigEntry> {
    const item = await post<BackendConfigEntry, AddFeatureRuleRequest>(
      `${BASE_URL}/${entryId}/feature-rules`,
      payload,
      {
        dedupeKey: `config:feature-rule:${entryId}:${payload.sortOrder ?? 'next'}`,
      },
    )

    return mapConfigEntry(item)
  },
  async updateFeatureRule(
    ruleId: string,
    payload: UpdateFeatureRuleRequest,
  ): Promise<ConfigEntry> {
    const item = await put<BackendConfigEntry, UpdateFeatureRuleRequest>(
      `/v1/infra/feature-rules/${ruleId}`,
      payload,
      { dedupeKey: `config:feature-rule:update:${ruleId}` },
    )

    return mapConfigEntry(item)
  },
  resolve(context: ConfigResolutionContext): Promise<ResolvedConfigValue> {
    return post<ResolvedConfigValue, ConfigResolutionContext>(
      `${BASE_URL}/resolve`,
      context,
      { dedupeKey: `config:resolve:${context.key}` },
    )
  },
}
