import type { PageData, PaginationQuery } from '@/types/api'

export type InfraStatus = 'enabled' | 'disabled' | 'draft'
export type AuditAction = 'CREATE' | 'UPDATE' | 'DELETE' | 'LOGIN' | 'READ'
export type SchedulerTaskStatus = 'enabled' | 'disabled' | 'running' | 'failed'

export interface InfraListQuery extends PaginationQuery {
  keyword?: string
}

export interface DictionaryType {
  code: string
  name: string
  description?: string
  status: InfraStatus
  updatedAt?: string
}

export interface DictionaryItem {
  code: string
  label: string
  value: string
  sortOrder: number
  enabled: boolean
  updatedAt?: string
}

export interface ConfigEntry {
  key: string
  value: string
  group: string
  description?: string
  encrypted: boolean
  updatedAt?: string
}

export interface ErrorCodeDefinition {
  code: string
  message: string
  httpStatus: number
  module: string
  description?: string
  updatedAt?: string
}

export interface CachePolicy {
  name: string
  ttlSeconds: number
  maxEntries: number
  enabled: boolean
  updatedAt?: string
}

export interface CacheStats {
  region: string
  hitRate: number
  entryCount: number
  memoryBytes: number
  updatedAt?: string
}

export interface AuditRecord {
  id: string
  actor: string
  action: AuditAction
  resource: string
  clientIp?: string
  createdAt: string
}

export interface AuditFilterValues {
  actor?: string
  action?: AuditAction | ''
  resource?: string
  from?: string
  to?: string
}

export interface TenantProfile {
  id: string
  name: string
  domain: string
  status: InfraStatus
  timezone: string
  updatedAt?: string
}

export interface SecurityPolicy {
  id: string
  name: string
  minPasswordLength: number
  mfaRequired: boolean
  sessionTimeoutMinutes: number
  updatedAt?: string
}

export interface SchedulerTask {
  id: string
  name: string
  cron: string
  status: SchedulerTaskStatus
  lastRunAt?: string
  nextRunAt?: string
}

export interface I18nResource {
  id: string
  locale: string
  namespace: string
  resourceKey: string
  resourceValue: string
  updatedAt?: string
}

export interface DataI18nTranslation {
  id: string
  entityType: string
  entityId: string
  field: string
  locale: string
  translatedValue: string
  updatedAt?: string
}

export interface TimezoneSetting {
  id: string
  tenantId: string
  timezone: string
  displayName: string
  defaultEnabled: boolean
  updatedAt?: string
}

export interface AttachmentAsset {
  id: string
  fileName: string
  contentType: string
  sizeBytes: number
  owner?: string
  createdAt?: string
}

export interface EventBusSubscription {
  id: string
  topic: string
  consumer: string
  endpoint: string
  enabled: boolean
}

export interface EventBusDeliveryStatus {
  id: string
  topic: string
  eventId: string
  consumer: string
  status: 'pending' | 'delivered' | 'failed'
  deliveredAt?: string
}

export type InfraPageData<T> = PageData<T>
