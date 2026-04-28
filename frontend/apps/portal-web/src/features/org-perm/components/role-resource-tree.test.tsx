import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { RoleResourceTree } from '@/features/org-perm/components/role-resource-tree'
import type { ResourceNode } from '@/features/org-perm/types/org-perm'

const nodes: ResourceNode[] = [
  {
    id: 'menu-portal',
    parentId: null,
    name: '门户',
    code: 'portal',
    type: 'MENU',
    effect: 'ALLOW',
    children: [
      {
        id: 'button-create',
        parentId: 'menu-portal',
        name: '新增按钮',
        code: 'portal:create',
        type: 'BUTTON',
        effect: 'ALLOW',
      },
    ],
  },
]

describe('RoleResourceTree', () => {
  it('renders resource tree and toggles descendants', () => {
    const onCheckedChange = vi.fn()

    render(
      <RoleResourceTree
        checkedIds={[]}
        nodes={nodes}
        onCheckedChange={onCheckedChange}
      />,
    )

    expect(screen.getByText('门户')).toBeInTheDocument()
    expect(screen.getByText('新增按钮')).toBeInTheDocument()

    fireEvent.click(screen.getByLabelText('门户'))

    expect(onCheckedChange).toHaveBeenCalledWith([
      'menu-portal',
      'button-create',
    ])
  })

  it('renders empty state', () => {
    render(
      <RoleResourceTree checkedIds={[]} nodes={[]} onCheckedChange={vi.fn()} />,
    )

    expect(screen.getByText('暂无资源权限')).toBeInTheDocument()
  })
})
