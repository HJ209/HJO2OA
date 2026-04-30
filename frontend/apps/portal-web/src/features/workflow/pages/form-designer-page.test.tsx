import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactElement, ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FormDesignerPage from '@/features/workflow/pages/form-designer-page'

const serviceMocks = vi.hoisted(() => ({
  formService: {
    createMetadata: vi.fn(),
    publishMetadata: vi.fn(),
  },
}))

vi.mock('@/features/workflow/services/form-service', () => ({
  formService: serviceMocks.formService,
}))

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

function dataTransfer(): DataTransfer {
  const values = new Map<string, string>()

  return {
    effectAllowed: 'all',
    getData: vi.fn((type: string) => values.get(type) ?? ''),
    setData: vi.fn((type: string, value: string) => {
      values.set(type, value)
    }),
  } as unknown as DataTransfer
}

describe('FormDesignerPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    serviceMocks.formService.createMetadata.mockResolvedValue({
      id: 'metadata-1',
      code: 'expense.request',
      name: 'Expense Request',
      version: 1,
      status: 'DRAFT',
      fieldSchema: [],
      layout: {},
      fieldPermissionMap: {},
      tenantId: '11111111-1111-1111-1111-111111111111',
      createdAt: '2026-04-30T00:00:00Z',
      updatedAt: '2026-04-30T00:00:00Z',
    })
  })

  it('adds palette fields by drag-and-drop, reorders rows, and saves schema', async () => {
    render(renderWithQueryClient(<FormDesignerPage />))

    const paletteTransfer = dataTransfer()
    fireEvent.dragStart(screen.getByTestId('field-palette-TEXT'), {
      dataTransfer: paletteTransfer,
    })
    fireEvent.drop(screen.getByTestId('form-designer-canvas'), {
      dataTransfer: paletteTransfer,
    })

    expect(screen.getByText('Text field')).toBeInTheDocument()

    const rowTransfer = dataTransfer()
    fireEvent.dragStart(screen.getByTestId('field-row-1'), {
      dataTransfer: rowTransfer,
    })
    fireEvent.drop(screen.getByTestId('field-row-0'), {
      dataTransfer: rowTransfer,
    })

    const rows = screen.getAllByTestId(/field-row-/)
    expect(rows[0]).toHaveTextContent('Attachments')
    fireEvent.click(rows[0])

    await waitFor(() => {
      expect(screen.getByLabelText('Field code')).toHaveValue('attachments')
    })
    fireEvent.change(screen.getByLabelText('Field code'), {
      target: { value: 'receipt files' },
    })
    expect(screen.getAllByTestId(/field-row-/)[0]).toHaveTextContent(
      'receipt_files',
    )

    fireEvent.click(screen.getByRole('button', { name: /^Save$/ }))

    await waitFor(() => {
      expect(serviceMocks.formService.createMetadata).toHaveBeenCalledWith(
        expect.objectContaining({
          code: 'expense.request',
          fieldSchema: expect.arrayContaining([
            expect.objectContaining({
              fieldCode: 'receipt_files',
              fieldType: 'ATTACHMENT',
            }),
            expect.objectContaining({
              fieldCode: 'text1',
              fieldType: 'TEXT',
            }),
          ]),
        }),
      )
    })
  })
})
