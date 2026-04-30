import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AuditFilter } from '@/features/infra-admin/components/audit-filter'

describe('AuditFilter', () => {
  it('renders filter controls and submits values', () => {
    const onSearch = vi.fn()

    render(<AuditFilter onSearch={onSearch} />)

    fireEvent.change(screen.getByLabelText('Module'), {
      target: { value: 'scheduler' },
    })
    fireEvent.change(screen.getByLabelText('Action'), {
      target: { value: 'UPDATE' },
    })
    fireEvent.change(screen.getByLabelText('Object ID'), {
      target: { value: 'job-1' },
    })
    fireEvent.click(screen.getByText('Search'))

    expect(onSearch).toHaveBeenCalledWith(
      expect.objectContaining({
        moduleCode: 'scheduler',
        action: 'UPDATE',
        objectId: 'job-1',
      }),
    )
  })
})
