import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { PersonFormDialog } from '@/features/org-perm/components/person-form-dialog'

describe('PersonFormDialog', () => {
  it('renders create form and submits payload', () => {
    const onSubmit = vi.fn()

    render(<PersonFormDialog onClose={vi.fn()} onSubmit={onSubmit} open />)

    fireEvent.change(screen.getByLabelText('Login account'), {
      target: { value: 'zhangsan' },
    })
    fireEvent.change(screen.getByLabelText('Display name'), {
      target: { value: 'Zhang San' },
    })
    fireEvent.change(screen.getByLabelText('Organization ID'), {
      target: { value: 'org-1' },
    })
    fireEvent.click(screen.getByText('Save'))

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        accountName: 'zhangsan',
        displayName: 'Zhang San',
        orgId: 'org-1',
        status: 'ACTIVE',
      }),
    )
  })

  it('does not render when closed', () => {
    render(
      <PersonFormDialog onClose={vi.fn()} onSubmit={vi.fn()} open={false} />,
    )

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})
