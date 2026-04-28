import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { DictTypeList } from '@/features/infra-admin/components/dict-type-list'

describe('DictTypeList', () => {
  it('renders dictionary types and emits selection', () => {
    const onSelect = vi.fn()

    render(
      <DictTypeList
        items={[
          {
            code: 'status',
            name: '状态',
            status: 'enabled',
          },
          {
            code: 'priority',
            name: '优先级',
            status: 'enabled',
          },
        ]}
        onSelect={onSelect}
        selectedCode="status"
      />,
    )

    expect(screen.getByText('字典类型')).toBeInTheDocument()
    expect(screen.getByText('状态')).toBeInTheDocument()
    fireEvent.click(screen.getByText('优先级'))
    expect(onSelect).toHaveBeenCalledWith('priority')
  })
})
