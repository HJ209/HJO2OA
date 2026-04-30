import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactElement, ReactNode } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import WorkflowPage from '@/features/workflow/pages/workflow-page'
import type {
  ProcessInstanceDetail,
  WorkflowDefinition,
} from '@/features/workflow/types/workflow'

const serviceMocks = vi.hoisted(() => ({
  workflowService: {
    listDefinitions: vi.fn(),
    createDefinition: vi.fn(),
    updateDefinition: vi.fn(),
    publishDefinition: vi.fn(),
    startProcess: vi.fn(),
    getTimeline: vi.fn(),
    listActions: vi.fn(),
    executeAction: vi.fn(),
  },
}))

vi.mock('@/features/workflow/services/workflow-service', () => ({
  workflowService: serviceMocks.workflowService,
}))

const routerFuture = {
  v7_relativeSplatPath: true,
  v7_startTransition: true,
} as const

const publishedDefinition: WorkflowDefinition = {
  id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
  code: 'codex_flow',
  name: 'Codex Flow',
  category: 'REGRESSION',
  version: 1,
  status: 'PUBLISHED',
  formMetadataId: null,
  startNodeId: 'start',
  endNodeId: 'end',
  nodes: '[]',
  routes: '[]',
  tenantId: '11111111-1111-1111-1111-111111111111',
  publishedAt: '2026-04-30T00:00:00Z',
  publishedBy: '99999999-9999-9999-9999-999999999999',
  createdAt: '2026-04-30T00:00:00Z',
  updatedAt: '2026-04-30T00:00:00Z',
}

const draftDefinition: WorkflowDefinition = {
  ...publishedDefinition,
  id: 'dddddddd-dddd-dddd-dddd-dddddddddddd',
  code: 'codex_draft_flow',
  name: 'Codex Draft Flow',
  status: 'DRAFT',
  publishedAt: null,
  publishedBy: null,
}

const processDetail: ProcessInstanceDetail = {
  instance: {
    id: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    definitionId: publishedDefinition.id,
    definitionVersion: 1,
    definitionCode: publishedDefinition.code,
    businessKey: 'bk-1',
    title: 'Expense Approval',
    category: 'REGRESSION',
    initiatorId: '99999999-9999-9999-9999-999999999999',
    initiatorOrgId: '11111111-1111-1111-1111-111111111111',
    initiatorDeptId: '22222222-2222-2222-2222-222222222222',
    initiatorPositionId: '33333333-3333-3333-3333-333333333333',
    formMetadataId: 'cccccccc-cccc-cccc-cccc-cccccccccccc',
    formDataId: '44444444-4444-4444-4444-444444444444',
    currentNodes: ['approve'],
    status: 'RUNNING',
    startTime: '2026-04-30T00:00:00Z',
    endTime: null,
    tenantId: '11111111-1111-1111-1111-111111111111',
    idempotencyKey: null,
    createdAt: '2026-04-30T00:00:00Z',
    updatedAt: '2026-04-30T00:00:00Z',
  },
  tasks: [],
  actions: [],
  nodeHistory: [],
  variableHistory: [],
}

function renderWithProviders(children: ReactNode): ReactElement {
  const queryClient = new QueryClient({
    defaultOptions: {
      mutations: { retry: false },
      queries: { retry: false },
    },
  })

  return (
    <MemoryRouter future={routerFuture} initialEntries={['/workflow']}>
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </MemoryRouter>
  )
}

function renderPage(): void {
  render(renderWithProviders(<WorkflowPage />))
}

describe('WorkflowPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    serviceMocks.workflowService.listDefinitions.mockResolvedValue([
      publishedDefinition,
    ])
    serviceMocks.workflowService.listActions.mockResolvedValue([])
    serviceMocks.workflowService.startProcess.mockResolvedValue(processDetail)
    serviceMocks.workflowService.publishDefinition.mockResolvedValue(
      publishedDefinition,
    )
  })

  it('starts a process with valid UUID defaults', async () => {
    renderPage()

    expect(await screen.findByText('Codex Flow')).toBeInTheDocument()
    fireEvent.submit(screen.getByPlaceholderText('title').closest('form')!)

    await waitFor(() => {
      expect(serviceMocks.workflowService.startProcess).toHaveBeenCalledWith(
        expect.objectContaining({
          definitionId: publishedDefinition.id,
          formDataId: '44444444-4444-4444-4444-444444444444',
          initiatorId: '99999999-9999-9999-9999-999999999999',
          initiatorOrgId: '11111111-1111-1111-1111-111111111111',
          initiatorPositionId: '33333333-3333-3333-3333-333333333333',
        }),
      )
    })
  })

  it('blocks invalid UUID values before sending start requests', async () => {
    renderPage()

    expect(await screen.findByText('Codex Flow')).toBeInTheDocument()
    fireEvent.change(screen.getByPlaceholderText('initiatorOrgId'), {
      target: { value: 'org-1' },
    })
    fireEvent.submit(screen.getByPlaceholderText('title').closest('form')!)

    await waitFor(() => {
      expect(screen.getByText('发起组织 ID 必须是 UUID 格式')).toBeInTheDocument()
    })
    expect(serviceMocks.workflowService.startProcess).not.toHaveBeenCalled()
  })

  it('blocks invalid publishedBy values before sending publish requests', async () => {
    serviceMocks.workflowService.listDefinitions.mockResolvedValue([
      draftDefinition,
    ])
    renderPage()

    expect(await screen.findByText('Codex Draft Flow')).toBeInTheDocument()
    fireEvent.change(screen.getByPlaceholderText('publishedBy'), {
      target: { value: 'admin' },
    })
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '发布' })).toBeEnabled()
    })
    fireEvent.click(screen.getByRole('button', { name: '发布' }))

    await waitFor(() => {
      expect(screen.getByText('发布人 ID 必须是 UUID 格式')).toBeInTheDocument()
    })
    expect(serviceMocks.workflowService.publishDefinition).not.toHaveBeenCalled()
  })
})
