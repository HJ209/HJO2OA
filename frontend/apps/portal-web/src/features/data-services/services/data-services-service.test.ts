import { beforeEach, describe, expect, it, vi } from 'vitest'
import apiClient from '@/services/api-client'
import { del, get, post, put } from '@/services/request'
import { dataServicesService } from '@/features/data-services/services/data-services-service'

vi.mock('@/services/api-client', () => ({
  default: {
    get: vi.fn(),
  },
}))

vi.mock('@/services/request', () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}))

const apiGetMock = vi.mocked(apiClient.get)
const getMock = vi.mocked(get)
const postMock = vi.mocked(post)
const putMock = vi.mocked(put)
const delMock = vi.mocked(del)

function readParams(callIndex = 0): URLSearchParams {
  const config = getMock.mock.calls[callIndex]?.[1]
  return config?.params as URLSearchParams
}

describe('dataServicesService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getMock.mockResolvedValue({
      items: [],
      pagination: { page: 1, size: 10, total: 0, totalPages: 0 },
    })
    postMock.mockResolvedValue({})
    putMock.mockResolvedValue({})
    delMock.mockResolvedValue(undefined)
    apiGetMock.mockResolvedValue({
      data: {
        data: new Blob(['id,name\n1,Report\n'], { type: 'text/csv' }),
        meta: { requestId: 'req-export', success: true },
      },
      headers: {
        'content-disposition': 'attachment; filename="task_report-report.csv"',
      },
    })
  })

  it('calls connector list and upsert endpoints with query params', async () => {
    await dataServicesService.listConnectors({
      connectorType: 'HTTP',
      status: 'ACTIVE',
      page: 2,
      size: 10,
    })

    expect(getMock).toHaveBeenCalledWith('/v1/data/connectors', {
      params: expect.any(URLSearchParams),
    })
    expect(readParams().get('connectorType')).toBe('HTTP')
    expect(readParams().get('status')).toBe('ACTIVE')
    expect(readParams().get('page')).toBe('2')

    await dataServicesService.upsertConnector('connector-1', {
      code: 'http_main',
      name: 'HTTP Main',
      connectorType: 'HTTP',
      authMode: 'NONE',
    })

    expect(putMock).toHaveBeenCalledWith(
      '/v1/data/connectors/connector-1',
      expect.objectContaining({ code: 'http_main' }),
      expect.objectContaining({
        dedupeKey: 'data-connector:upsert:connector-1',
      }),
    )
  })

  it('uses DataService definition and runtime preview paths', async () => {
    await dataServicesService.listDataServices({ status: 'ACTIVE' })
    expect(getMock).toHaveBeenCalledWith('/v1/data/services/definitions', {
      params: expect.any(URLSearchParams),
    })
    expect(readParams().get('status')).toBe('ACTIVE')

    await dataServicesService.previewDataServiceInvocation('task_query', {
      parameters: { keyword: '审批' },
    })

    expect(postMock).toHaveBeenCalledWith(
      '/v1/data/services/runtime/task_query/query',
      { parameters: { keyword: '审批' } },
      expect.objectContaining({
        dedupeKey: 'data-service:preview:task_query',
      }),
    )
  })

  it('calls DataSync trigger, execution retry and delete endpoints', async () => {
    await dataServicesService.triggerSyncTask('task-1', { source: 'test' })
    expect(postMock).toHaveBeenCalledWith(
      '/v1/data/sync/tasks/task-1/trigger',
      expect.objectContaining({
        operatorAccountId: expect.any(String),
        triggerContext: { source: 'test' },
      }),
      expect.objectContaining({
        dedupeKey: 'data-sync:trigger:task-1',
        idempotencyKey: expect.any(String),
      }),
    )

    await dataServicesService.retrySyncExecution('execution-1', {
      operatorAccountId: 'account-1',
      reason: 'retry',
    })
    expect(postMock).toHaveBeenCalledWith(
      '/v1/data/sync/executions/execution-1/retry',
      expect.objectContaining({ reason: 'retry' }),
      expect.objectContaining({
        dedupeKey: 'data-sync:retry:execution-1',
      }),
    )

    await dataServicesService.deleteSyncTask('task-1')
    expect(delMock).toHaveBeenCalledWith(
      '/v1/data/sync/tasks/task-1',
      expect.objectContaining({ dedupeKey: 'data-sync:delete:task-1' }),
    )
  })

  it('calls OpenApi publish, credential, policy and audit endpoints', async () => {
    await dataServicesService.publishOpenApiEndpoint('todo_api', 'v1')
    expect(postMock).toHaveBeenCalledWith(
      '/v1/data/open-api/endpoints/todo_api/versions/v1/publish',
      undefined,
      expect.objectContaining({ dedupeKey: 'open-api:publish:todo_api:v1' }),
    )

    await dataServicesService.upsertOpenApiCredential(
      'todo_api',
      'v1',
      'portal',
      { secretRef: 'secret/api/portal', scopes: ['todo:read'] },
    )
    expect(putMock).toHaveBeenCalledWith(
      '/v1/data/open-api/endpoints/todo_api/versions/v1/credentials/portal',
      expect.objectContaining({ secretRef: 'secret/api/portal' }),
      expect.objectContaining({
        dedupeKey: 'open-api:credential:todo_api:v1:portal',
      }),
    )

    await dataServicesService.disableOpenApiPolicy('todo_api', 'v1', 'minute')
    expect(postMock).toHaveBeenCalledWith(
      '/v1/data/open-api/endpoints/todo_api/versions/v1/policies/minute/disable',
      undefined,
      expect.objectContaining({
        params: expect.any(URLSearchParams),
        dedupeKey: 'open-api:policy-disable:todo_api:v1:minute',
      }),
    )

    await dataServicesService.reviewOpenApiAuditLog(
      'log-1',
      true,
      'abnormal',
      'checked',
    )
    expect(postMock).toHaveBeenCalledWith(
      '/v1/data/open-api/audit-logs/log-1/review',
      {
        abnormalFlag: true,
        reviewConclusion: 'abnormal',
        note: 'checked',
      },
      expect.objectContaining({ dedupeKey: 'open-api:audit-review:log-1' }),
    )
  })

  it('calls Report preview and Governance endpoints', async () => {
    await dataServicesService.reportSummary('task_report', {
      metricCode: 'total',
      filters: { orgId: 'org-1' },
    })
    expect(getMock).toHaveBeenCalledWith(
      '/v1/data/report/definitions/task_report/summary',
      { params: expect.any(URLSearchParams) },
    )
    expect(readParams().get('metricCode')).toBe('total')
    expect(readParams().get('orgId')).toBe('org-1')

    const exportFile = await dataServicesService.exportReportCsv('task_report')
    expect(apiGetMock).toHaveBeenCalledWith(
      '/v1/data/report/definitions/task_report/export',
      expect.objectContaining({
        params: expect.any(URLSearchParams),
        responseType: 'blob',
      }),
    )
    expect(exportFile.filename).toBe('task_report-report.csv')
    expect(exportFile.blob).toBeInstanceOf(Blob)

    await dataServicesService.listGovernanceAlerts({
      targetType: 'API',
      status: 'OPEN',
    })
    expect(getMock).toHaveBeenCalledWith('/v1/data/governance/alerts', {
      params: expect.any(URLSearchParams),
    })

    await dataServicesService.submitGovernanceIntervention({
      tenantId: '00000000-0000-4000-8000-000000000001',
      targetType: 'API',
      targetCode: 'todo_api',
      actionType: 'REQUEST_RETRY',
      reason: 'manual',
    })
    expect(postMock).toHaveBeenCalledWith(
      '/v1/data/governance/interventions',
      expect.objectContaining({
        actionType: 'REQUEST_RETRY',
        requestId: expect.any(String),
      }),
      expect.objectContaining({
        dedupeKey: 'governance:intervention:API:todo_api',
      }),
    )

    await dataServicesService.upsertGovernanceHealthRule('gov-api', {
      tenantId: '00000000-0000-4000-8000-000000000001',
      ruleCode: 'hc-api-failure-rate',
      ruleName: 'API failure rate',
      checkType: 'FAILURE_RATE',
      severity: 'ERROR',
      status: 'ENABLED',
      metricName: 'FAILURE_RATE',
      comparisonOperator: 'GREATER_THAN',
      thresholdValue: 0,
    })
    expect(postMock).toHaveBeenCalledWith(
      '/v1/data/governance/profiles/gov-api/health-rules',
      expect.objectContaining({ ruleCode: 'hc-api-failure-rate' }),
      expect.objectContaining({
        dedupeKey: 'governance:health-rule:gov-api:hc-api-failure-rate',
      }),
    )

    await dataServicesService.upsertGovernanceAlertRule('gov-api', {
      tenantId: '00000000-0000-4000-8000-000000000001',
      ruleCode: 'ar-api-failure-rate',
      ruleName: 'API failure alert',
      sourceRuleCode: 'hc-api-failure-rate',
      alertType: 'DATA_QUALITY',
      alertLevel: 'ERROR',
      status: 'ENABLED',
      comparisonOperator: 'GREATER_THAN',
      thresholdValue: 0,
    })
    expect(postMock).toHaveBeenCalledWith(
      '/v1/data/governance/profiles/gov-api/alert-rules',
      expect.objectContaining({ sourceRuleCode: 'hc-api-failure-rate' }),
      expect.objectContaining({
        dedupeKey: 'governance:alert-rule:gov-api:ar-api-failure-rate',
      }),
    )
  })
})
