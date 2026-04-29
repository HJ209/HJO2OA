import { Suspense, lazy, type ReactElement } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import AppShell from '@/app/AppShell'
import { Skeleton } from '@/components/ui/skeleton'
import ProtectedRoute from '@/routes/protected-route'

const LoginPage = lazy(() => import('@/features/auth/pages/login-page'))
const WorkspacePage = lazy(() => import('@/pages/workspace-page'))

function RouteFallback(): ReactElement {
  return (
    <div className="space-y-4 px-4 py-8 lg:px-8">
      <Skeleton className="h-8 w-40" />
      <Skeleton className="h-28 w-full" />
      <Skeleton className="h-28 w-full" />
    </div>
  )
}

export default function AppRoutes(): ReactElement {
  return (
    <Suspense fallback={<RouteFallback />}>
      <Routes>
        <Route element={<LoginPage />} path="/login" />
        <Route element={<Navigate replace to="/login" />} path="/auth/login" />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            <Route element={<WorkspacePage />} index />
            <Route element={<WorkspacePage />} path="docs" />
            <Route element={<WorkspacePage />} path="todo" />
            <Route element={<WorkspacePage />} path="messages" />
            <Route element={<WorkspacePage />} path="org/*" />
            <Route element={<WorkspacePage />} path="admin/*" />
          </Route>
        </Route>
        <Route element={<Navigate replace to="/" />} path="*" />
      </Routes>
    </Suspense>
  )
}
