import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
import App from './App'
import { useAuthStore } from '@/stores/auth-store'
import { useIdentityStore } from '@/stores/identity-store'
import { useWorkspaceShellStore } from '@/stores/workspace-shell-store'

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
  useWorkspaceShellStore.setState({
    activeKey: 'home',
    openKeys: ['home'],
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

  it('renders an authenticated workspace route with the matching tab selected', async () => {
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

    const messageTabs = await screen.findAllByRole(
      'tab',
      { name: '信息发布' },
      { timeout: 10000 },
    )

    expect(
      messageTabs.some(
        (element) => element.getAttribute('aria-selected') === 'true',
      ),
    ).toBe(true)
    expect(
      await screen.findByText('门户管理员', undefined, {
        timeout: 10000,
      }),
    ).toBeInTheDocument()
  }, 15000)
})
