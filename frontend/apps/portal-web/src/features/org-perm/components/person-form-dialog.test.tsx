import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { PersonFormDialog } from '@/features/org-perm/components/person-form-dialog'

vi.mock('@/features/infra-admin/hooks/use-dictionary', () => ({
  useSystemEnumOptions: () => ({ data: [] }),
}))

describe('PersonFormDialog', () => {
  it('renders create form and submits payload', () => {
    const onSubmit = vi.fn()

    render(<PersonFormDialog onClose={vi.fn()} onSubmit={onSubmit} open />)

    fireEvent.change(screen.getByLabelText('登录账号'), {
      target: { value: 'zhangsan' },
    })
    fireEvent.change(screen.getByLabelText('显示名称'), {
      target: { value: '张三' },
    })
    fireEvent.click(screen.getByText('保存'))

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        accountName: 'zhangsan',
        displayName: '张三',
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
