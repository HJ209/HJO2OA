import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactElement, ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DataServicesPage from '@/features/data-services/pages/data-services-page'
import type {
  ConnectorDetail,
  ConnectorListView,
  ConnectorSummary,
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
    listSyncTasks: vi.fn(),
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
    serviceMocks.dataServicesService.listSyncTasks.mockResolvedValue({
      items: [],
      pagination: {
        page: 1,
        size: 10,
        total: 0,
        totalPages: 0,
      },
    })
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
})
