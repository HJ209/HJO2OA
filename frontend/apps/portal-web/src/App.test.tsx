import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
import App from './App'
import { useAuthStore } from '@/stores/auth-store'
import { useIdentityStore } from '@/stores/identity-store'

const routerFuture = {
  v7_relativeSplatPath: true,
  v7_startTransition: true,
} as const

afterEach(() => {
  useAuthStore.setState({ token: null, user: null })
  useIdentityStore.setState({
    currentAssignment: null,
    orgId: null,
    roleIds: [],
  })
})

describe('App', () => {
  it('redirects unauthenticated users to the login entry page', async () => {
    render(
      <MemoryRouter future={routerFuture} initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    )

    expect(await screen.findByText('进入门户演示环境')).toBeInTheDocument()
    expect(screen.getByText('进入工作台')).toBeInTheDocument()
  })

  it('renders an authenticated nested route page', async () => {
    useAuthStore.setState({
      token: 'test-token',
      user: {
        id: 'acct-test-001',
        accountName: 'portal.admin',
        displayName: '门户管理员',
        tenantId: 'tenant-demo',
        locale: 'zh-CN',
      },
    })
    useIdentityStore.setState({
      currentAssignment: {
        assignmentId: 'assign-demo-001',
        positionId: 'position-demo-001',
        orgId: 'org-demo-001',
        positionName: '门户平台主管',
        orgName: '数字办公部',
      },
      orgId: 'org-demo-001',
      roleIds: ['ROLE_PORTAL_ADMIN'],
    })

    render(
      <MemoryRouter future={routerFuture} initialEntries={['/messages']}>
        <App />
      </MemoryRouter>,
    )

    expect(
      await screen.findByText('统一查看系统通知、审批提醒和业务播报。'),
    ).toBeInTheDocument()
    expect(screen.getByText('门户管理员')).toBeInTheDocument()
  })
})
