import { get, post, put, del } from '@/services/request'
import apiClient from '@/services/api-client'
import { useAuthStore } from '@/stores/auth-store'
import type { ApiResponse, PageData } from '@/types/api'
import type {
  AlertLevel,
  AlertRuleStatus,
  AlertStatus,
  ApiCredentialGrant,
  ApiCredentialStatus,
  ApiInvocationAuditLog,
  ApiPolicyType,
  ApiRateLimitPolicy,
  ApiWindowUnit,
  CompensationAction,
  ComparisonOperator,
  ConnectorDetail,
  ConnectorHealthOverview,
  ConnectorHealthSnapshot,
  ConnectorHealthStatus,
  ConnectorListView,
  ConnectorParameter,
  ConnectorParameterTemplate,
  ConnectorStatus,
  ConnectorType,
  DataServiceDetail,
  DataServiceExecutionPlan,
  DataServiceFieldMapping,
  DataServiceInvocationRequest,
  DataServiceParameter,
  DataServiceStatus,
  DataServiceSummary,
  DataServiceType,
  ExecutionStatus,
  ExecutionTriggerType,
  GovernanceActionAuditRecord,
  GovernanceActionType,
  GovernanceAlertRecord,
  GovernanceAlertRule,
  GovernanceHealthRule,
  GovernanceHealthSnapshot,
  GovernanceProfile,
  GovernanceProfileStatus,
  GovernanceScopeType,
  GovernanceTraceRecord,
  HealthCheckRuleStatus,
  HealthCheckSeverity,
  HealthCheckType,
  OpenApiEndpointDetail,
  OpenApiEndpointSummary,
  OpenApiHttpMethod,
  OpenApiStatus,
  OperatorActionRequest,
  ReportDefinition,
  ReportRankingPreview,
  ReportSnapshot,
  ReportSourceScope,
  ReportStatus,
  ReportSummaryPreview,
  ReportTrendPreview,
  ReportType,
  ReportVisibilityMode,
  SaveDataServiceRequest,
  SaveSyncTaskRequest,
  ServiceVersionRecord,
  ServiceVersionStatus,
  SyncExecutionDetail,
  SyncExecutionSummary,
  SyncMode,
  SyncTaskDetail,
  SyncTaskStatus,
  SyncTaskSummary,
  TraceStatus,
  UpsertConnectorRequest,
  UpsertOpenApiEndpointRequest,
} from '@/features/data-services/types/data-services'

const CONNECTOR_URL = '/v1/data/connectors'
const DATA_SERVICE_URL = '/v1/data/services/definitions'
const DATA_SERVICE_RUNTIME_URL = '/v1/data/services/runtime'
const SYNC_URL = '/v1/data/sync'
const OPEN_API_URL = '/v1/data/open-api'
const REPORT_URL = '/v1/data/report'
const GOVERNANCE_URL = '/v1/data/governance'

export interface ConnectorListQuery {
  connectorType?: ConnectorType
  status?: ConnectorStatus
  code?: string
  keyword?: string
  page?: number
  size?: number
}

export interface DataServiceListQuery {
  code?: string
  keyword?: string
  serviceType?: DataServiceType
  status?: DataServiceStatus
  page?: number
  size?: number
}

export interface SyncTaskListQuery {
  tenantId?: string
  code?: string
  syncMode?: SyncMode
  status?: SyncTaskStatus
  sourceConnectorId?: string
  targetConnectorId?: string
  page?: number
  size?: number
}

export interface SyncExecutionListQuery {
  taskCode?: string
  executionStatus?: ExecutionStatus
  triggerType?: ExecutionTriggerType
  startedFrom?: string
  startedTo?: string
  page?: number
  size?: number
}

export interface OpenApiEndpointListQuery {
  path?: string
  httpMethod?: OpenApiHttpMethod
  version?: string
  status?: OpenApiStatus
  dataServiceCode?: string
  page?: number
  size?: number
}

export interface OpenApiCredentialListQuery {
  clientCode?: string
  status?: ApiCredentialStatus
  expiresBefore?: string
  page?: number
  size?: number
}

export interface OpenApiPolicyListQuery {
  endpointCode?: string
  clientCode?: string
  policyCode?: string
  page?: number
  size?: number
}

export interface OpenApiAuditListQuery {
  endpointCode?: string
  clientCode?: string
  responseStatus?: number
  occurredFrom?: string
  occurredTo?: string
  page?: number
  size?: number
}

export interface ReportListQuery {
  reportType?: ReportType
  sourceScope?: ReportSourceScope
  status?: ReportStatus
  visibilityMode?: ReportVisibilityMode
  page?: number
  size?: number
}

export interface ReportPreviewQuery {
  from?: string
  to?: string
  dimensionCode?: string
  metricCode?: string
  topN?: number
  filters?: Record<string, string>
}

export interface ReportExportFile {
  filename: string
  blob: Blob
}

export interface GovernanceListQuery {
  tenantId?: string
  targetType?: GovernanceScopeType
  targetCode?: string
  scopeType?: GovernanceScopeType
  ruleCode?: string
  alertLevel?: AlertLevel
  status?:
    | AlertStatus
    | TraceStatus
    | GovernanceProfileStatus
    | ServiceVersionStatus
  checkedFrom?: string
  checkedTo?: string
  occurredFrom?: string
  occurredTo?: string
  openedFrom?: string
  openedTo?: string
  createdFrom?: string
  createdTo?: string
  page?: number
  size?: number
}

export interface UpsertCredentialRequest {
  secretRef: string
  scopes?: string[]
  expiresAt?: string
}

export interface UpsertPolicyRequest {
  clientCode?: string
  policyType: ApiPolicyType
  windowValue: number
  windowUnit: ApiWindowUnit
  threshold: number
  description?: string
}

export interface GovernanceOperatorRequest {
  tenantId: string
  operatorId?: string
  operatorName?: string
  reason?: string
  requestId?: string
}

function appendParam(
  params: URLSearchParams,
  key: string,
  value: string | number | boolean | null | undefined,
): void {
  if (value === undefined || value === null || value === '') {
    return
  }

  params.set(key, String(value))
}

function buildParams<TQuery extends object>(
  query: TQuery = {} as TQuery,
): URLSearchParams {
  const params = new URLSearchParams()

  Object.entries(query).forEach(([key, value]) =>
    appendParam(params, key, value),
  )

  return params
}

function omitPayloadKeys<TPayload extends object, TKey extends keyof TPayload>(
  payload: TPayload,
  keys: TKey[],
): Omit<TPayload, TKey> {
  return Object.fromEntries(
    Object.entries(payload).filter(([key]) => !keys.includes(key as TKey)),
  ) as Omit<TPayload, TKey>
}

export function createClientId(prefix = 'client'): string {
  if (
    typeof crypto !== 'undefined' &&
    typeof crypto.randomUUID === 'function'
  ) {
    return crypto.randomUUID()
  }

  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export function getCurrentTenantId(): string {
  const authState = useAuthStore.getState()

  return (
    authState.user?.tenantId ??
    import.meta.env.VITE_TENANT_ID ??
    '00000000-0000-4000-8000-000000000001'
  )
}

export function getCurrentOperatorId(): string {
  const authState = useAuthStore.getState()

  return authState.user?.id ?? 'portal-web'
}

export function getCurrentOperatorName(): string {
  const authState = useAuthStore.getState()

  return (
    authState.user?.displayName ?? authState.user?.accountName ?? 'portal-web'
  )
}

export const dataServicesService = {
  listConnectors(query: ConnectorListQuery = {}): Promise<ConnectorListView> {
    return get(CONNECTOR_URL, { params: buildParams(query) })
  },
  getConnector(connectorId: string): Promise<ConnectorDetail> {
    return get(`${CONNECTOR_URL}/${connectorId}`)
  },
  listConnectorParameterTemplates(
    connectorId: string,
  ): Promise<ConnectorParameterTemplate[]> {
    return get(`${CONNECTOR_URL}/${connectorId}/parameter-templates`)
  },
  latestConnectorTest(connectorId: string): Promise<ConnectorHealthSnapshot> {
    return get(`${CONNECTOR_URL}/${connectorId}/tests/latest`)
  },
  connectorTestHistory(
    connectorId: string,
    query: {
      status?: ConnectorHealthStatus
      checkedFrom?: string
      checkedTo?: string
      limit?: number
    } = {},
  ): Promise<ConnectorHealthSnapshot[]> {
    return get(`${CONNECTOR_URL}/${connectorId}/tests/history`, {
      params: buildParams(query),
    })
  },
  connectorHealth(connectorId: string): Promise<ConnectorHealthOverview> {
    return get(`${CONNECTOR_URL}/${connectorId}/health`)
  },
  connectorHealthHistory(
    connectorId: string,
    query: {
      status?: ConnectorHealthStatus
      checkedFrom?: string
      checkedTo?: string
      limit?: number
    } = {},
  ): Promise<ConnectorHealthSnapshot[]> {
    return get(`${CONNECTOR_URL}/${connectorId}/health/history`, {
      params: buildParams(query),
    })
  },
  upsertConnector(
    connectorId: string,
    payload: UpsertConnectorRequest,
  ): Promise<ConnectorDetail> {
    return put(`${CONNECTOR_URL}/${connectorId}`, payload, {
      dedupeKey: `data-connector:upsert:${connectorId}`,
    })
  },
  saveConnectorParameters(
    connectorId: string,
    parameters: ConnectorParameter[],
  ): Promise<ConnectorDetail> {
    return put(
      `${CONNECTOR_URL}/${connectorId}/parameters`,
      { parameters },
      { dedupeKey: `data-connector:parameters:${connectorId}` },
    )
  },
  activateConnector(connectorId: string): Promise<ConnectorDetail> {
    return post(`${CONNECTOR_URL}/${connectorId}/activate`, undefined, {
      dedupeKey: `data-connector:activate:${connectorId}`,
    })
  },
  disableConnector(connectorId: string): Promise<ConnectorDetail> {
    return post(`${CONNECTOR_URL}/${connectorId}/disable`, undefined, {
      dedupeKey: `data-connector:disable:${connectorId}`,
    })
  },
  testConnector(connectorId: string): Promise<ConnectorHealthSnapshot> {
    return post(`${CONNECTOR_URL}/${connectorId}/test`, undefined, {
      dedupeKey: `data-connector:test:${connectorId}`,
    })
  },
  refreshConnectorHealth(
    connectorId: string,
  ): Promise<ConnectorHealthSnapshot> {
    return post(`${CONNECTOR_URL}/${connectorId}/health/refresh`, undefined, {
      dedupeKey: `data-connector:health:${connectorId}`,
    })
  },
  confirmConnectorHealth(
    connectorId: string,
    snapshotId: string,
    note: string,
  ): Promise<ConnectorHealthSnapshot> {
    return post(
      `${CONNECTOR_URL}/${connectorId}/health/${snapshotId}/confirm`,
      { note },
      { dedupeKey: `data-connector:health-confirm:${snapshotId}` },
    )
  },

  listDataServices(
    query: DataServiceListQuery = {},
  ): Promise<PageData<DataServiceSummary>> {
    return get(DATA_SERVICE_URL, { params: buildParams(query) })
  },
  getDataService(serviceId: string): Promise<DataServiceDetail> {
    return get(`${DATA_SERVICE_URL}/${serviceId}`)
  },
  createDataService(
    payload: SaveDataServiceRequest,
  ): Promise<DataServiceDetail> {
    return post(
      DATA_SERVICE_URL,
      { ...payload, serviceId: payload.serviceId ?? createClientId('svc') },
      { dedupeKey: `data-service:create:${payload.code}` },
    )
  },
  updateDataService(
    serviceId: string,
    payload: SaveDataServiceRequest,
  ): Promise<DataServiceDetail> {
    const body = omitPayloadKeys(payload, ['serviceId'])

    return put(`${DATA_SERVICE_URL}/${serviceId}`, body, {
      dedupeKey: `data-service:update:${serviceId}`,
    })
  },
  activateDataService(serviceId: string): Promise<DataServiceDetail> {
    return post(`${DATA_SERVICE_URL}/${serviceId}/activate`, undefined, {
      dedupeKey: `data-service:activate:${serviceId}`,
    })
  },
  disableDataService(serviceId: string): Promise<DataServiceDetail> {
    return post(`${DATA_SERVICE_URL}/${serviceId}/disable`, undefined, {
      dedupeKey: `data-service:disable:${serviceId}`,
    })
  },
  deleteDataService(serviceId: string): Promise<void> {
    return del<void>(`${DATA_SERVICE_URL}/${serviceId}`, {
      dedupeKey: `data-service:delete:${serviceId}`,
    })
  },
  listDataServiceParameters(
    serviceId: string,
  ): Promise<DataServiceParameter[]> {
    return get(`${DATA_SERVICE_URL}/${serviceId}/parameters`)
  },
  upsertDataServiceParameter(
    serviceId: string,
    paramCode: string,
    payload: DataServiceParameter,
  ): Promise<DataServiceParameter> {
    return put(
      `${DATA_SERVICE_URL}/${serviceId}/parameters/${paramCode}`,
      payload,
      {
        dedupeKey: `data-service:param:${serviceId}:${paramCode}`,
      },
    )
  },
  listDataServiceFieldMappings(
    serviceId: string,
  ): Promise<DataServiceFieldMapping[]> {
    return get(`${DATA_SERVICE_URL}/${serviceId}/field-mappings`)
  },
  upsertDataServiceFieldMapping(
    serviceId: string,
    mappingId: string,
    payload: DataServiceFieldMapping,
  ): Promise<DataServiceFieldMapping> {
    return put(
      `${DATA_SERVICE_URL}/${serviceId}/field-mappings/${mappingId}`,
      payload,
      { dedupeKey: `data-service:mapping:${serviceId}:${mappingId}` },
    )
  },
  previewDataServiceInvocation(
    serviceCode: string,
    payload: DataServiceInvocationRequest,
  ): Promise<DataServiceExecutionPlan> {
    return post(`${DATA_SERVICE_RUNTIME_URL}/${serviceCode}/query`, payload, {
      dedupeKey: `data-service:preview:${serviceCode}`,
    })
  },

  listSyncTasks(
    query: SyncTaskListQuery = {},
  ): Promise<PageData<SyncTaskSummary>> {
    return get(`${SYNC_URL}/tasks`, { params: buildParams(query) })
  },
  getSyncTask(taskId: string): Promise<SyncTaskDetail> {
    return get(`${SYNC_URL}/tasks/${taskId}`)
  },
  createSyncTask(payload: SaveSyncTaskRequest): Promise<SyncTaskDetail> {
    return post(`${SYNC_URL}/tasks`, payload, {
      dedupeKey: `data-sync:create:${payload.code ?? payload.name}`,
    })
  },
  updateSyncTask(
    taskId: string,
    payload: SaveSyncTaskRequest,
  ): Promise<SyncTaskDetail> {
    const body = omitPayloadKeys(payload, ['tenantId', 'code'])

    return put(`${SYNC_URL}/tasks/${taskId}`, body, {
      dedupeKey: `data-sync:update:${taskId}`,
    })
  },
  deleteSyncTask(taskId: string): Promise<void> {
    return del<void>(`${SYNC_URL}/tasks/${taskId}`, {
      dedupeKey: `data-sync:delete:${taskId}`,
    })
  },
  activateSyncTask(taskId: string): Promise<SyncTaskDetail> {
    return post(`${SYNC_URL}/tasks/${taskId}/activate`, undefined, {
      dedupeKey: `data-sync:activate:${taskId}`,
    })
  },
  pauseSyncTask(taskId: string): Promise<SyncTaskDetail> {
    return post(`${SYNC_URL}/tasks/${taskId}/pause`, undefined, {
      dedupeKey: `data-sync:pause:${taskId}`,
    })
  },
  resetSyncCheckpoint(
    taskId: string,
    checkpointValue: string,
    reason: string,
  ): Promise<SyncTaskDetail> {
    return post(
      `${SYNC_URL}/tasks/${taskId}/checkpoint/reset`,
      {
        checkpointValue,
        operatorAccountId: getCurrentOperatorId(),
        reason,
      },
      { dedupeKey: `data-sync:checkpoint:${taskId}` },
    )
  },
  triggerSyncTask(
    taskId: string,
    triggerContext: Record<string, unknown> = {},
  ): Promise<SyncExecutionDetail> {
    const idempotencyKey = createClientId('sync-trigger')

    return post(
      `${SYNC_URL}/tasks/${taskId}/trigger`,
      {
        idempotencyKey,
        operatorAccountId: getCurrentOperatorId(),
        operatorPersonId: getCurrentOperatorId(),
        triggerContext,
      },
      {
        dedupeKey: `data-sync:trigger:${taskId}`,
        idempotencyKey,
      },
    )
  },
  listTaskExecutions(
    taskId: string,
    query: SyncExecutionListQuery = {},
  ): Promise<PageData<SyncExecutionSummary>> {
    return get(`${SYNC_URL}/tasks/${taskId}/executions`, {
      params: buildParams(query),
    })
  },
  listSyncExecutions(
    query: SyncExecutionListQuery = {},
  ): Promise<PageData<SyncExecutionSummary>> {
    return get(`${SYNC_URL}/executions`, { params: buildParams(query) })
  },
  getSyncExecution(executionId: string): Promise<SyncExecutionDetail> {
    return get(`${SYNC_URL}/executions/${executionId}`)
  },
  retrySyncExecution(
    executionId: string,
    request: OperatorActionRequest,
  ): Promise<SyncExecutionDetail> {
    const idempotencyKey =
      request.idempotencyKey ?? createClientId('sync-retry')

    return post(
      `${SYNC_URL}/executions/${executionId}/retry`,
      { ...request, idempotencyKey },
      {
        dedupeKey: `data-sync:retry:${executionId}`,
        idempotencyKey,
      },
    )
  },
  reconcileSyncExecution(
    executionId: string,
    request: OperatorActionRequest,
  ): Promise<SyncExecutionDetail> {
    const idempotencyKey =
      request.idempotencyKey ?? createClientId('sync-reconcile')

    return post(
      `${SYNC_URL}/executions/${executionId}/reconcile`,
      { ...request, idempotencyKey },
      {
        dedupeKey: `data-sync:reconcile:${executionId}`,
        idempotencyKey,
      },
    )
  },
  compensateSyncExecution(
    executionId: string,
    differenceCode: string,
    action: CompensationAction,
    reason: string,
  ): Promise<SyncExecutionDetail> {
    const idempotencyKey = createClientId('sync-compensate')

    return post(
      `${SYNC_URL}/executions/${executionId}/compensate`,
      {
        idempotencyKey,
        operatorAccountId: getCurrentOperatorId(),
        operatorPersonId: getCurrentOperatorId(),
        reason,
        decisions: [{ differenceCode, action, reason }],
      },
      {
        dedupeKey: `data-sync:compensate:${executionId}:${differenceCode}`,
        idempotencyKey,
      },
    )
  },

  listOpenApiEndpoints(
    query: OpenApiEndpointListQuery = {},
  ): Promise<PageData<OpenApiEndpointSummary>> {
    return get(`${OPEN_API_URL}/endpoints`, { params: buildParams(query) })
  },
  getOpenApiEndpoint(
    code: string,
    version: string,
  ): Promise<OpenApiEndpointDetail> {
    return get(`${OPEN_API_URL}/endpoints/${code}/versions/${version}`)
  },
  upsertOpenApiEndpoint(
    code: string,
    version: string,
    payload: UpsertOpenApiEndpointRequest,
  ): Promise<OpenApiEndpointDetail> {
    return put(
      `${OPEN_API_URL}/endpoints/${code}/versions/${version}`,
      payload,
      {
        dedupeKey: `open-api:endpoint:${code}:${version}`,
      },
    )
  },
  publishOpenApiEndpoint(
    code: string,
    version: string,
  ): Promise<OpenApiEndpointDetail> {
    return post(
      `${OPEN_API_URL}/endpoints/${code}/versions/${version}/publish`,
      undefined,
      { dedupeKey: `open-api:publish:${code}:${version}` },
    )
  },
  deprecateOpenApiEndpoint(
    code: string,
    version: string,
    sunsetAt?: string,
  ): Promise<OpenApiEndpointDetail> {
    return post(
      `${OPEN_API_URL}/endpoints/${code}/versions/${version}/deprecate`,
      { sunsetAt },
      { dedupeKey: `open-api:deprecate:${code}:${version}` },
    )
  },
  offlineOpenApiEndpoint(
    code: string,
    version: string,
  ): Promise<OpenApiEndpointDetail> {
    return post(
      `${OPEN_API_URL}/endpoints/${code}/versions/${version}/offline`,
      undefined,
      { dedupeKey: `open-api:offline:${code}:${version}` },
    )
  },
  deleteOpenApiEndpoint(code: string, version: string): Promise<void> {
    return del<void>(`${OPEN_API_URL}/endpoints/${code}/versions/${version}`, {
      dedupeKey: `open-api:delete:${code}:${version}`,
    })
  },
  listOpenApiCredentials(
    query: OpenApiCredentialListQuery = {},
  ): Promise<PageData<ApiCredentialGrant>> {
    return get(`${OPEN_API_URL}/credentials`, { params: buildParams(query) })
  },
  upsertOpenApiCredential(
    code: string,
    version: string,
    clientCode: string,
    payload: UpsertCredentialRequest,
  ): Promise<OpenApiEndpointDetail> {
    return put(
      `${OPEN_API_URL}/endpoints/${code}/versions/${version}/credentials/${clientCode}`,
      payload,
      { dedupeKey: `open-api:credential:${code}:${version}:${clientCode}` },
    )
  },
  revokeOpenApiCredential(
    code: string,
    version: string,
    clientCode: string,
  ): Promise<OpenApiEndpointDetail> {
    return post(
      `${OPEN_API_URL}/endpoints/${code}/versions/${version}/credentials/${clientCode}/revoke`,
      undefined,
      { dedupeKey: `open-api:revoke:${code}:${version}:${clientCode}` },
    )
  },
  listOpenApiPolicies(
    query: OpenApiPolicyListQuery = {},
  ): Promise<PageData<ApiRateLimitPolicy>> {
    return get(`${OPEN_API_URL}/policies`, { params: buildParams(query) })
  },
  upsertOpenApiPolicy(
    code: string,
    version: string,
    policyCode: string,
    payload: UpsertPolicyRequest,
  ): Promise<OpenApiEndpointDetail> {
    return put(
      `${OPEN_API_URL}/endpoints/${code}/versions/${version}/policies/${policyCode}`,
      payload,
      { dedupeKey: `open-api:policy:${code}:${version}:${policyCode}` },
    )
  },
  disableOpenApiPolicy(
    code: string,
    version: string,
    policyCode: string,
    clientCode?: string,
  ): Promise<OpenApiEndpointDetail> {
    return post(
      `${OPEN_API_URL}/endpoints/${code}/versions/${version}/policies/${policyCode}/disable`,
      undefined,
      {
        params: buildParams({ clientCode }),
        dedupeKey: `open-api:policy-disable:${code}:${version}:${policyCode}`,
      },
    )
  },
  listOpenApiAuditLogs(
    query: OpenApiAuditListQuery = {},
  ): Promise<PageData<ApiInvocationAuditLog>> {
    return get(`${OPEN_API_URL}/audit-logs`, { params: buildParams(query) })
  },
  reviewOpenApiAuditLog(
    logId: string,
    abnormalFlag: boolean,
    reviewConclusion: string,
    note: string,
  ): Promise<ApiInvocationAuditLog> {
    return post(
      `${OPEN_API_URL}/audit-logs/${logId}/review`,
      { abnormalFlag, reviewConclusion, note },
      { dedupeKey: `open-api:audit-review:${logId}` },
    )
  },

  listReports(
    query: ReportListQuery = {},
  ): Promise<PageData<ReportDefinition>> {
    return get(`${REPORT_URL}/definitions`, { params: buildParams(query) })
  },
  getReport(code: string): Promise<ReportDefinition> {
    return get(`${REPORT_URL}/definitions/${code}`)
  },
  createReport(payload: ReportDefinition): Promise<ReportDefinition> {
    return post(`${REPORT_URL}/definitions`, payload, {
      dedupeKey: `report:create:${payload.code}`,
    })
  },
  updateReport(
    code: string,
    payload: ReportDefinition,
  ): Promise<ReportDefinition> {
    return put(`${REPORT_URL}/definitions/${code}`, payload, {
      dedupeKey: `report:update:${code}`,
    })
  },
  changeReportStatus(
    code: string,
    status: ReportStatus,
  ): Promise<ReportDefinition> {
    return post(
      `${REPORT_URL}/definitions/${code}/status`,
      { status },
      { dedupeKey: `report:status:${code}:${status}` },
    )
  },
  refreshReport(code: string, reason: string): Promise<ReportSnapshot> {
    return post(
      `${REPORT_URL}/definitions/${code}/refresh`,
      { reason, batchId: createClientId('report-refresh') },
      { dedupeKey: `report:refresh:${code}` },
    )
  },
  reportSummary(
    code: string,
    query: ReportPreviewQuery = {},
  ): Promise<ReportSummaryPreview> {
    return get(`${REPORT_URL}/definitions/${code}/summary`, {
      params: buildReportPreviewParams(query),
    })
  },
  reportTrend(
    code: string,
    query: ReportPreviewQuery = {},
  ): Promise<ReportTrendPreview> {
    return get(`${REPORT_URL}/definitions/${code}/trend`, {
      params: buildReportPreviewParams(query),
    })
  },
  reportRanking(
    code: string,
    query: ReportPreviewQuery = {},
  ): Promise<ReportRankingPreview> {
    return get(`${REPORT_URL}/definitions/${code}/ranking`, {
      params: buildReportPreviewParams(query),
    })
  },
  reportCard(code: string, query: ReportPreviewQuery = {}): Promise<unknown> {
    return get(`${REPORT_URL}/cards/${code}`, {
      params: buildReportPreviewParams(query),
    })
  },
  async exportReportCsv(
    code: string,
    query: ReportPreviewQuery = {},
  ): Promise<ReportExportFile> {
    const response = await apiClient.get<ApiResponse<Blob>>(
      `${REPORT_URL}/definitions/${code}/export`,
      {
        params: buildReportPreviewParams(query),
        responseType: 'blob',
      },
    )
    const blob = response.data.data

    return {
      filename: resolveFilename(response.headers['content-disposition'], code),
      blob,
    }
  },
  reportSnapshots(
    code: string,
    page = 1,
    size = 20,
  ): Promise<PageData<ReportSnapshot>> {
    return get(`${REPORT_URL}/definitions/${code}/snapshots`, {
      params: buildParams({ page, size }),
    })
  },

  listGovernanceProfiles(
    query: GovernanceListQuery = {},
  ): Promise<PageData<GovernanceProfile>> {
    return get(`${GOVERNANCE_URL}/profiles`, {
      params: buildParams({
        ...query,
        tenantId: query.tenantId ?? getCurrentTenantId(),
      }),
    })
  },
  upsertGovernanceProfile(payload: {
    tenantId: string
    code: string
    scopeType: GovernanceScopeType
    targetCode: string
    slaPolicyJson?: string
    alertPolicyJson?: string
    status?: GovernanceProfileStatus
  }): Promise<GovernanceProfile> {
    return post(`${GOVERNANCE_URL}/profiles`, withGovernanceOperator(payload), {
      dedupeKey: `governance:profile:${payload.code}`,
    })
  },
  listGovernanceHealthRules(
    profileCode: string,
    query: { tenantId?: string; status?: HealthCheckRuleStatus } = {},
  ): Promise<PageData<GovernanceHealthRule>> {
    return get(
      `${GOVERNANCE_URL}/profiles/${encodeURIComponent(profileCode)}/health-rules`,
      {
        params: buildParams({
          ...query,
          tenantId: query.tenantId ?? getCurrentTenantId(),
        }),
      },
    )
  },
  upsertGovernanceHealthRule(
    profileCode: string,
    payload: {
      tenantId: string
      ruleCode: string
      ruleName: string
      checkType: HealthCheckType
      severity: HealthCheckSeverity
      status: HealthCheckRuleStatus
      metricName: string
      comparisonOperator: ComparisonOperator
      thresholdValue: number
      windowMinutes?: number
      dedupMinutes?: number
      scheduleExpression?: string
      strategyJson?: string
    },
  ): Promise<GovernanceHealthRule> {
    return post(
      `${GOVERNANCE_URL}/profiles/${encodeURIComponent(profileCode)}/health-rules`,
      withGovernanceOperator(payload),
      {
        dedupeKey: `governance:health-rule:${profileCode}:${payload.ruleCode}`,
      },
    )
  },
  listGovernanceAlertRules(
    profileCode: string,
    query: { tenantId?: string; status?: AlertRuleStatus } = {},
  ): Promise<PageData<GovernanceAlertRule>> {
    return get(
      `${GOVERNANCE_URL}/profiles/${encodeURIComponent(profileCode)}/alert-rules`,
      {
        params: buildParams({
          ...query,
          tenantId: query.tenantId ?? getCurrentTenantId(),
        }),
      },
    )
  },
  upsertGovernanceAlertRule(
    profileCode: string,
    payload: {
      tenantId: string
      ruleCode: string
      ruleName: string
      sourceRuleCode?: string
      metricName?: string
      alertType: string
      alertLevel: AlertLevel
      status: AlertRuleStatus
      comparisonOperator: ComparisonOperator
      thresholdValue: number
      dedupMinutes?: number
      escalationMinutes?: number
      notificationPolicyJson?: string
      strategyJson?: string
    },
  ): Promise<GovernanceAlertRule> {
    return post(
      `${GOVERNANCE_URL}/profiles/${encodeURIComponent(profileCode)}/alert-rules`,
      withGovernanceOperator(payload),
      {
        dedupeKey: `governance:alert-rule:${profileCode}:${payload.ruleCode}`,
      },
    )
  },
  runGovernanceHealthChecks(
    targetType?: GovernanceScopeType,
    targetCode?: string,
  ): Promise<PageData<GovernanceHealthSnapshot>> {
    return post(
      `${GOVERNANCE_URL}/health-checks/run`,
      withGovernanceOperator({
        tenantId: getCurrentTenantId(),
        targetType,
        targetCode,
      }),
      {
        dedupeKey: `governance:health-run:${targetType ?? 'all'}:${targetCode ?? 'all'}`,
      },
    )
  },
  listGovernanceHealthSnapshots(
    query: GovernanceListQuery = {},
  ): Promise<PageData<GovernanceHealthSnapshot>> {
    return get(`${GOVERNANCE_URL}/health-snapshots`, {
      params: buildParams(query),
    })
  },
  listGovernanceAlerts(
    query: GovernanceListQuery = {},
  ): Promise<PageData<GovernanceAlertRecord>> {
    return get(`${GOVERNANCE_URL}/alerts`, { params: buildParams(query) })
  },
  handleGovernanceAlert(
    alertId: string,
    action: 'acknowledge' | 'escalate' | 'close',
    request: GovernanceOperatorRequest,
  ): Promise<GovernanceAlertRecord> {
    return post(`${GOVERNANCE_URL}/alerts/${alertId}/${action}`, request, {
      dedupeKey: `governance:alert:${action}:${alertId}`,
    })
  },
  listGovernanceTraces(
    query: GovernanceListQuery = {},
  ): Promise<PageData<GovernanceTraceRecord>> {
    return get(`${GOVERNANCE_URL}/traces`, { params: buildParams(query) })
  },
  listGovernanceAudits(
    query: GovernanceListQuery = {},
  ): Promise<PageData<GovernanceActionAuditRecord>> {
    return get(`${GOVERNANCE_URL}/audits`, { params: buildParams(query) })
  },
  submitGovernanceIntervention(payload: {
    tenantId: string
    targetType: GovernanceScopeType
    targetCode: string
    traceId?: string
    actionType: GovernanceActionType
    reason?: string
    payloadJson?: string
  }): Promise<GovernanceActionAuditRecord> {
    return post(
      `${GOVERNANCE_URL}/interventions`,
      withGovernanceOperator(payload),
      {
        dedupeKey: `governance:intervention:${payload.targetType}:${payload.targetCode}`,
      },
    )
  },
  listServiceVersions(
    query: GovernanceListQuery = {},
  ): Promise<PageData<ServiceVersionRecord>> {
    return get(`${GOVERNANCE_URL}/versions`, {
      params: buildParams({
        ...query,
        tenantId: query.tenantId ?? getCurrentTenantId(),
      }),
    })
  },
  registerServiceVersion(payload: {
    tenantId: string
    profileCode: string
    targetType?: GovernanceScopeType
    targetCode?: string
    version: string
    compatibilityNote?: string
    changeSummary?: string
    approvalNote?: string
  }): Promise<ServiceVersionRecord> {
    return post(
      `${GOVERNANCE_URL}/versions/register`,
      withGovernanceOperator(payload),
      {
        dedupeKey: `governance:version-register:${payload.profileCode}:${payload.version}`,
      },
    )
  },
  publishServiceVersion(payload: {
    tenantId: string
    profileCode: string
    version: string
    approvalNote?: string
  }): Promise<ServiceVersionRecord> {
    return post(
      `${GOVERNANCE_URL}/versions/publish`,
      withGovernanceOperator(payload),
      {
        dedupeKey: `governance:version-publish:${payload.profileCode}:${payload.version}`,
      },
    )
  },
  deprecateServiceVersion(payload: {
    tenantId: string
    profileCode: string
    version: string
    approvalNote?: string
  }): Promise<ServiceVersionRecord> {
    return post(
      `${GOVERNANCE_URL}/versions/deprecate`,
      withGovernanceOperator(payload),
      {
        dedupeKey: `governance:version-deprecate:${payload.profileCode}:${payload.version}`,
      },
    )
  },
}

function buildReportPreviewParams(query: ReportPreviewQuery): URLSearchParams {
  const params = buildParams({
    from: query.from,
    to: query.to,
    dimensionCode: query.dimensionCode,
    metricCode: query.metricCode,
    topN: query.topN,
  })

  Object.entries(query.filters ?? {}).forEach(([key, value]) => {
    appendParam(params, key, value)
  })

  return params
}

function resolveFilename(disposition: unknown, code: string): string {
  if (typeof disposition === 'string') {
    const match = /filename="?([^";]+)"?/i.exec(disposition)
    if (match?.[1]) {
      return match[1]
    }
  }
  return `${code}-report.csv`
}

function withGovernanceOperator<TPayload extends Record<string, unknown>>(
  payload: TPayload,
): TPayload & {
  operatorId: string
  operatorName: string
  requestId: string
} {
  return {
    ...payload,
    operatorId:
      typeof payload.operatorId === 'string'
        ? payload.operatorId
        : getCurrentOperatorId(),
    operatorName:
      typeof payload.operatorName === 'string'
        ? payload.operatorName
        : getCurrentOperatorName(),
    requestId:
      typeof payload.requestId === 'string'
        ? payload.requestId
        : createClientId('governance-request'),
  }
}
