import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactElement, ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DataServicesPage from '@/features/data-services/pages/data-services-page'
import type {
  ConnectorDetail,
  ConnectorListView,
  ConnectorSummary,
  DataServiceDetail,
  DataServiceSummary,
} from '@/features/data-services/types/data-services'

const serviceMocks = vi.hoisted(() => ({
  createClientId: vi.fn(() => '00000000-0000-4000-8000-000000000013'),
  getCurrentTenantId: vi.fn(() => '00000000-0000-4000-8000-000000000001'),
  getCurrentOperatorId: vi.fn(() => 'operator-1'),
  dataServicesService: {
    listConnectors: vi.fn(),
    getConnector: vi.fn(),
    listConnectorParameterTemplates: vi.fn(),
    connectorHealth: vi.fn(),
    connectorTestHistory: vi.fn(),
    upsertConnector: vi.fn(),
    saveConnectorParameters: vi.fn(),
    activateConnector: vi.fn(),
    disableConnector: vi.fn(),
    testConnector: vi.fn(),
    refreshConnectorHealth: vi.fn(),
    confirmConnectorHealth: vi.fn(),
    listDataServices: vi.fn(),
    getDataService: vi.fn(),
    createDataService: vi.fn(),
    updateDataService: vi.fn(),
    deleteDataService: vi.fn(),
    activateDataService: vi.fn(),
    disableDataService: vi.fn(),
    listDataServiceParameters: vi.fn(),
    listDataServiceFieldMappings: vi.fn(),
    upsertDataServiceParameter: vi.fn(),
    upsertDataServiceFieldMapping: vi.fn(),
    previewDataServiceInvocation: vi.fn(),
    listSyncTasks: vi.fn(),
    listGovernanceProfiles: vi.fn(),
    listGovernanceHealthSnapshots: vi.fn(),
    listGovernanceAlerts: vi.fn(),
    listGovernanceTraces: vi.fn(),
    listGovernanceAudits: vi.fn(),
    listServiceVersions: vi.fn(),
    listGovernanceHealthRules: vi.fn(),
    listGovernanceAlertRules: vi.fn(),
    handleGovernanceAlert: vi.fn(),
  },
}))

vi.mock('@/features/data-services/services/data-services-service', () => ({
  createClientId: serviceMocks.createClientId,
  getCurrentTenantId: serviceMocks.getCurrentTenantId,
  getCurrentOperatorId: serviceMocks.getCurrentOperatorId,
  dataServicesService: serviceMocks.dataServicesService,
}))

function emptyConnectorPage(): ConnectorListView {
  return {
    items: [],
    pagination: {
      page: 1,
      size: 10,
      total: 0,
      totalPages: 0,
    },
  }
}

function connectorSummary(): ConnectorSummary {
  return {
    connectorId: 'connector-1',
    tenantId: '00000000-0000-4000-8000-000000000001',
    code: 'http_main',
    name: 'HTTP Main',
    connectorType: 'HTTP',
    vendor: 'internal',
    protocol: 'https',
    authMode: 'NONE',
    timeoutConfig: {
      connectTimeoutMs: 5000,
      readTimeoutMs: 15000,
      retryCount: 2,
      retryIntervalMs: 1000,
    },
    status: 'ACTIVE',
    changeSequence: 1,
    latestHealthSnapshot: {
      snapshotId: 'snapshot-1',
      connectorId: 'connector-1',
      checkType: 'HEALTH_CHECK',
      healthStatus: 'HEALTHY',
      latencyMs: 20,
      targetEnvironment: 'default',
      changeSequence: 1,
      checkedAt: '2026-04-29T08:00:00Z',
    },
    createdAt: '2026-04-29T08:00:00Z',
    updatedAt: '2026-04-29T08:00:00Z',
  }
}

function connectorDetail(): ConnectorDetail {
  return {
    ...connectorSummary(),
    parameters: [
      {
        paramKey: 'baseUrl',
        paramValueRef: 'secret/data/base-url',
        sensitive: false,
      },
    ],
  }
}

function dataServiceSummary(): DataServiceSummary {
  return {
    serviceId: 'service-existing',
    tenantId: '00000000-0000-4000-8000-000000000001',
    code: 'existing_service',
    name: 'Existing Service',
    serviceType: 'QUERY',
    sourceMode: 'INTERNAL_QUERY',
    permissionMode: 'PUBLIC_INTERNAL',
    status: 'DRAFT',
    cacheEnabled: false,
    openApiReferenceCount: 0,
    reportReferenceCount: 0,
    syncReferenceCount: 0,
    openApiReusable: true,
    reportReusable: true,
    createdAt: '2026-04-29T08:00:00Z',
    updatedAt: '2026-04-29T08:00:00Z',
  }
}

function dataServiceDetail(
  overrides: Partial<DataServiceDetail> = {},
): DataServiceDetail {
  return {
    ...dataServiceSummary(),
    permissionBoundary: {
      allowedAppCodes: [],
      allowedSubjectIds: [],
      requiredRoles: [],
    },
    cachePolicy: {
      enabled: false,
      ttlSeconds: 300,
      scope: 'TENANT',
      cacheNullValue: false,
      invalidationEvents: [],
    },
    sourceRef: 'internal.query',
    connectorId: null,
    description: null,
    statusSequence: 1,
    parameters: [],
    fieldMappings: [],
    ...overrides,
  }
}

function renderWithQueryClient(children: ReactNode): ReactElement {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
}

function renderPage(): void {
  render(renderWithQueryClient(<DataServicesPage />))
}

describe('DataServicesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    serviceMocks.createClientId.mockReturnValue(
      '00000000-0000-4000-8000-000000000013',
    )
    serviceMocks.dataServicesService.listConnectors.mockResolvedValue(
      emptyConnectorPage(),
    )
    serviceMocks.dataServicesService.getConnector.mockResolvedValue(
      connectorDetail(),
    )
    serviceMocks.dataServicesService.listConnectorParameterTemplates.mockResolvedValue(
      [],
    )
    serviceMocks.dataServicesService.connectorHealth.mockResolvedValue({
      connectorId: 'connector-1',
      latestHealthSnapshot: connectorSummary().latestHealthSnapshot,
      lastFailureSnapshot: null,
      sampleSize: 1,
      healthyCount: 1,
      degradedCount: 0,
      unreachableCount: 0,
    })
    serviceMocks.dataServicesService.connectorTestHistory.mockResolvedValue([])
    serviceMocks.dataServicesService.upsertConnector.mockResolvedValue(
      connectorDetail(),
    )
    serviceMocks.dataServicesService.listDataServices.mockResolvedValue({
      items: [dataServiceSummary()],
      pagination: {
        page: 1,
        size: 10,
        total: 1,
        totalPages: 1,
      },
    })
    serviceMocks.dataServicesService.getDataService.mockResolvedValue(
      dataServiceDetail(),
    )
    serviceMocks.dataServicesService.createDataService.mockResolvedValue(
      dataServiceDetail({
        serviceId: 'service-new',
        code: 'browser_service',
        name: 'Browser Service',
      }),
    )
    serviceMocks.dataServicesService.updateDataService.mockResolvedValue(
      dataServiceDetail({
        name: 'Existing Service Updated',
      }),
    )
    serviceMocks.dataServicesService.listDataServiceParameters.mockResolvedValue(
      [],
    )
    serviceMocks.dataServicesService.listDataServiceFieldMappings.mockResolvedValue(
      [],
    )
    serviceMocks.dataServicesService.listSyncTasks.mockResolvedValue({
      items: [],
      pagination: {
        page: 1,
        size: 10,
        total: 0,
        totalPages: 0,
      },
    })
    serviceMocks.dataServicesService.listGovernanceProfiles.mockResolvedValue({
      items: [],
      total: 0,
    })
    serviceMocks.dataServicesService.listGovernanceHealthSnapshots.mockResolvedValue(
      {
        items: [],
        total: 0,
      },
    )
    serviceMocks.dataServicesService.listGovernanceAlerts.mockResolvedValue({
      items: [],
      total: 0,
    })
    serviceMocks.dataServicesService.listGovernanceTraces.mockResolvedValue({
      items: [],
      total: 0,
    })
    serviceMocks.dataServicesService.listGovernanceAudits.mockResolvedValue({
      items: [],
      total: 0,
    })
    serviceMocks.dataServicesService.listServiceVersions.mockResolvedValue({
      items: [],
      total: 0,
    })
    serviceMocks.dataServicesService.listGovernanceHealthRules.mockResolvedValue(
      [],
    )
    serviceMocks.dataServicesService.listGovernanceAlertRules.mockResolvedValue(
      [],
    )
  })

  it('shows connector empty state and validates required form fields', async () => {
    renderPage()

    expect(await screen.findByText('暂无数据')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /保存定义/ }))

    expect(screen.getByText('connectorId、code、name 必填')).toBeInTheDocument()
    expect(
      serviceMocks.dataServicesService.upsertConnector,
    ).not.toHaveBeenCalled()
  })

  it('validates sensitive connector parameters before saving', async () => {
    serviceMocks.dataServicesService.listConnectors.mockResolvedValueOnce({
      items: [connectorSummary()],
      pagination: {
        page: 1,
        size: 10,
        total: 1,
        totalPages: 1,
      },
    })
    serviceMocks.dataServicesService.listConnectorParameterTemplates.mockResolvedValueOnce(
      [
        {
          paramKey: 'baseUrl',
          displayName: 'Base URL',
          category: 'ENDPOINT',
          required: true,
          sensitive: false,
        },
        {
          paramKey: 'token',
          displayName: 'Token',
          category: 'AUTH',
          required: true,
          sensitive: true,
        },
      ],
    )

    renderPage()

    expect(await screen.findByText('http_main')).toBeInTheDocument()
    const parameterInput = await screen.findByLabelText(/参数 key=value/)
    fireEvent.change(parameterInput, {
      target: {
        value: 'baseUrl=https://example.test|false\ntoken=plain-token|true',
      },
    })
    fireEvent.click(screen.getByRole('button', { name: /保存参数/ }))

    expect(
      await screen.findByText('敏感连接参数必须使用 keyRef: 引用: token'),
    ).toBeInTheDocument()
    expect(
      serviceMocks.dataServicesService.saveConnectorParameters,
    ).not.toHaveBeenCalled()
  })

  it('shows connector error state with retry action', async () => {
    serviceMocks.dataServicesService.listConnectors.mockRejectedValueOnce(
      new Error('connector boom'),
    )

    renderPage()

    expect(await screen.findByText('加载失败')).toBeInTheDocument()
    expect(screen.getByText('connector boom')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /重试/ })).toBeInTheDocument()
  })

  it('loads connector rows and detail actions from real service calls', async () => {
    serviceMocks.dataServicesService.listConnectors.mockResolvedValueOnce({
      items: [connectorSummary()],
      pagination: {
        page: 1,
        size: 10,
        total: 1,
        totalPages: 1,
      },
    })

    renderPage()

    expect(await screen.findByText('http_main')).toBeInTheDocument()
    await waitFor(() =>
      expect(
        serviceMocks.dataServicesService.getConnector,
      ).toHaveBeenCalledWith('connector-1'),
    )

    expect(screen.getByRole('button', { name: /测试连接/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /启用/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /停用/ })).toBeInTheDocument()
  })
  it('disables governance alert actions that no longer match the lifecycle state', async () => {
    serviceMocks.dataServicesService.listGovernanceProfiles.mockResolvedValue({
      items: [
        {
          governanceId: 'gov-1',
          code: 'gov-api',
          scopeType: 'API',
          targetCode: 'api.invoice.query',
          slaPolicyJson: '{}',
          alertPolicyJson: '{}',
          status: 'ACTIVE',
          tenantId: '00000000-0000-4000-8000-000000000001',
          createdAt: '2026-04-30T00:00:00Z',
          updatedAt: '2026-04-30T00:00:00Z',
        },
      ],
      total: 1,
    })
    serviceMocks.dataServicesService.listGovernanceAlerts.mockResolvedValue({
      items: [
        {
          alertId: 'alert-1',
          governanceId: 'gov-1',
          ruleId: 'rule-1',
          targetType: 'API',
          targetCode: 'api.invoice.query',
          alertLevel: 'ERROR',
          alertType: 'DATA_QUALITY',
          status: 'CLOSED',
          alertKey: 'gov-api:alert',
          summary: 'closed alert',
          detail: 'already handled',
          traceId: 'trace-1',
          occurredAt: '2026-04-30T00:00:00Z',
        },
      ],
      total: 1,
    })

    renderPage()
    fireEvent.click(screen.getByRole('tab', { name: 'Governance' }))
    fireEvent.click(await screen.findByText('closed alert'))

    expect(screen.getByRole('button', { name: '确认' })).toBeDisabled()
    expect(screen.getByRole('button', { name: '升级' })).toBeDisabled()
    expect(screen.getByRole('button', { name: '关闭' })).toBeDisabled()
    expect(
      serviceMocks.dataServicesService.handleGovernanceAlert,
    ).not.toHaveBeenCalled()
  })

  it('creates a data service from a fresh form even when an existing row is selected', async () => {
    renderPage()
    fireEvent.click(screen.getByRole('tab', { name: 'DataService' }))

    expect(await screen.findByText('existing_service')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Code'), {
      target: { value: 'browser_service' },
    })
    fireEvent.change(screen.getByLabelText('名称'), {
      target: { value: 'Browser Service' },
    })

    fireEvent.click(screen.getByRole('button', { name: /保存服务/ }))

    await waitFor(() =>
      expect(
        serviceMocks.dataServicesService.createDataService,
      ).toHaveBeenCalledWith(
        expect.objectContaining({
          serviceId: '00000000-0000-4000-8000-000000000013',
          code: 'browser_service',
          name: 'Browser Service',
        }),
      ),
    )
    expect(
      serviceMocks.dataServicesService.updateDataService,
    ).not.toHaveBeenCalled()
  })

  it('updates a data service only after loading the selected detail into the form', async () => {
    renderPage()
    fireEvent.click(screen.getByRole('tab', { name: 'DataService' }))

    expect(await screen.findByText('existing_service')).toBeInTheDocument()
    fireEvent.click(await screen.findByRole('button', { name: /载入编辑/ }))
    await waitFor(() =>
      expect(screen.getByDisplayValue('Existing Service')).toBeInTheDocument(),
    )
    fireEvent.change(screen.getByLabelText('名称'), {
      target: { value: 'Existing Service Updated' },
    })

    fireEvent.click(screen.getByRole('button', { name: /保存服务/ }))

    await waitFor(() =>
      expect(
        serviceMocks.dataServicesService.updateDataService,
      ).toHaveBeenCalledWith(
        'service-existing',
        expect.objectContaining({
          code: 'existing_service',
          name: 'Existing Service Updated',
        }),
      ),
    )
    expect(
      serviceMocks.dataServicesService.createDataService,
    ).not.toHaveBeenCalled()
  })
})
