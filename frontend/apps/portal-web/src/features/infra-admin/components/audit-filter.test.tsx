import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AuditFilter } from '@/features/infra-admin/components/audit-filter'

describe('AuditFilter', () => {
  it('renders filter controls and submits values', () => {
    const onSearch = vi.fn()

    render(<AuditFilter onSearch={onSearch} />)

    fireEvent.change(screen.getByLabelText('操作者'), {
      target: { value: 'admin' },
    })
    fireEvent.change(screen.getByLabelText('操作'), {
      target: { value: 'UPDATE' },
    })
    fireEvent.change(screen.getByLabelText('资源'), {
      target: { value: 'config' },
    })
    fireEvent.click(screen.getByText('查询'))

    expect(onSearch).toHaveBeenCalledWith(
      expect.objectContaining({
        actor: 'admin',
        action: 'UPDATE',
        resource: 'config',
      }),
    )
  })
})
