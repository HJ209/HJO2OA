import type { ReactElement } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/hooks/use-auth'

export default function ProtectedRoute(): ReactElement {
  const location = useLocation()
  const { isAuthenticated } = useAuth()

  if (!isAuthenticated) {
    const redirectTarget = `${location.pathname}${location.search}`
    return <Navigate replace state={{ from: redirectTarget }} to="/login" />
  }

  return <Outlet />
}
