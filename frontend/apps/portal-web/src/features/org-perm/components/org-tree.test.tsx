import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { OrgTree } from '@/features/org-perm/components/org-tree'
import type { OrgStructure } from '@/features/org-perm/types/org-perm'

const nodes: OrgStructure[] = [
  {
    id: 'org-1',
    parentId: null,
    name: '总部',
    code: 'HQ',
    type: 'COMPANY',
    status: 'ACTIVE',
    sortOrder: 1,
    children: [
      {
        id: 'org-2',
        parentId: 'org-1',
        name: '研发中心',
        code: 'RD',
        type: 'DEPARTMENT',
        status: 'ACTIVE',
        sortOrder: 2,
      },
    ],
  },
]

describe('OrgTree', () => {
  it('renders recursive organization nodes and selects a node', () => {
    const onSelect = vi.fn()

    render(<OrgTree nodes={nodes} onSelect={onSelect} />)

    expect(screen.getByText('总部')).toBeInTheDocument()
    expect(screen.getByText('研发中心')).toBeInTheDocument()

    fireEvent.click(screen.getByText('研发中心'))

    expect(onSelect).toHaveBeenCalledWith(nodes[0].children?.[0])
  })

  it('renders empty state', () => {
    render(<OrgTree nodes={[]} onSelect={vi.fn()} />)

    expect(screen.getByText('暂无组织数据')).toBeInTheDocument()
  })
})
