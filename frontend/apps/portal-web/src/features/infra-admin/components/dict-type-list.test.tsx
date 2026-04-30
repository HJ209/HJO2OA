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
            name: 'Status',
            status: 'enabled',
          },
          {
            code: 'priority',
            name: 'Priority',
            status: 'enabled',
            systemManaged: true,
          },
        ]}
        onSelect={onSelect}
        selectedCode="status"
      />,
    )

    expect(screen.getByText('Dictionary Types')).toBeInTheDocument()
    expect(screen.getByText('Status')).toBeInTheDocument()
    expect(screen.getByText('priority / system')).toBeInTheDocument()
    fireEvent.click(screen.getByText('Priority'))
    expect(onSelect).toHaveBeenCalledWith('priority')
  })
})
