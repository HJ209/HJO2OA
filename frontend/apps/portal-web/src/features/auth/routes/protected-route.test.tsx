import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
import ProtectedRoute from '@/routes/protected-route'
import { useAuthStore } from '@/stores/auth-store'

const routerFuture = {
  v7_relativeSplatPath: true,
  v7_startTransition: true,
} as const

afterEach(() => {
  useAuthStore.getState().logout()
})

describe('ProtectedRoute', () => {
  it('redirects anonymous users to /login', () => {
    render(
      <MemoryRouter future={routerFuture} initialEntries={['/todo']}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route element={<div>受保护内容</div>} path="/todo" />
          </Route>
          <Route element={<div>登录页</div>} path="/login" />
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByText('登录页')).toBeInTheDocument()
    expect(screen.queryByText('受保护内容')).not.toBeInTheDocument()
  })
})
