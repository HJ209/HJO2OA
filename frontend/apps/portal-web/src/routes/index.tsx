import { Suspense, lazy, type ReactElement } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import AppShell from '@/app/AppShell'
import { Skeleton } from '@/components/ui/skeleton'
import ProtectedRoute from '@/routes/protected-route'

const LoginPage = lazy(() => import('@/features/auth/pages/login-page'))
const PortalHomePage = lazy(
  () => import('@/features/portal-home/pages/portal-home-page'),
)
const TodoCenterPage = lazy(
  () => import('@/features/todo/pages/todo-center-page'),
)
const MessageCenterPage = lazy(
  () => import('@/features/messages/pages/message-center-page'),
)
const OrgPermPage = lazy(
  () => import('@/features/org-perm/pages/org-perm-page'),
)
const OrgTreePage = lazy(
  () => import('@/features/org-perm/pages/org-tree-page'),
)
const PersonListPage = lazy(
  () => import('@/features/org-perm/pages/person-list-page'),
)
const PositionPage = lazy(
  () => import('@/features/org-perm/pages/position-page'),
)
const RoleAuthPage = lazy(
  () => import('@/features/org-perm/pages/role-auth-page'),
)
const DataPermissionPage = lazy(
  () => import('@/features/org-perm/pages/data-permission-page'),
)
const SyncAuditPage = lazy(
  () => import('@/features/org-perm/pages/sync-audit-page'),
)
const InfraAdminPage = lazy(
  () => import('@/features/infra-admin/pages/infra-admin-page'),
)
const DictionaryPage = lazy(
  () => import('@/features/infra-admin/pages/dictionary-page'),
)
const ConfigPage = lazy(
  () => import('@/features/infra-admin/pages/config-page'),
)
const ErrorCodePage = lazy(
  () => import('@/features/infra-admin/pages/error-code-page'),
)
const CachePage = lazy(() => import('@/features/infra-admin/pages/cache-page'))
const AuditPage = lazy(() => import('@/features/infra-admin/pages/audit-page'))
const TenantPage = lazy(
  () => import('@/features/infra-admin/pages/tenant-page'),
)
const SecurityPage = lazy(
  () => import('@/features/infra-admin/pages/security-page'),
)
const SchedulerPage = lazy(
  () => import('@/features/infra-admin/pages/scheduler-page'),
)
const I18nPage = lazy(() => import('@/features/infra-admin/pages/i18n-page'))
const DataI18nPage = lazy(
  () => import('@/features/infra-admin/pages/data-i18n-page'),
)
const TimezonePage = lazy(
  () => import('@/features/infra-admin/pages/timezone-page'),
)
const AttachmentPage = lazy(
  () => import('@/features/infra-admin/pages/attachment-page'),
)
const EventBusPage = lazy(
  () => import('@/features/infra-admin/pages/event-bus-page'),
)

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
            <Route element={<PortalHomePage />} index />
            <Route element={<TodoCenterPage />} path="todo" />
            <Route element={<MessageCenterPage />} path="messages" />
            <Route element={<OrgPermPage />} path="org">
              <Route element={<Navigate replace to="tree" />} index />
              <Route element={<OrgTreePage />} path="tree" />
              <Route element={<PersonListPage />} path="persons" />
              <Route element={<PositionPage />} path="positions" />
              <Route element={<RoleAuthPage />} path="roles" />
              <Route element={<DataPermissionPage />} path="data-permission" />
              <Route element={<SyncAuditPage />} path="sync-audit" />
            </Route>
            <Route element={<InfraAdminPage />} path="admin">
              <Route element={<DictionaryPage />} path="dictionary" />
              <Route element={<ConfigPage />} path="config" />
              <Route element={<ErrorCodePage />} path="error-codes" />
              <Route element={<CachePage />} path="cache" />
              <Route element={<AuditPage />} path="audit" />
              <Route element={<TenantPage />} path="tenant" />
              <Route element={<SecurityPage />} path="security" />
              <Route element={<SchedulerPage />} path="scheduler" />
              <Route element={<I18nPage />} path="i18n" />
              <Route element={<DataI18nPage />} path="data-i18n" />
              <Route element={<TimezonePage />} path="timezone" />
              <Route element={<AttachmentPage />} path="attachment" />
              <Route element={<EventBusPage />} path="event-bus" />
            </Route>
          </Route>
        </Route>
        <Route element={<Navigate replace to="/" />} path="*" />
      </Routes>
    </Suspense>
  )
}
