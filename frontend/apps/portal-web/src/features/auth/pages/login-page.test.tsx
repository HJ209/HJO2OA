import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
import LoginPage from '@/features/auth/pages/login-page'
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

describe('LoginPage', () => {
  it('renders username and password login form', () => {
    render(
      <MemoryRouter future={routerFuture}>
        <LoginPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('进入门户演示环境')).toBeInTheDocument()
    expect(screen.getByLabelText('用户名')).toBeInTheDocument()
    expect(screen.getByLabelText('密码')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /进入工作台/u })).toBeEnabled()
  })
})
