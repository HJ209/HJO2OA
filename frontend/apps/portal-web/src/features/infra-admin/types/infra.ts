import type { PageData, PaginationQuery } from '@/types/api'

export type InfraStatus = 'enabled' | 'disabled' | 'draft'
export type AuditAction = string
export type SchedulerTaskStatus = 'enabled' | 'paused' | 'disabled'
export type SchedulerExecutionStatus =
  | 'RUNNING'
  | 'RETRYING'
  | 'SUCCESS'
  | 'FAILED'
  | 'TIMEOUT'
  | 'CANCELLED'

export interface InfraListQuery extends PaginationQuery {
  keyword?: string
}

export interface DictionaryType {
  id?: string
  code: string
  name: string
  description?: string
  status: InfraStatus
  category?: string
  hierarchical?: boolean
  cacheable?: boolean
  sortOrder?: number
  systemManaged?: boolean
  tenantId?: string | null
  updatedAt?: string
}

export interface DictionaryItem {
  id?: string
  code: string
  label: string
  value: string
  sortOrder: number
  enabled: boolean
  parentId?: string
  defaultItem?: boolean
  extensionJson?: string
  children?: DictionaryItem[]
  updatedAt?: string
}

export interface SystemEnumDictionary {
  code: string
  name: string
  className: string
  category: string
  imported?: boolean
  newItemCodes?: string[]
  changedItemCodes?: string[]
  disabledItemCodes?: string[]
  items: Array<{
    code: string
    name: string
    sortOrder: number
  }>
}

export interface SystemEnumImportResult {
  discoveredTypes: number
  createdTypes: number
  createdItems: number
  updatedItems?: number
  disabledItems?: number
  importedCodes: string[]
}

export interface ConfigEntry {
  id?: string
  key: string
  value: string
  group: string
  description?: string
  encrypted: boolean
  name?: string
  configType?: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON' | 'FEATURE_FLAG'
  defaultValue?: string
  validationRule?: string | null
  mutableAtRuntime?: boolean
  tenantAware?: boolean
  status?: 'ACTIVE' | 'DISABLED' | 'DEPRECATED'
  overrides?: ConfigOverrideRule[]
  featureRules?: FeatureRule[]
  updatedAt?: string
}

export type ConfigOverrideScope = 'TENANT' | 'ORGANIZATION' | 'ROLE' | 'USER'

export interface ConfigOverrideRule {
  id: string
  configEntryId: string
  scopeType: ConfigOverrideScope
  scopeId: string
  overrideValue: string
  active: boolean
}

export type FeatureRuleType =
  | 'GLOBAL'
  | 'TENANT'
  | 'ORG'
  | 'ROLE'
  | 'USER'
  | 'PERCENTAGE'

export interface FeatureRule {
  id: string
  configEntryId: string
  ruleType: FeatureRuleType
  ruleValue: string
  sortOrder: number
  active: boolean
}

export interface ConfigResolutionContext {
  key: string
  tenantId?: string
  orgId?: string
  roleId?: string
  userId?: string
}

export interface ResolvedConfigValue {
  entryId: string
  configKey: string
  configType: string
  status: string
  resolvedValue: string
  sourceType: 'DEFAULT' | 'OVERRIDE' | 'FEATURE_RULE'
  overrideId?: string | null
  featureRuleId?: string | null
  tenantId?: string | null
  orgId?: string | null
  roleId?: string | null
  userId?: string | null
  trace: string[]
}

export type ErrorSeverity = 'INFO' | 'WARN' | 'ERROR' | 'FATAL'

export interface ErrorCodeDefinition {
  id: string
  code: string
  moduleCode: string
  category?: string | null
  severity: ErrorSeverity
  httpStatus: number
  messageKey: string
  message: string
  retryable: boolean
  deprecated: boolean
  createdAt?: string
  updatedAt?: string
}

export interface CachePolicy {
  id?: string
  name: string
  backendType?: 'MEMORY' | 'REDIS' | 'HYBRID'
  ttlSeconds: number
  maxEntries: number
  evictionPolicy?: string
  invalidationMode?: string
  enabled: boolean
  updatedAt?: string
}

export interface CacheStats {
  region: string
  hitRate?: number
  localHitCount?: number
  redisHitCount?: number
  missCount?: number
  putCount?: number
  invalidationCount?: number
  entryCount: number
  memoryBytes: number
  updatedAt?: string
}

export interface CacheRuntimeKey {
  namespace: string
  tenantId?: string | null
  key: string
  backendType: 'MEMORY' | 'REDIS' | 'HYBRID'
  expiresAt?: string | null
}

export interface CacheInvalidation {
  id: string
  cachePolicyId: string
  namespace: string
  invalidateKey: string
  reasonType: string
  reasonRef?: string | null
  invalidatedAt: string
}

export interface AuditRecord {
  id: string
  moduleCode: string
  objectType: string
  objectId: string
  actionType: AuditAction
  actor?: string | null
  action?: AuditAction
  resource?: string
  clientIp?: string | null
  operatorAccountId?: string | null
  operatorPersonId?: string | null
  tenantId?: string | null
  traceId?: string | null
  summary?: string | null
  occurredAt: string
  archiveStatus: 'ACTIVE' | 'ARCHIVED'
  createdAt: string
}

export interface AuditFieldChange {
  id: string
  auditRecordId: string
  fieldName: string
  oldValue?: string | null
  newValue?: string | null
  sensitivityLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | null
}

export interface AuditRecordDetail extends AuditRecord {
  fieldChanges: AuditFieldChange[]
}

export interface AuditFilterValues {
  actor?: string
  action?: AuditAction | ''
  resource?: string
  operatorAccountId?: string
  moduleCode?: string
  objectType?: string
  objectId?: string
  requestId?: string
  from?: string
  to?: string
}

export interface TenantProfile {
  id: string
  name: string
  domain: string
  status: InfraStatus
  timezone: string
  tenantCode?: string
  packageCode?: string
  defaultLocale?: string
  defaultTimezone?: string
  isolationMode?: 'SHARED_DB' | 'DEDICATED_DB'
  initialized?: boolean
  adminAccountId?: string | null
  adminPersonId?: string | null
  quotas?: TenantQuota[]
  updatedAt?: string
}

export type TenantQuotaType =
  | 'USER_COUNT'
  | 'STORAGE'
  | 'ATTACHMENT_STORAGE'
  | 'API_CALL'
  | 'DATA_SIZE'
  | 'JOB_COUNT'

export interface TenantQuota {
  id: string
  tenantProfileId: string
  quotaType: TenantQuotaType
  limitValue: number
  usedValue: number
  warningThreshold?: number | null
  warning: boolean
}

export interface SecurityPolicy {
  id: string
  policyCode: string
  policyType:
    | 'KEY_MANAGEMENT'
    | 'MASKING'
    | 'SIGNATURE'
    | 'PASSWORD'
    | 'ACCESS_CONTROL'
  name: string
  status: 'ACTIVE' | 'DISABLED'
  tenantId?: string | null
  configSnapshot: string
  secretKeys: SecretKeyMaterial[]
  maskingRules: MaskingRule[]
  rateLimitRules: RateLimitRule[]
  minPasswordLength?: number
  mfaRequired?: boolean
  sessionTimeoutMinutes?: number
  updatedAt?: string
}

export interface SecretKeyMaterial {
  id: string
  keyRef: string
  algorithm: string
  keyStatus: 'ACTIVE' | 'ROTATING' | 'REVOKED'
  rotatedAt?: string | null
}

export interface MaskingRule {
  id: string
  dataType: string
  ruleExpr: string
  active: boolean
}

export interface RateLimitRule {
  id: string
  subjectType: 'IP' | 'USER' | 'TENANT' | 'API_CLIENT'
  windowSeconds: number
  maxRequests: number
  active: boolean
}

export interface CreateSecurityPolicyPayload {
  policyCode: string
  policyType: SecurityPolicy['policyType']
  name: string
  configSnapshot: string
  tenantId?: string | null
}

export interface CryptoResult {
  keyRef: string
  algorithm: string
  value: string
}

export interface PasswordValidationResult {
  accepted: boolean
  violations: string[]
}

export interface SchedulerTask {
  id: string
  jobCode: string
  handlerName: string
  name: string
  cron: string
  triggerType: 'CRON' | 'MANUAL' | 'DEPENDENCY'
  timezoneId?: string | null
  concurrencyPolicy: 'ALLOW' | 'FORBID' | 'REPLACE'
  retryPolicy?: string | null
  status: SchedulerTaskStatus
  tenantId?: string | null
  lastRunAt?: string
  nextRunAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface SchedulerExecution {
  id: string
  scheduledJobId: string
  parentExecutionId?: string | null
  triggerSource: 'CRON' | 'MANUAL' | 'RETRY' | 'DEPENDENCY'
  executionStatus: SchedulerExecutionStatus
  startedAt: string
  finishedAt?: string | null
  durationMs?: number | null
  attemptNo: number
  maxAttempts: number
  errorCode?: string | null
  errorMessage?: string | null
  errorStack?: string | null
  executionLog?: string | null
  triggerContext?: string | null
  idempotencyKey?: string | null
  nextRetryAt?: string | null
}

export type I18nBundleStatus = 'DRAFT' | 'ACTIVE' | 'DEPRECATED'

export interface I18nResourceEntry {
  id: string
  localeBundleId: string
  resourceKey: string
  resourceValue: string
  version: number
  active: boolean
}

export interface I18nBundle {
  id: string
  bundleCode: string
  moduleCode: string
  locale: string
  fallbackLocale?: string | null
  status: I18nBundleStatus
  tenantId?: string | null
  createdAt?: string
  updatedAt?: string
  entries: I18nResourceEntry[]
}

export interface ResolvedI18nMessage {
  bundleCode: string
  resourceKey: string
  requestedLocale: string
  resolvedLocale: string
  resourceValue: string
  tenantId?: string | null
  usedFallback: boolean
}

export interface DataI18nTranslation {
  id: string
  entityType: string
  entityId: string
  fieldName: string
  locale: string
  translatedValue: string
  translationStatus: 'TRANSLATED' | 'REVIEWED'
  tenantId: string
  updatedBy?: string | null
  updatedAt?: string
}

export interface TimezoneSetting {
  id: string
  scopeType: 'SYSTEM' | 'TENANT' | 'PERSON'
  scopeId?: string | null
  timezoneId: string
  isDefault: boolean
  effectiveFrom?: string
  active: boolean
  tenantId?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface ResolvedTimezone {
  settingId?: string | null
  tenantId?: string | null
  personId?: string | null
  scopeType: TimezoneSetting['scopeType']
  scopeId?: string | null
  timezoneId: string
  isDefault: boolean
  effectiveFrom?: string
}

export interface TimezoneConversion {
  utcInstant?: string
  localDateTime?: string
  timezoneId: string
}

export interface AttachmentAsset {
  id: string
  storageKey: string
  originalFilename: string
  fileName?: string
  contentType: string
  sizeBytes: number
  checksum: string
  storageProvider: 'LOCAL' | 'MINIO' | 'S3' | 'OSS'
  previewStatus: 'NONE' | 'READY' | 'FAILED' | 'PROCESSING'
  latestVersionNo: number
  permissionMode: 'INHERIT_BUSINESS' | 'CUSTOM'
  tenantId: string
  createdBy?: string | null
  owner?: string | null
  createdAt?: string
  updatedAt?: string
  versions: AttachmentVersion[]
  bindings: AttachmentBinding[]
}

export interface AttachmentVersion {
  id: string
  attachmentAssetId: string
  versionNo: number
  storageKey: string
  checksum: string
  sizeBytes: number
  createdBy?: string | null
  createdAt?: string
}

export interface AttachmentBinding {
  id: string
  attachmentAssetId: string
  businessType: string
  businessId: string
  bindingRole: 'PRIMARY' | 'ATTACHMENT' | 'COVER' | 'PREVIEW_SOURCE'
  active: boolean
}

export interface AttachmentPreview {
  assetId: string
  previewStatus: AttachmentAsset['previewStatus']
  previewAvailable: boolean
  contentType: string
  downloadUrl: string
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

export type EventOutboxStatus = 'PENDING' | 'PUBLISHED' | 'FAILED' | 'DEAD'

export interface EventBusEvent {
  id: string
  eventId: string
  eventType: string
  aggregateType: string
  aggregateId: string
  tenantId: string
  occurredAt: string
  traceId: string
  schemaVersion: string
  status: EventOutboxStatus
  retryCount: number
  nextRetryAt?: string
  publishedAt?: string
  deadAt?: string
  lastError?: string
  createdAt: string
}

export interface EventBusEventDetail extends EventBusEvent {
  payloadJson: string
}

export interface EventBusStatistics {
  pending: number
  published: number
  failed: number
  dead: number
  total: number
}

export interface EventBusQuery extends InfraListQuery {
  eventId?: string
  eventType?: string
  aggregateType?: string
  aggregateId?: string
  tenantId?: string
  traceId?: string
  status?: EventOutboxStatus | ''
  occurredFrom?: string
  occurredTo?: string
}

export interface EventBusReplayRequest {
  eventId?: string
  eventType?: string
  aggregateType?: string
  aggregateId?: string
  tenantId?: string
  traceId?: string
  status?: EventOutboxStatus | ''
  occurredFrom?: string
  occurredTo?: string
  reason: string
}

export interface EventBusReplayResult {
  replayedCount: number
  eventIds: string[]
  replayedAt: string
}

export type InfraPageData<T> = PageData<T>
