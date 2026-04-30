import type { PageData } from '@/types/api'

export type ConnectorType = 'HTTP' | 'DATABASE' | 'MQ' | 'FILE' | 'SAAS'
export type ConnectorAuthMode = 'BASIC' | 'TOKEN' | 'SECRET_REF' | 'NONE'
export type ConnectorStatus = 'DRAFT' | 'ACTIVE' | 'DISABLED'
export type ConnectorHealthStatus = 'HEALTHY' | 'DEGRADED' | 'UNREACHABLE'
export type ConnectorCheckType = 'MANUAL_TEST' | 'HEALTH_CHECK'
export type ConnectorParameterTemplateCategory =
  | 'ENDPOINT'
  | 'AUTH'
  | 'HEALTH'
  | 'ADVANCED'

export interface TimeoutRetryConfig {
  connectTimeoutMs: number
  readTimeoutMs: number
  retryCount: number
  retryIntervalMs: number
}

export interface ConnectorParameter {
  paramKey: string
  paramValueRef: string
  sensitive: boolean
}

export interface ConnectorParameterTemplate {
  paramKey: string
  displayName: string
  category: ConnectorParameterTemplateCategory
  required: boolean
  sensitive: boolean
  description?: string | null
}

export interface ConnectorHealthSnapshot {
  snapshotId: string
  connectorId: string
  checkType: ConnectorCheckType
  healthStatus: ConnectorHealthStatus
  latencyMs: number
  errorCode?: string | null
  errorSummary?: string | null
  operatorId?: string | null
  targetEnvironment: string
  confirmedBy?: string | null
  confirmationNote?: string | null
  confirmedAt?: string | null
  changeSequence: number
  checkedAt: string
}

export interface ConnectorHealthOverview {
  connectorId: string
  latestHealthSnapshot?: ConnectorHealthSnapshot | null
  lastFailureSnapshot?: ConnectorHealthSnapshot | null
  sampleSize: number
  healthyCount: number
  degradedCount: number
  unreachableCount: number
}

export interface ConnectorSummary {
  connectorId: string
  tenantId: string
  code: string
  name: string
  connectorType: ConnectorType
  vendor?: string | null
  protocol?: string | null
  authMode: ConnectorAuthMode
  timeoutConfig: TimeoutRetryConfig
  status: ConnectorStatus
  changeSequence: number
  latestTestSnapshot?: ConnectorHealthSnapshot | null
  latestHealthSnapshot?: ConnectorHealthSnapshot | null
  createdAt: string
  updatedAt: string
}

export interface ConnectorDetail extends ConnectorSummary {
  parameters: ConnectorParameter[]
}

export interface ConnectorListView extends PageData<ConnectorSummary> {
  filters?: {
    connectorType?: ConnectorType | null
    status?: ConnectorStatus | null
    code?: string | null
    keyword?: string | null
  }
}

export interface UpsertConnectorRequest {
  code: string
  name: string
  connectorType: ConnectorType
  vendor?: string
  protocol?: string
  authMode: ConnectorAuthMode
  timeoutConfig?: TimeoutRetryConfig
}

export type DataServiceType = 'QUERY' | 'COMMAND' | 'EXPORT' | 'CALLBACK'
export type DataServiceSourceMode = 'INTERNAL_QUERY' | 'CONNECTOR' | 'MIXED'
export type DataServicePermissionMode =
  | 'PUBLIC_INTERNAL'
  | 'APP_SCOPED'
  | 'SUBJECT_SCOPED'
export type DataServiceStatus = 'DRAFT' | 'ACTIVE' | 'DEPRECATED' | 'DISABLED'
export type DataServiceCacheScope = 'TENANT' | 'APP' | 'SUBJECT' | 'GLOBAL'
export type DataServiceParameterType =
  | 'STRING'
  | 'NUMBER'
  | 'BOOLEAN'
  | 'DATE'
  | 'JSON'
  | 'PAGEABLE'
export type DataServiceTransformType =
  | 'DIRECT'
  | 'TRIM'
  | 'UPPERCASE'
  | 'LOWERCASE'
  | 'DATE_FORMAT'
  | 'CONSTANT'

export interface DataServicePermissionBoundary {
  allowedAppCodes: string[]
  allowedSubjectIds: string[]
  requiredRoles: string[]
}

export interface DataServiceCachePolicy {
  enabled: boolean
  ttlSeconds?: number | null
  cacheKeyTemplate?: string | null
  scope?: DataServiceCacheScope | null
  cacheNullValue: boolean
  invalidationEvents: string[]
}

export interface DataServiceValidationRule {
  minLength?: number | null
  maxLength?: number | null
  minValue?: number | null
  maxValue?: number | null
  regex?: string | null
  allowedValues: string[]
  maxPageSize?: number | null
}

export interface DataServiceParameter {
  parameterId?: string | null
  paramCode: string
  paramType: DataServiceParameterType
  required: boolean
  defaultValue?: string | null
  validationRule?: DataServiceValidationRule | null
  enabled: boolean
  description?: string | null
  sortOrder: number
}

export interface DataServiceTransformRule {
  type: DataServiceTransformType
  expression?: string | null
  formatPattern?: string | null
  constantValue?: string | null
}

export interface DataServiceFieldMapping {
  mappingId?: string | null
  sourceField: string
  targetField: string
  transformRule?: DataServiceTransformRule | null
  masked: boolean
  description?: string | null
  sortOrder: number
}

export interface DataServiceSummary {
  serviceId: string
  tenantId: string
  code: string
  name: string
  serviceType: DataServiceType
  sourceMode: DataServiceSourceMode
  permissionMode: DataServicePermissionMode
  status: DataServiceStatus
  cacheEnabled: boolean
  activatedAt?: string | null
  openApiReferenceCount: number
  reportReferenceCount: number
  syncReferenceCount: number
  openApiReusable: boolean
  reportReusable: boolean
  createdAt: string
  updatedAt: string
}

export interface DataServiceDetail extends DataServiceSummary {
  permissionBoundary: DataServicePermissionBoundary
  cachePolicy: DataServiceCachePolicy
  sourceRef: string
  connectorId?: string | null
  description?: string | null
  statusSequence: number
  createdBy?: string | null
  updatedBy?: string | null
  parameters: DataServiceParameter[]
  fieldMappings: DataServiceFieldMapping[]
}

export interface SaveDataServiceRequest {
  serviceId?: string
  code: string
  name: string
  serviceType: DataServiceType
  sourceMode: DataServiceSourceMode
  permissionMode: DataServicePermissionMode
  permissionBoundary?: DataServicePermissionBoundary
  cachePolicy?: DataServiceCachePolicy
  sourceRef: string
  connectorId?: string
  description?: string
  parameters?: DataServiceParameter[]
  fieldMappings?: DataServiceFieldMapping[]
}

export interface DataServiceInvocationRequest {
  appCode?: string
  subjectId?: string
  parameters?: Record<string, unknown>
}

export interface DataServiceExecutionPlan {
  serviceId: string
  serviceCode: string
  serviceType: DataServiceType
  sourceMode: DataServiceSourceMode
  permissionMode: DataServicePermissionMode
  cacheEnabled: boolean
  cacheKey?: string | null
  cacheTtlSeconds?: number | null
  appCode?: string | null
  subjectId?: string | null
  idempotencyKey?: string | null
  normalizedParameters: Record<string, unknown>
  outputMappings: DataServiceFieldMapping[]
  outputFields: string[]
  openApiReusable: boolean
  reportReusable: boolean
  preparedAt: string
}

export type SyncTaskType = 'IMPORT' | 'EXPORT' | 'BIDIRECTIONAL'
export type SyncMode = 'FULL' | 'INCREMENTAL' | 'EVENT_DRIVEN'
export type CheckpointMode = 'OFFSET' | 'TIMESTAMP' | 'VERSION' | 'NONE'
export type SyncTaskStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'ERROR'
export type ConnectorDependencyStatus =
  | 'READY'
  | 'SOURCE_UNAVAILABLE'
  | 'TARGET_UNAVAILABLE'
  | 'BOTH_UNAVAILABLE'
export type ExecutionStatus = 'RUNNING' | 'SUCCESS' | 'FAILED' | 'COMPENSATING'
export type ExecutionTriggerType =
  | 'MANUAL'
  | 'SCHEDULED'
  | 'EVENT_DRIVEN'
  | 'RETRY'
  | 'RECONCILIATION'
  | 'COMPENSATION'
export type ConflictStrategy = 'OVERWRITE' | 'SKIP' | 'MERGE' | 'MANUAL'
export type ReconciliationStatus =
  | 'NOT_CHECKED'
  | 'CONSISTENT'
  | 'DIFFERENCES_FOUND'
  | 'MANUAL_REVIEW_REQUIRED'
export type DifferenceStatus =
  | 'DETECTED'
  | 'IGNORED'
  | 'MANUAL_CONFIRMED'
  | 'COMPENSATED'
export type DifferenceType =
  | 'MISSING_TARGET'
  | 'EXTRA_TARGET'
  | 'VALUE_MISMATCH'
  | 'CONFLICT'
  | 'WRITE_FAILURE'
export type CompensationAction =
  | 'RETRY_WRITE'
  | 'IGNORE_DIFFERENCE'
  | 'MANUAL_CONFIRMED'

export interface SyncResultSummary {
  sourceCount: number
  insertedCount: number
  updatedCount: number
  skippedCount: number
  failedCount: number
}

export interface SyncDiffSummary {
  differenceCount: number
  pendingCount: number
  ignoredCount: number
  manualConfirmedCount: number
  compensatedCount: number
  reconciliationStatus: ReconciliationStatus
}

export interface SyncExecutionSummary {
  executionId: string
  taskId: string
  parentExecutionId?: string | null
  taskCode?: string | null
  executionBatchNo?: string | null
  triggerType?: ExecutionTriggerType | null
  executionStatus: ExecutionStatus
  checkpointValue?: string | null
  retryable: boolean
  retryCount: number
  failureCode?: string | null
  failureMessage?: string | null
  resultSummary?: SyncResultSummary | null
  diffSummary?: SyncDiffSummary | null
  reconciliationStatus?: ReconciliationStatus | null
  startedAt?: string | null
  finishedAt?: string | null
}

export interface SyncTaskSummary {
  taskId: string
  tenantId: string
  code: string
  name: string
  description?: string | null
  taskType: SyncTaskType
  syncMode: SyncMode
  sourceConnectorId?: string | null
  targetConnectorId?: string | null
  dependencyStatus: ConnectorDependencyStatus
  checkpointMode?: CheckpointMode | null
  status: SyncTaskStatus
  latestCheckpoint?: string | null
  latestExecution?: SyncExecutionSummary | null
  createdAt: string
  updatedAt: string
}

export interface SyncCheckpointConfig {
  checkpointField?: string | null
  idempotencyField?: string | null
  allowManualReset: boolean
  initialValue?: string | null
  currentValue?: string | null
}

export interface SyncTriggerConfig {
  manualTriggerEnabled: boolean
  eventPatterns: string[]
  schedulerJobCode?: string | null
}

export interface SyncRetryPolicy {
  maxRetries: number
  manualRetryEnabled: boolean
  automaticRetryEnabled: boolean
  retryableErrorCodes: string[]
}

export interface SyncCompensationPolicy {
  manualCompensationEnabled: boolean
  allowIgnoreDifference: boolean
  requireReason: boolean
  maxCompensationAttempts: number
}

export interface SyncReconciliationPolicy {
  enabled: boolean
  checkExtraTargetRecords: boolean
  failWhenDifferenceDetected: boolean
  manualReviewThreshold: number
}

export interface SyncScheduleConfig {
  enabled: boolean
  cron?: string | null
  zoneId?: string | null
  schedulerJobCode?: string | null
}

export interface SyncMappingRule {
  mappingId?: string | null
  sourceField: string
  targetField: string
  transformRule?: Record<string, unknown> | null
  conflictStrategy: ConflictStrategy
  keyMapping: boolean
  sortOrder: number
}

export interface SyncTaskDetail {
  summary: SyncTaskSummary
  checkpointConfig?: SyncCheckpointConfig | null
  triggerConfig?: SyncTriggerConfig | null
  retryPolicy?: SyncRetryPolicy | null
  compensationPolicy?: SyncCompensationPolicy | null
  reconciliationPolicy?: SyncReconciliationPolicy | null
  scheduleConfig?: SyncScheduleConfig | null
  mappingRules: SyncMappingRule[]
}

export interface SyncDifferenceItem {
  differenceCode: string
  differenceType: DifferenceType
  status: DifferenceStatus
  recordKey: string
  fieldName?: string | null
  expectedPayload: Record<string, unknown>
  actualPayload: Record<string, unknown>
  message?: string | null
  detectedAt: string
  resolvedBy?: string | null
  resolutionReason?: string | null
  resolvedAt?: string | null
}

export interface SyncExecutionDetail {
  summary: SyncExecutionSummary
  differences: SyncDifferenceItem[]
  triggerContext: Record<string, unknown>
  operatorAccountId?: string | null
  operatorPersonId?: string | null
}

export interface SaveSyncTaskRequest {
  tenantId: string
  code?: string
  name: string
  description?: string
  taskType: SyncTaskType
  syncMode: SyncMode
  sourceConnectorId: string
  targetConnectorId: string
  checkpointMode: CheckpointMode
  checkpointConfig?: SyncCheckpointConfig
  triggerConfig?: SyncTriggerConfig
  retryPolicy?: SyncRetryPolicy
  compensationPolicy?: SyncCompensationPolicy
  reconciliationPolicy?: SyncReconciliationPolicy
  scheduleConfig?: SyncScheduleConfig
  mappingRules: SyncMappingRule[]
}

export interface OperatorActionRequest {
  idempotencyKey?: string
  operatorAccountId: string
  operatorPersonId?: string
  reason: string
}

export type OpenApiHttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'
export type OpenApiStatus = 'DRAFT' | 'ACTIVE' | 'DEPRECATED' | 'OFFLINE'
export type OpenApiAuthType = 'APP_KEY' | 'SIGNATURE' | 'OAUTH2' | 'INTERNAL'
export type ApiCredentialStatus = 'ACTIVE' | 'REVOKED' | 'EXPIRED'
export type ApiPolicyType = 'RATE_LIMIT' | 'QUOTA'
export type ApiPolicyStatus = 'ACTIVE' | 'DISABLED'
export type ApiWindowUnit = 'SECOND' | 'MINUTE' | 'HOUR' | 'DAY' | 'MONTH'
export type ApiInvocationOutcome =
  | 'SUCCESS'
  | 'AUTH_FAILED'
  | 'RATE_LIMITED'
  | 'QUOTA_EXCEEDED'
  | 'DEPENDENCY_ERROR'
  | 'ERROR'

export interface OpenApiInvocationSummary {
  totalCount: number
  successCount: number
  failureCount: number
  avgDurationMs: number
  lastInvokedAt?: string | null
}

export interface OpenApiEndpointSummary {
  apiId: string
  code: string
  name: string
  path: string
  httpMethod: OpenApiHttpMethod
  version: string
  authType: OpenApiAuthType
  status: OpenApiStatus
  dataServiceCode: string
  dataServiceName?: string | null
  invocationSummary?: OpenApiInvocationSummary | null
  recentAlertSummary?: string | null
  publishedAt?: string | null
  deprecatedAt?: string | null
  createdAt: string
  updatedAt: string
}

export interface ApiCredentialGrant {
  grantId: string
  openApiId: string
  tenantId: string
  clientCode: string
  secretRef: string
  scopes: string[]
  expiresAt?: string | null
  status: ApiCredentialStatus
  createdAt: string
  updatedAt: string
}

export interface ApiRateLimitPolicy {
  policyId: string
  policyCode: string
  clientCode?: string | null
  policyType: ApiPolicyType
  windowValue: number
  windowUnit: ApiWindowUnit
  threshold: number
  status: ApiPolicyStatus
  description?: string | null
  currentWindowUsedCount?: number
  currentWindowStartedAt?: string | null
  createdAt: string
  updatedAt: string
}

export interface OpenApiVersionRelation {
  version: string
  status: OpenApiStatus
  publishedAt?: string | null
  deprecatedAt?: string | null
  sunsetAt?: string | null
}

export interface OpenApiEndpointDetail extends OpenApiEndpointSummary {
  dataServiceId?: string | null
  compatibilityNotes?: string | null
  sunsetAt?: string | null
  credentialGrants: ApiCredentialGrant[]
  rateLimitPolicies: ApiRateLimitPolicy[]
  versions: OpenApiVersionRelation[]
}

export interface ApiInvocationAuditLog {
  logId: string
  requestId: string
  tenantId: string
  apiId?: string | null
  endpointCode?: string | null
  endpointVersion?: string | null
  path: string
  httpMethod: OpenApiHttpMethod
  clientCode?: string | null
  authType?: OpenApiAuthType | null
  outcome: ApiInvocationOutcome
  responseStatus: number
  errorCode?: string | null
  durationMs: number
  requestDigest?: string | null
  remoteIp?: string | null
  occurredAt: string
  abnormalFlag: boolean
  reviewConclusion?: string | null
  note?: string | null
  reviewedBy?: string | null
  reviewedAt?: string | null
}

export interface UpsertOpenApiEndpointRequest {
  name: string
  dataServiceCode: string
  path: string
  httpMethod: OpenApiHttpMethod
  authType: OpenApiAuthType
  compatibilityNotes?: string
}

export type ReportType = 'TREND' | 'RANK' | 'SUMMARY' | 'CARD'
export type ReportSourceScope =
  | 'PROCESS'
  | 'CONTENT'
  | 'MEETING'
  | 'TASK'
  | 'ATTENDANCE'
  | 'MESSAGE'
  | 'MIXED'
export type ReportRefreshMode = 'SCHEDULED' | 'EVENT_DRIVEN' | 'ON_DEMAND'
export type ReportVisibilityMode = 'INTERNAL' | 'PORTAL_CARD' | 'OPEN_API'
export type ReportStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED'
export type ReportMetricAggregationType =
  | 'COUNT'
  | 'SUM'
  | 'AVG'
  | 'DISTINCT'
  | 'RATIO'
export type ReportDimensionType =
  | 'TIME'
  | 'ORGANIZATION'
  | 'ROLE'
  | 'CATEGORY'
  | 'SUBJECT'
  | 'CUSTOM'
export type ReportTimeGranularity = 'NONE' | 'DAY' | 'WEEK' | 'MONTH'
export type ReportCardType = 'SUMMARY' | 'TREND' | 'RANK' | 'MIXED'
export type ReportFreshnessStatus = 'READY' | 'STALE' | 'FAILED'

export interface ReportMetric {
  id?: string | null
  metricCode: string
  metricName: string
  aggregationType: ReportMetricAggregationType
  sourceField?: string | null
  formula?: string | null
  filterExpression?: string | null
  unit?: string | null
  trendEnabled: boolean
  rankEnabled: boolean
  displayOrder: number
}

export interface ReportDimension {
  id?: string | null
  dimensionCode: string
  dimensionName: string
  dimensionType: ReportDimensionType
  sourceField: string
  timeGranularity?: ReportTimeGranularity | null
  filterable: boolean
  displayOrder: number
}

export interface ReportDefinition {
  code: string
  name: string
  reportType: ReportType
  sourceScope: ReportSourceScope
  refreshMode: ReportRefreshMode
  visibilityMode: ReportVisibilityMode
  status: ReportStatus
  tenantId: string
  definitionVersion?: number | null
  lastRefreshedAt?: string | null
  lastFreshnessStatus?: ReportFreshnessStatus | null
  nextRefreshAt?: string | null
  caliber: {
    sourceProviderKey: string
    subjectCode: string
    defaultTimeField?: string | null
    organizationField?: string | null
    dataServiceCode?: string | null
    baseFilters: Record<string, string>
    triggerEventTypes: string[]
    description?: string | null
  }
  refreshConfig?: {
    refreshIntervalSeconds?: number | null
    staleAfterSeconds?: number | null
    maxRows?: number | null
  } | null
  cardProtocol?: {
    cardCode: string
    title: string
    cardType: ReportCardType
    summaryMetricCode?: string | null
    trendMetricCode?: string | null
    rankMetricCode?: string | null
    rankDimensionCode?: string | null
    maxItems?: number | null
  } | null
  metrics: ReportMetric[]
  dimensions: ReportDimension[]
}

export interface ReportSnapshot {
  id: string
  snapshotAt: string
  refreshBatch?: string | null
  freshnessStatus: ReportFreshnessStatus
  triggerMode: string
  triggerReason?: string | null
  errorMessage?: string | null
  rowCount: number
}

export interface ReportMetricValue {
  metricCode: string
  metricName?: string | null
  value: number
  unit?: string | null
}

export interface ReportSummaryPreview {
  reportCode: string
  reportName: string
  refreshedAt?: string | null
  freshnessStatus?: ReportFreshnessStatus | null
  metrics: ReportMetricValue[]
}

export interface ReportTrendPoint {
  bucket: string
  value: number
  label?: string | null
}

export interface ReportTrendPreview {
  reportCode: string
  reportName: string
  dimensionCode?: string | null
  metricCode?: string | null
  refreshedAt?: string | null
  freshnessStatus?: ReportFreshnessStatus | null
  points: ReportTrendPoint[]
}

export interface ReportRankingItem {
  rank: number
  dimensionValue: string
  metricValue: number
  label?: string | null
}

export interface ReportRankingPreview {
  reportCode: string
  reportName: string
  dimensionCode?: string | null
  metricCode?: string | null
  refreshedAt?: string | null
  freshnessStatus?: ReportFreshnessStatus | null
  items: ReportRankingItem[]
}

export type GovernanceScopeType =
  | 'API'
  | 'CONNECTOR'
  | 'SYNC'
  | 'REPORT'
  | 'MODULE'
export type GovernanceProfileStatus = 'ACTIVE' | 'DISABLED'
export type GovernanceHealthStatus =
  | 'HEALTHY'
  | 'DEGRADED'
  | 'UNHEALTHY'
  | 'UNKNOWN'
export type AlertLevel = 'INFO' | 'WARN' | 'ERROR' | 'CRITICAL'
export type AlertStatus = 'OPEN' | 'ACKNOWLEDGED' | 'ESCALATED' | 'CLOSED'
export type TraceStatus = 'OPEN' | 'INVESTIGATING' | 'COMPENSATED' | 'RESOLVED'
export type GovernanceActionType =
  | 'UPSERT_PROFILE'
  | 'UPSERT_HEALTH_RULE'
  | 'UPSERT_ALERT_RULE'
  | 'REGISTER_VERSION'
  | 'PUBLISH_VERSION'
  | 'DEPRECATE_VERSION'
  | 'RUN_HEALTH_CHECK'
  | 'ACKNOWLEDGE_ALERT'
  | 'ESCALATE_ALERT'
  | 'CLOSE_ALERT'
  | 'REQUEST_DISABLE'
  | 'REQUEST_RETRY'
  | 'REQUEST_DEGRADE'
  | 'REQUEST_COMPENSATION'
  | 'ADD_NOTE'
export type ServiceVersionStatus =
  | 'REGISTERED'
  | 'PUBLISHED'
  | 'DEPRECATED'
  | 'ROLLED_BACK'

export interface GovernanceProfile {
  governanceId: string
  code: string
  scopeType: GovernanceScopeType
  targetCode: string
  slaPolicyJson?: string | null
  alertPolicyJson?: string | null
  status: GovernanceProfileStatus
  tenantId: string
  createdAt: string
  updatedAt: string
}

export interface GovernanceHealthSnapshot {
  snapshotId: string
  governanceId: string
  ruleId: string
  targetType: GovernanceScopeType
  targetCode: string
  ruleCode: string
  healthStatus: GovernanceHealthStatus
  measuredValue: number
  thresholdValue: number
  summary: string
  traceId?: string | null
  checkedAt: string
}

export interface GovernanceAlertRecord {
  alertId: string
  governanceId: string
  ruleId: string
  targetType: GovernanceScopeType
  targetCode: string
  alertLevel: AlertLevel
  alertType: string
  status: AlertStatus
  alertKey: string
  summary: string
  detail?: string | null
  traceId?: string | null
  occurredAt: string
  acknowledgedAt?: string | null
  acknowledgedBy?: string | null
  escalatedAt?: string | null
  escalatedBy?: string | null
  closedAt?: string | null
  closedBy?: string | null
  closeReason?: string | null
}

export interface GovernanceTraceRecord {
  traceId: string
  governanceId: string
  targetType: GovernanceScopeType
  targetCode: string
  traceType: string
  status: TraceStatus
  sourceEventType?: string | null
  sourceExecutionId?: string | null
  correlationId?: string | null
  summary: string
  detail?: string | null
  openedAt: string
  updatedAt: string
  resolvedAt?: string | null
}

export interface GovernanceActionAuditRecord {
  auditId: string
  governanceId: string
  targetType: GovernanceScopeType
  targetCode: string
  actionType: GovernanceActionType
  actionResult: string
  operatorId: string
  operatorName?: string | null
  reason?: string | null
  requestId: string
  payloadJson?: string | null
  resultMessage?: string | null
  traceId?: string | null
  createdAt: string
  completedAt?: string | null
}

export interface ServiceVersionRecord {
  versionRecordId: string
  governanceId: string
  targetType: GovernanceScopeType
  targetCode: string
  version: string
  compatibilityNote?: string | null
  changeSummary?: string | null
  status: ServiceVersionStatus
  registeredAt: string
  publishedAt?: string | null
  deprecatedAt?: string | null
  operatorId?: string | null
  approvalNote?: string | null
  auditTraceId?: string | null
}
