import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
import { IdentitySwitcher } from '@/features/identity/components/identity-switcher'
import { useAuthStore } from '@/stores/auth-store'
import { useIdentityStore } from '@/stores/identity-store'

const routerFuture = {
  v7_relativeSplatPath: true,
  v7_startTransition: true,
} as const

afterEach(() => {
  useAuthStore.getState().logout()
  useIdentityStore.getState().clear()
})

describe('IdentitySwitcher', () => {
  it('renders current assignment and available assignments', () => {
    useAuthStore.getState().login({
      token: 'test-token',
      user: {
        id: 'acct-001',
        accountName: 'portal.admin',
        displayName: '门户管理员',
        tenantId: 'tenant-demo',
        locale: 'zh-CN',
      },
    })
    useIdentityStore.getState().applyContext({
      currentAssignment: {
        assignmentId: 'assign-001',
        positionId: 'position-001',
        orgId: 'org-001',
        positionName: '平台主管',
        orgName: '数字办公部',
      },
      orgId: 'org-001',
      roleIds: ['ROLE_PORTAL_ADMIN'],
      assignments: [
        {
          assignmentId: 'assign-001',
          positionId: 'position-001',
          orgId: 'org-001',
          positionName: '平台主管',
          orgName: '数字办公部',
        },
        {
          assignmentId: 'assign-002',
          positionId: 'position-002',
          orgId: 'org-002',
          positionName: '组织专员',
          orgName: '人力资源部',
        },
      ],
      pendingTodoCount: 3,
      unreadMessageCount: 2,
    })

    render(
      <MemoryRouter future={routerFuture}>
        <IdentitySwitcher />
      </MemoryRouter>,
    )

    expect(screen.getByText('门户管理员')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /门户管理员/u }))

    expect(screen.getAllByText('平台主管').length).toBeGreaterThanOrEqual(2)
    expect(screen.getAllByText('数字办公部').length).toBeGreaterThanOrEqual(2)
    expect(screen.getByText('组织专员')).toBeInTheDocument()
    expect(screen.getByText(/待办 3/u)).toBeInTheDocument()
    expect(screen.getByText(/消息 2/u)).toBeInTheDocument()
  })
})
