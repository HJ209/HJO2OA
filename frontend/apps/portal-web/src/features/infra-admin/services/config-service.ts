import { get, post, put } from '@/services/request'
import {
  buildListParams,
  toPageData,
} from '@/features/infra-admin/services/service-utils'
import type {
  ConfigEntry,
  InfraListQuery,
  InfraPageData,
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

interface BackendConfigEntry {
  id: string
  configKey: string
  name: string
  configType: string
  defaultValue: string
  validationRule?: string | null
  mutableAtRuntime: boolean
  tenantAware: boolean
  status: string
  updatedAt?: string
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

function mapConfigEntry(item: BackendConfigEntry): ConfigEntry {
  return {
    id: item.id,
    key: item.configKey,
    value: item.defaultValue,
    group: item.configType,
    description: item.validationRule ?? undefined,
    encrypted: item.tenantAware,
    updatedAt: item.updatedAt,
  }
}

function buildCreateRequest(payload: ConfigEntry): CreateConfigRequest {
  const requestedType = payload.group?.trim().toUpperCase()

  return {
    configKey: payload.key,
    name: payload.description?.trim() || payload.key,
    configType:
      requestedType && CONFIG_TYPES.has(requestedType)
        ? requestedType
        : DEFAULT_CONFIG_TYPE,
    defaultValue: payload.value,
    mutableAtRuntime: true,
    tenantAware: payload.encrypted,
    validationRule: payload.description,
  }
}

export const configService = {
  async list(query: InfraListQuery = {}): Promise<InfraPageData<ConfigEntry>> {
    const items = await get<BackendConfigEntry[]>(BASE_URL, {
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
}
